package portalauth

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
)

// LoginHealthChecker chama POST /api/public/auth/login-health-checker e devolve o accessToken JWT.
func LoginHealthChecker(ctx context.Context, httpClient *http.Client, baseURL, login, password string, healthCheckerID *int64) (accessToken string, err error) {
	if httpClient == nil {
		httpClient = http.DefaultClient
	}
	ep := strings.TrimRight(strings.TrimSpace(baseURL), "/") + "/api/public/auth/login-health-checker"

	body := map[string]any{
		"login":    login,
		"password": password,
	}
	if healthCheckerID != nil && *healthCheckerID > 0 {
		body["extraInfo"] = *healthCheckerID
	}
	raw, err := json.Marshal(body)
	if err != nil {
		return "", err
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, ep, bytes.NewReader(raw))
	if err != nil {
		return "", err
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := httpClient.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	b, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return "", fmt.Errorf("login-health-checker: HTTP %d: %s", resp.StatusCode, strings.TrimSpace(string(b)))
	}

	var out struct {
		AccessToken string `json:"accessToken"`
	}
	if err := json.Unmarshal(b, &out); err != nil {
		return "", fmt.Errorf("decodificar resposta JSON: %w", err)
	}
	if out.AccessToken == "" {
		return "", fmt.Errorf("resposta sem accessToken")
	}
	return out.AccessToken, nil
}
