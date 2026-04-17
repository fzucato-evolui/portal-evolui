package urlutil

import (
	"fmt"
	"net/url"
	"strings"
)

// ValidateHTTPBaseURL garante http:// ou https:// (login HTTP).
func ValidateHTTPBaseURL(raw string) error {
	u, err := url.Parse(strings.TrimSpace(raw))
	if err != nil {
		return err
	}
	if u.Scheme != "http" && u.Scheme != "https" {
		return fmt.Errorf("base-url: use http ou https, obtido %q", u.Scheme)
	}
	return nil
}

// ValidateWebSocketURL garante ws:// ou wss:// (STOMP).
func ValidateWebSocketURL(raw string) error {
	u, err := url.Parse(strings.TrimSpace(raw))
	if err != nil {
		return err
	}
	if u.Scheme != "ws" && u.Scheme != "wss" {
		return fmt.Errorf("ws-url: use ws ou wss, obtido %q", u.Scheme)
	}
	return nil
}

// DeriveWebSocketURL monta a URL do endpoint STOMP a partir da URL HTTP(S) raiz do portal
// (ex.: https://host/app → wss://host/app/portalEvoluiWebSocket; http → ws).
func DeriveWebSocketURL(httpBase string) (string, error) {
	u, err := url.Parse(strings.TrimSpace(httpBase))
	if err != nil {
		return "", err
	}
	if u.Scheme != "http" && u.Scheme != "https" {
		return "", fmt.Errorf("esquema não suportado: %s", u.Scheme)
	}
	switch u.Scheme {
	case "https":
		u.Scheme = "wss"
	case "http":
		u.Scheme = "ws"
	}
	u.Path = strings.TrimSuffix(u.Path, "/") + "/portalEvoluiWebSocket"
	u.RawQuery = ""
	u.Fragment = ""
	return u.String(), nil
}
