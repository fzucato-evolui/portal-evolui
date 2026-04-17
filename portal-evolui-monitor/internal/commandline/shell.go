// Package commandline implementa o prompt remoto (paridade CommandLineController do health-checker Java).
package commandline

import (
	"bufio"
	"bytes"
	"context"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"sync"
	"unicode/utf8"

	"golang.org/x/text/encoding/charmap"
)

// ConsoleResponse espelha ConsoleResponseMessageDTO (shared).
type ConsoleResponse struct {
	CurrentDirectory string
	Finished         bool
	Output           string
	OutputError      string
	Sequence         int
}

// Client mantém diretório de trabalho e um processo shell opcional (stdin quando ainda vivo).
type Client struct {
	mu      sync.Mutex
	seqMu   sync.Mutex
	seq     int // incrementa por processo; primeiro chunk = 0
	workDir string
	cmd     *exec.Cmd
	stdin   io.WriteCloser
}

// New cria cliente com cwd atual ou home.
func New() *Client {
	wd, err := os.Getwd()
	if err != nil || wd == "" {
		wd, _ = os.UserHomeDir()
	}
	return &Client{workDir: wd, seq: -1}
}

func lineEnding() string {
	if runtime.GOOS == "windows" {
		return "\r\n"
	}
	return "\n"
}

// decodeConsoleLine converte uma linha lida do stdout/stderr do cmd.exe (geralmente CP850 OEM)
// para UTF-8. Se já for UTF-8 válido, devolve como está.
func decodeConsoleLine(raw []byte) string {
	raw = bytes.TrimRight(raw, "\r\n")
	if len(raw) == 0 {
		return ""
	}
	if runtime.GOOS != "windows" {
		return string(raw)
	}
	if utf8.Valid(raw) {
		return string(raw)
	}
	// cmd.exe no Windows costuma emitir OEM (850); em alguns fluxos aparece Windows-1252.
	if out, err := charmap.CodePage850.NewDecoder().Bytes(raw); err == nil && len(out) > 0 {
		return string(out)
	}
	if out, err := charmap.Windows1252.NewDecoder().Bytes(raw); err == nil && len(out) > 0 {
		return string(out)
	}
	return string(raw)
}

func (c *Client) procRunning() bool {
	return c.cmd != nil && c.cmd.Process != nil && c.cmd.ProcessState == nil
}

func (c *Client) nextSeq() int {
	c.seqMu.Lock()
	defer c.seqMu.Unlock()
	c.seq++
	return c.seq
}

func (c *Client) resetSeq() {
	c.seqMu.Lock()
	defer c.seqMu.Unlock()
	c.seq = -1
}

func (c *Client) disposeLocked() {
	if c.stdin != nil {
		_ = c.stdin.Close()
		c.stdin = nil
	}
	if c.cmd != nil && c.cmd.Process != nil {
		_ = c.cmd.Process.Kill()
		_ = c.cmd.Wait()
	}
	c.cmd = nil
}

// Execute trata cd, quit, escrita em stdin se o processo anterior ainda estiver vivo, ou inicia cmd.exe /c ou sh -c.
// emit pode ser chamado de goroutines; o chamador deve serializar envio STOMP.
func (c *Client) Execute(ctx context.Context, line string, emit func(ConsoleResponse)) error {
	s := strings.TrimSpace(line)

	if strings.HasPrefix(s, "cd ") {
		return c.handleCD(strings.TrimSpace(s[3:]), emit)
	}
	if strings.EqualFold(s, "quit") {
		c.mu.Lock()
		c.disposeLocked()
		wd := c.workDir
		c.mu.Unlock()
		emit(ConsoleResponse{
			CurrentDirectory: wd,
			Finished:         true,
			Output:           "",
			Sequence:         0,
		})
		return nil
	}

	c.mu.Lock()
	if c.procRunning() && c.stdin != nil {
		_, err := io.WriteString(c.stdin, s+lineEnding())
		c.mu.Unlock()
		return err
	}
	c.disposeLocked()
	c.resetSeq()
	c.mu.Unlock()

	if runtime.GOOS == "windows" {
		return c.runAndPump(ctx, exec.CommandContext(ctx, "cmd.exe", "/c", s), emit)
	}
	return c.runAndPump(ctx, exec.CommandContext(ctx, "sh", "-c", s), emit)
}

func (c *Client) runAndPump(ctx context.Context, cmd *exec.Cmd, emit func(ConsoleResponse)) error {
	c.mu.Lock()
	c.cmd = cmd
	c.cmd.Dir = c.workDir
	stdout, err := c.cmd.StdoutPipe()
	if err != nil {
		c.cmd = nil
		c.mu.Unlock()
		return err
	}
	stderr, err := c.cmd.StderrPipe()
	if err != nil {
		c.cmd = nil
		c.mu.Unlock()
		return err
	}
	in, err := c.cmd.StdinPipe()
	if err != nil {
		c.cmd = nil
		c.mu.Unlock()
		return err
	}
	c.stdin = in
	if err := c.cmd.Start(); err != nil {
		c.disposeLocked()
		c.mu.Unlock()
		return err
	}
	c.mu.Unlock()

	c.pumpOutput(stdout, stderr, emit)
	return nil
}

func (c *Client) handleCD(path string, emit func(ConsoleResponse)) error {
	c.mu.Lock()
	defer c.mu.Unlock()

	var target string
	switch path {
	case "..":
		parent := filepath.Dir(c.workDir)
		if parent != "" && parent != c.workDir {
			target = filepath.Clean(parent)
		} else {
			target = c.workDir
		}
	default:
		if filepath.IsAbs(path) {
			target = filepath.Clean(path)
		} else {
			target = filepath.Clean(filepath.Join(c.workDir, path))
		}
	}

	st, err := os.Stat(target)
	if err != nil || !st.IsDir() {
		emit(ConsoleResponse{
			OutputError:      "Diretório " + path + " não existe",
			CurrentDirectory: c.workDir,
			Finished:         true,
			Sequence:         0,
		})
		return nil
	}
	c.workDir = target
	emit(ConsoleResponse{
		Output:           c.workDir,
		CurrentDirectory: c.workDir,
		Finished:         true,
		Sequence:         0,
	})
	return nil
}

func (c *Client) pumpOutput(stdout, stderr io.ReadCloser, emit func(ConsoleResponse)) {
	var wg sync.WaitGroup
	wg.Add(2)

	pump := func(r io.Reader, isErr bool) {
		defer wg.Done()
		br := bufio.NewReader(r)
		for {
			raw, err := br.ReadBytes('\n')
			if len(raw) > 0 {
				text := decodeConsoleLine(raw)
				c.mu.Lock()
				wd := c.workDir
				c.mu.Unlock()
				seq := c.nextSeq()
				cr := ConsoleResponse{
					CurrentDirectory: wd,
					Finished:         false,
					Sequence:         seq,
				}
				if isErr {
					cr.OutputError = text
				} else {
					cr.Output = text
				}
				emit(cr)
			}
			if err != nil {
				if err == io.EOF {
					break
				}
				break
			}
		}
	}

	go pump(stdout, false)
	go pump(stderr, true)
	wg.Wait()

	c.mu.Lock()
	if c.stdin != nil {
		_ = c.stdin.Close()
		c.stdin = nil
	}
	cmd := c.cmd
	c.cmd = nil
	wd := c.workDir
	seq := c.nextSeq()
	c.mu.Unlock()

	if cmd != nil {
		_ = cmd.Wait()
	}

	emit(ConsoleResponse{
		CurrentDirectory: wd,
		Finished:         true,
		Output:           "",
		OutputError:      "",
		Sequence:         seq,
	})
}
