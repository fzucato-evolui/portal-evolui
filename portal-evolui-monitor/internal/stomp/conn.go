package stomp

import (
	"bufio"
	"bytes"
	"crypto/tls"
	"fmt"
	"io"
	"log"
	"net/http"

	"net/url"
	"portal-evolui-monitor/internal/logutil"
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

// DialOpts configura o handshake WebSocket + STOMP (alinhado ao portal-evolui-runner-installer).
type DialOpts struct {
	JWT           string
	Identifier    string
	SkipTLSVerify bool
}

type Client struct {
	conn     *websocket.Conn
	writeMu  sync.Mutex
	subMu    sync.RWMutex
	subs     map[string]func(m Message)
	nextSub  int
	dest2sub map[string]string
	done     chan struct{}
	doneOnce sync.Once
}

// Dial abre WebSocket + frame STOMP CONNECT (heart-beat 10s/10s).
func Dial(wsURL string, opts DialOpts) (*Client, error) {
	u, err := url.Parse(wsURL)
	if err != nil {
		return nil, err
	}
	q := u.Query()
	q.Set("Authorization", opts.JWT)
	u.RawQuery = q.Encode()

	hdr := http.Header{}
	hdr.Set("Identifier", opts.Identifier)

	d := &websocket.Dialer{
		Subprotocols: []string{"v12.stomp", "v11.stomp", "v10.stomp"},
	}
	// wss: TLS; ws: sem TLS (cleartext).
	if u.Scheme == "wss" {
		d.TLSClientConfig = &tls.Config{InsecureSkipVerify: opts.SkipTLSVerify}
	}

	ws, _, err := d.Dial(u.String(), hdr)
	if err != nil {
		return nil, fmt.Errorf("websocket: %w", err)
	}
	cl := &Client{
		conn:     ws,
		subs:     make(map[string]func(Message)),
		dest2sub: make(map[string]string),
		done:     make(chan struct{}),
	}
	if err := cl.connectStomp(); err != nil {
		_ = ws.Close()
		return nil, err
	}
	go cl.readLoop()
	go cl.heartbeat()
	return cl, nil
}

// Closed é fechado quando a leitura WebSocket termina (reconectar no supervisor).
func (c *Client) Closed() <-chan struct{} {
	return c.done
}

func (c *Client) signalClosed() {
	c.doneOnce.Do(func() { close(c.done) })
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
	if err == nil {
		logutil.V("[stomp] SUBSCRIBE ok id=%s destination=%s", sid, destination)
	} else {
		log.Printf("[stomp] SUBSCRIBE falhou destination=%s: %v", destination, err)
	}
	return sid, err
}

func (c *Client) Send(appDestination string, jsonBody []byte) error {
	logutil.V("[stomp] SEND destination=%s payloadLen=%d", appDestination, len(jsonBody))
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
	if err != nil {
		log.Printf("[stomp] SEND falhou: %v", err)
	}
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
	defer c.signalClosed()
	defer c.conn.Close()
	var buf []byte
	const maxBuf = 1 << 20
	for {
		_, msg, err := c.conn.ReadMessage()
		if err != nil {
			logutil.V("[stomp] WebSocket ReadMessage encerrado: %v", err)
			return
		}
		buf = append(buf, msg...)
		for len(buf) > 0 && (buf[0] == '\n' || buf[0] == '\r') {
			buf = buf[1:]
		}
		if len(buf) > maxBuf {
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
				log.Printf("[stomp] parseFrame: %v", err)
				continue
			}
			switch m.Command {
			case "MESSAGE":
				dest := m.Headers["destination"]
				sub := m.Headers["subscription"]
				logutil.V("[stomp] MESSAGE recebido destination=%s subscription=%s bodyLen=%d", dest, sub, len(m.Body))
				if sub == "" {
					log.Printf("[stomp] MESSAGE sem header subscription; headers=%v", m.Headers)
					continue
				}
				c.subMu.RLock()
				h := c.subs[sub]
				c.subMu.RUnlock()
				if h == nil {
					log.Printf("[stomp] MESSAGE sem handler para subscription=%s (dest=%s)", sub, dest)
					continue
				}
				h(m)
			case "ERROR":
				log.Printf("[stomp] ERROR frame: headers=%v body=%s", m.Headers, string(m.Body))
			case "RECEIPT":
				logutil.V("[stomp] RECEIPT: %v", m.Headers)
			default:
				if m.Command != "" {
					logutil.V("[stomp] frame não tratado: command=%s headers=%v bodyLen=%d", m.Command, m.Headers, len(m.Body))
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
