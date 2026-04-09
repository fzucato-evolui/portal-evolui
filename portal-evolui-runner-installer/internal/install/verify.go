package install

import (
	"context"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strings"
	"time"
)

const listeningMarker = "Listening for Jobs"

// WaitForRunnerOnline observa os logs em _diag até o listener reportar que está à escuta ou esgotar o prazo.
func WaitForRunnerOnline(ctx context.Context, runnerRoot string, timeout time.Duration) error {
	diagDir := filepath.Join(runnerRoot, "_diag")
	_ = os.MkdirAll(diagDir, 0755)

	deadline := time.Now().Add(timeout)
	var lastSnippet string
	started := time.Now()
	nextProgress := started.Add(30 * time.Second)
	log.Printf("[install] aguardando «%s» em %s (timeout %v)", listeningMarker, diagDir, timeout)

	for time.Now().Before(deadline) {
		select {
		case <-ctx.Done():
			return ctx.Err()
		default:
		}

		if now := time.Now(); !now.Before(nextProgress) {
			log.Printf("[install] ainda aguardando runner online… (%v decorridos)", now.Sub(started).Round(time.Second))
			nextProgress = now.Add(30 * time.Second)
		}

		snippet, err := readLatestDiagSnippet(diagDir, 48*1024)
		if err == nil && snippet != "" {
			lastSnippet = snippet
			if strings.Contains(snippet, listeningMarker) {
				log.Printf("[install] «%s» encontrado nos logs do runner (%v após o início da espera)", listeningMarker, time.Since(started).Round(time.Second))
				return nil
			}
		}

		select {
		case <-ctx.Done():
			return ctx.Err()
		case <-time.After(2 * time.Second):
		}
	}

	detail := lastSnippet
	if detail == "" {
		detail = "(sem ficheiros em _diag ainda — permissões ou serviço podem estar incorretos)"
	}
	return fmt.Errorf(
		"timeout: runner não reportou «%s» em %s. Verifique permissões nas pastas de instalação e de trabalho e journalctl -u 'actions.runner.*'. Trecho:\n%s",
		listeningMarker,
		diagDir,
		truncateForError(detail, 2500),
	)
}

func readLatestDiagSnippet(diagDir string, maxBytes int64) (string, error) {
	entries, err := os.ReadDir(diagDir)
	if err != nil {
		return "", err
	}
	var latestPath string
	var latestMod time.Time
	for _, e := range entries {
		if e.IsDir() {
			continue
		}
		name := strings.ToLower(e.Name())
		if !strings.HasSuffix(name, ".log") {
			continue
		}
		info, err := e.Info()
		if err != nil {
			continue
		}
		if info.ModTime().After(latestMod) {
			latestMod = info.ModTime()
			latestPath = filepath.Join(diagDir, e.Name())
		}
	}
	if latestPath == "" {
		return "", fmt.Errorf("nenhum .log em _diag")
	}
	return tailFile(latestPath, maxBytes)
}

func tailFile(path string, maxBytes int64) (string, error) {
	f, err := os.Open(path)
	if err != nil {
		return "", err
	}
	defer f.Close()
	st, err := f.Stat()
	if err != nil {
		return "", err
	}
	start := int64(0)
	if st.Size() > maxBytes {
		start = st.Size() - maxBytes
	}
	if _, err := f.Seek(start, 0); err != nil {
		return "", err
	}
	buf := make([]byte, maxBytes)
	n, err := f.Read(buf)
	if err != nil && n == 0 {
		return "", err
	}
	return string(buf[:n]), nil
}

func truncateForError(s string, max int) string {
	if len(s) <= max {
		return s
	}
	return s[len(s)-max:]
}
