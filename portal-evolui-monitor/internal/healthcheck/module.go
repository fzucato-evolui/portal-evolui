package healthcheck

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os/exec"
	"regexp"
	"runtime"
	"sort"
	"strings"
	"time"
)

const moduleAlertType = "MODULE"

// CheckModule executa um teste WEB ou EXECUTABLE (paridade HealthCheckController.checkModule).
func CheckModule(ctx context.Context, httpClient *http.Client, mod ModuleConfig) ModuleResult {
	out := ModuleResult{
		AlertType: moduleAlertType,
		Health:    false,
	}
	// Paridade JAR: checkModule roda mesmo sem id (test-config antes de persistir).
	if mod.ID != nil && *mod.ID > 0 {
		id := *mod.ID
		out.ID = &id
	}
	pattern := strings.TrimSpace(mod.AcceptableResponsePattern)
	if pattern == "" {
		out.Error = "acceptableResponsePattern vazio"
		return out
	}
	re, err := regexp.Compile(pattern)
	if err != nil {
		out.Error = fmt.Sprintf("regex: %v", err)
		return out
	}

	mt := strings.ToUpper(strings.TrimSpace(mod.ModuleType))
	switch mt {
	case "EXECUTABLE":
		s, err := runShell(ctx, mod.CommandAddress, 30*time.Second)
		if err != nil {
			out.Error = err.Error()
			return out
		}
		if re.FindStringIndex(s) != nil {
			out.Health = true
		} else {
			out.Error = s
		}
		return out
	case "WEB":
		if httpClient == nil {
			httpClient = http.DefaultClient
		}
		// Paridade WebClientController.doHealthCheck: regex sobre JSON com STATUS, CONTENT e HEADERS (não só o body).
		s, err := httpWebProbeJSON(ctx, httpClient, mod.CommandAddress, 30*time.Second)
		if err != nil {
			out.Error = err.Error()
			return out
		}
		if re.FindStringIndex(s) != nil {
			out.Health = true
		} else {
			out.Error = truncate(s, 500)
		}
		return out
	default:
		out.Error = "moduleType desconhecido: " + mod.ModuleType
		return out
	}
}

func runShell(ctx context.Context, cmdline string, timeout time.Duration) (string, error) {
	cmdline = strings.TrimSpace(cmdline)
	if cmdline == "" {
		return "", fmt.Errorf("commandAddress vazio")
	}
	cctx, cancel := context.WithTimeout(ctx, timeout)
	defer cancel()
	var cmd *exec.Cmd
	if runtime.GOOS == "windows" {
		cmd = exec.CommandContext(cctx, "cmd", "/c", cmdline)
	} else {
		cmd = exec.CommandContext(cctx, "sh", "-c", cmdline)
	}
	b, err := cmd.Output()
	if err != nil {
		return "", err
	}
	return string(b), nil
}

// javaWebProbeJSON espelha o mapa serializado por WebClientController.doHealthCheck (STATUS, CONTENT, HEADERS).
type javaWebProbeJSON struct {
	STATUS  int          `json:"STATUS"`
	CONTENT string       `json:"CONTENT"`
	HEADERS []headerPair `json:"HEADERS"`
}

type headerPair struct {
	Name  string `json:"name"`
	Value string `json:"value"`
}

// httpWebProbeJSON executa GET e devolve JSON no mesmo espírito do JAR (regex pode casar status, body ou headers).
func httpWebProbeJSON(ctx context.Context, client *http.Client, url string, timeout time.Duration) (string, error) {
	url = strings.TrimSpace(url)
	if url == "" {
		return "", fmt.Errorf("commandAddress vazio")
	}
	cctx, cancel := context.WithTimeout(ctx, timeout)
	defer cancel()
	req, err := http.NewRequestWithContext(cctx, http.MethodGet, url, nil)
	if err != nil {
		return "", err
	}
	resp, err := client.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()
	b, err := io.ReadAll(io.LimitReader(resp.Body, 2<<20))
	if err != nil {
		return "", err
	}
	content := string(b)
	headers := flattenHeaders(resp.Header)
	probe := javaWebProbeJSON{
		STATUS:  resp.StatusCode,
		CONTENT: content,
		HEADERS: headers,
	}
	raw, err := json.Marshal(probe)
	if err != nil {
		return "", err
	}
	return string(raw), nil
}

func flattenHeaders(h http.Header) []headerPair {
	if h == nil {
		return nil
	}
	var keys []string
	for k := range h {
		keys = append(keys, k)
	}
	sort.Strings(keys)
	var out []headerPair
	for _, k := range keys {
		for _, v := range h[k] {
			out = append(out, headerPair{Name: k, Value: v})
		}
	}
	return out
}

func truncate(s string, n int) string {
	s = strings.TrimSpace(s)
	if len(s) <= n {
		return s
	}
	return s[:n] + "…"
}
