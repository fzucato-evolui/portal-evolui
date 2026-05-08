package install

import (
	"context"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strings"
	"time"

	"portal-evolui-runner-installer/internal/wsjson"
)

const listeningMarker = "Listening for Jobs"

// OnlineProbeDeps liga o WaitForRunnerOnlineCombined ao caminho via frontend:
// AskFrontend dispara um runner-install-online-check-request (uma vez); ResponsesCh entrega
// cada runner-install-online-check-response que chega, até confirmar online ou esgotar.
type OnlineProbeDeps struct {
	AskFrontend func(ctx context.Context, runnerName string) error
	ResponsesCh <-chan wsjson.OnlineCheckResponse
}

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

// WaitForRunnerOnlineCombined aguarda confirmação de "runner online" por dois caminhos paralelos:
// (A) WaitForRunnerOnline lendo logs em _diag (caminho atual);
// (B) frontend executando polling REST contra a API GitHub (mediado por probe).
// Quem confirmar primeiro vence; ambas as goroutines são canceladas via ctx.
// probe pode ser nil para preservar o comportamento legado (só logs).
func WaitForRunnerOnlineCombined(ctx context.Context, runnerRoot, runnerName string,
	timeout time.Duration, probe *OnlineProbeDeps) error {
	ctx, cancel := context.WithTimeout(ctx, timeout)
	defer cancel()

	type result struct {
		source string
		err    error
	}
	ch := make(chan result, 2)

	// FASE 1 (validação inicial): caminho A (leitura de logs em _diag) está DESABILITADO
	// para validar isoladamente o caminho B (frontend → API GitHub).
	// Como reabilitar (rodar em paralelo, primeiro a confirmar vence):
	//   docs/plans/fase1-online-check-via-frontend.md
	// Passos: (1) descomentar a goroutine entre os marcadores abaixo;
	//         (2) remover o early-return logo após os marcadores.
	//
	// >>> INÍCIO BLOCO FASE 1 — CAMINHO A COMENTADO <<<
	/*
		go func() {
			err := WaitForRunnerOnline(ctx, runnerRoot, timeout)
			ch <- result{"logs", err}
		}()
	*/
	// >>> FIM BLOCO FASE 1 — REMOVA TAMBÉM O EARLY-RETURN ABAIXO AO REABILITAR <<<
	if probe == nil || probe.AskFrontend == nil || probe.ResponsesCh == nil || runnerName == "" {
		return fmt.Errorf("verificação por logs (_diag) está desabilitada nesta fase de validação " +
			"e o caminho via frontend não está disponível (probe nulo ou runnerName vazio); " +
			"reabilite o bloco em verify.go conforme docs/plans/fase1-online-check-via-frontend.md")
	}
	log.Printf("[install] FASE 1: verificação por logs desabilitada; aguardando confirmação só via frontend/API GitHub")

	// Caminho B — frontend faz polling e devolve via STOMP.
	if probe != nil && probe.AskFrontend != nil && probe.ResponsesCh != nil && runnerName != "" {
		go func() {
			if err := probe.AskFrontend(ctx, runnerName); err != nil {
				ch <- result{"frontend", fmt.Errorf("falha ao pedir verificação ao frontend: %w", err)}
				return
			}
			for {
				select {
				case <-ctx.Done():
					ch <- result{"frontend", ctx.Err()}
					return
				case r := <-probe.ResponsesCh:
					if r.Found && r.Online {
						log.Printf("[install] frontend confirmou runner online (tentativa %d)", r.Attempt)
						ch <- result{"frontend", nil}
						return
					}
					if r.Exhausted {
						ch <- result{"frontend", fmt.Errorf("frontend esgotou %d tentativas sem confirmar online", r.Attempt)}
						return
					}
					// resposta intermediária — continua aguardando próxima
				}
			}
		}()
	}

	first := <-ch
	cancel() // sinaliza a outra goroutine para encerrar
	if first.err == nil {
		log.Printf("[install] runner online confirmado via %s", first.source)
		return nil
	}
	// se o primeiro caminho falhou, espera brevemente o segundo — pode ainda confirmar online
	select {
	case second := <-ch:
		if second.err == nil {
			log.Printf("[install] runner online confirmado via %s (após falha em %s)", second.source, first.source)
			return nil
		}
		return fmt.Errorf("ambos os caminhos falharam: %s=%v ; %s=%v",
			first.source, first.err, second.source, second.err)
	case <-time.After(time.Second):
		return first.err
	}
}
