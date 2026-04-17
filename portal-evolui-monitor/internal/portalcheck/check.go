package portalcheck

import (
	"bytes"
	"context"
	"fmt"
	"io"
	"net/http"
	"strings"
)

// PostCheck envia POST /api/admin/health-checker/check com JWT ROLE_HEALTHCHECKER (paridade WebClientController.saveChecker).
func PostCheck(ctx context.Context, client *http.Client, baseURL, token string, body []byte) error {
	if client == nil {
		client = http.DefaultClient
	}
	ep := strings.TrimRight(strings.TrimSpace(baseURL), "/") + "/api/admin/health-checker/check"
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, ep, bytes.NewReader(body))
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+token)

	resp, err := client.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	b, _ := io.ReadAll(resp.Body)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return fmt.Errorf("health-checker/check: HTTP %d: %s", resp.StatusCode, strings.TrimSpace(string(b)))
	}
	return nil
}
