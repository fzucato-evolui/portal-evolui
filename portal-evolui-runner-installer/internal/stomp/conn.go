package stomp

import (
	"bufio"
	"bytes"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
	"sync"
	"time"

	"github.com/gorilla/websocket"
)

type Message struct {
	Command string
	Headers map[string]string
	Body    []byte
}

type Client struct {
	conn     *websocket.Conn
	writeMu  sync.Mutex
	subMu    sync.RWMutex
	subs     map[string]func(m Message) // subscription id -> handler
	nextSub  int
	dest2sub map[string]string // stomp destination -> subscription id
}

func Dial(wsBaseURL, jwt, identifier string) (*Client, error) {
	u, err := url.Parse(wsBaseURL)
	if err != nil {
		return nil, err
	}
	q := u.Query()
	q.Set("Authorization", jwt)
	u.RawQuery = q.Encode()

	hdr := http.Header{}
	hdr.Set("Identifier", identifier)

	d := websocket.Dialer{
		Subprotocols: []string{"v12.stomp", "v11.stomp", "v10.stomp"},
	}
	c, _, err := d.Dial(u.String(), hdr)
	if err != nil {
		return nil, fmt.Errorf("websocket: %w", err)
	}
	cl := &Client{conn: c, subs: make(map[string]func(Message)), dest2sub: make(map[string]string)}
	if err := cl.connectStomp(); err != nil {
		_ = c.Close()
		return nil, err
	}
	go cl.readLoop()
	go cl.heartbeat()
	return cl, nil
}

func (c *Client) connectStomp() error {
	var b strings.Builder
	b.WriteString("CONNECT\n")
	b.WriteString("accept-version:1.2,1.1,1.0\n")
	b.WriteString("heart-beat:10000,10000\n")
	b.WriteString("host:localhost\n")
	b.WriteString("\n")
	b.WriteByte(0)
	c.writeMu.Lock()
	err := c.conn.WriteMessage(websocket.TextMessage, []byte(b.String()))
	c.writeMu.Unlock()
	if err != nil {
		return err
	}
	// aguarda CONNECTED (primeiro frame)
	m, err := c.readOneFrame()
	if err != nil {
		return err
	}
	if m.Command != "CONNECTED" {
		return fmt.Errorf("esperado CONNECTED, veio %s: %s", m.Command, string(m.Body))
	}
	return nil
}

func (c *Client) Subscribe(destination string, handler func(m Message)) (string, error) {
	c.subMu.Lock()
	c.nextSub++
	sid := fmt.Sprintf("sub-%d", c.nextSub)
	c.subs[sid] = handler
	c.dest2sub[destination] = sid
	c.subMu.Unlock()

	var b strings.Builder
	b.WriteString("SUBSCRIBE\n")
	b.WriteString("id:" + sid + "\n")
	b.WriteString("destination:" + destination + "\n")
	b.WriteString("ack:auto\n")
	b.WriteString("\n")
	b.WriteByte(0)
	c.writeMu.Lock()
	err := c.conn.WriteMessage(websocket.TextMessage, []byte(b.String()))
	c.writeMu.Unlock()
	return sid, err
}

func (c *Client) Send(appDestination string, jsonBody []byte) error {
	var b strings.Builder
	b.WriteString("SEND\n")
	b.WriteString("destination:" + appDestination + "\n")
	b.WriteString("content-type:application/json;charset=UTF-8\n")
	b.WriteString("\n")
	b.Write(jsonBody)
	b.WriteByte(0)
	c.writeMu.Lock()
	err := c.conn.WriteMessage(websocket.TextMessage, []byte(b.String()))
	c.writeMu.Unlock()
	return err
}

func (c *Client) Close() error {
	return c.conn.Close()
}

func (c *Client) heartbeat() {
	t := time.NewTicker(8 * time.Second)
	defer t.Stop()
	for range t.C {
		c.writeMu.Lock()
		err := c.conn.WriteMessage(websocket.TextMessage, []byte("\n"))
		c.writeMu.Unlock()
		if err != nil {
			return
		}
	}
}

func (c *Client) readLoop() {
	defer c.conn.Close()
	var buf []byte
	const maxBuf = 1 << 20
	for {
		_, msg, err := c.conn.ReadMessage()
		if err != nil {
			return
		}
		buf = append(buf, msg...)
		for len(buf) > 0 && (buf[0] == '\n' || buf[0] == '\r') {
			buf = buf[1:]
		}
		if len(buf) > maxBuf {
			buf = nil
			return
		}
		for {
			i := bytes.IndexByte(buf, 0)
			if i < 0 {
				break
			}
			frame := buf[:i]
			buf = buf[i+1:]
			if len(frame) == 0 || (len(frame) == 1 && frame[0] == '\n') {
				continue
			}
			m, err := parseFrame(frame)
			if err != nil {
				continue
			}
			if m.Command == "MESSAGE" {
				if sub, ok := m.Headers["subscription"]; ok {
					c.subMu.RLock()
					h := c.subs[sub]
					c.subMu.RUnlock()
					if h != nil {
						h(m)
					}
				}
			}
		}
	}
}

func (c *Client) readOneFrame() (Message, error) {
	var buf []byte
	for {
		_, msg, err := c.conn.ReadMessage()
		if err != nil {
			return Message{}, err
		}
		buf = append(buf, msg...)
		for len(buf) > 0 && (buf[0] == '\n' || buf[0] == '\r') {
			buf = buf[1:]
		}
		i := bytes.IndexByte(buf, 0)
		if i < 0 {
			if len(buf) > 1<<20 {
				return Message{}, fmt.Errorf("frame STOMP incompleto (buffer grande)")
			}
			continue
		}
		frame := buf[:i]
		return parseFrame(frame)
	}
}

func parseFrame(data []byte) (Message, error) {
	data = bytes.TrimLeft(data, "\r\n")
	br := bufio.NewReader(bytes.NewReader(data))
	line, err := br.ReadString('\n')
	if err != nil {
		return Message{}, err
	}
	cmd := strings.TrimSpace(strings.TrimPrefix(line, "\n"))
	if cmd == "" {
		line, _ = br.ReadString('\n')
		cmd = strings.TrimSpace(line)
	}
	headers := map[string]string{}
	for {
		line, err := br.ReadString('\n')
		if err != nil {
			return Message{}, err
		}
		line = strings.TrimRight(line, "\r\n")
		if line == "" {
			break
		}
		p := strings.IndexByte(line, ':')
		if p > 0 {
			k := strings.TrimSpace(line[:p])
			v := strings.TrimSpace(line[p+1:])
			headers[strings.ToLower(k)] = v
		}
	}
	rest, _ := io.ReadAll(br)
	return Message{Command: cmd, Headers: headers, Body: rest}, nil
}
