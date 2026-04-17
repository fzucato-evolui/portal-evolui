package initflow

import (
	"encoding/json"
	"fmt"
	"strings"

	"portal-evolui-monitor/internal/config"
	"portal-evolui-monitor/internal/crypto"
	"portal-evolui-monitor/internal/urlutil"
)

// connectionJSON espelha HealthCheckerConnectionDTO (shared).
type connectionJSON struct {
	Destination string     `json:"destination"`
	Host        string     `json:"host"`
	Login       loginBlock `json:"login"`
}

type loginBlock struct {
	Login    string `json:"login"`
	Password string `json:"password"`
}

// ApplyEncryptedToken descriptografa o token do admin e preenche cfg (equivalente a MainController.init()).
func ApplyEncryptedToken(cfg *config.Config, hexToken string) error {
	plain, err := crypto.DecryptHex(hexToken, crypto.DefaultKey)
	if err != nil {
		return fmt.Errorf("decrypt token: %w", err)
	}
	var conn connectionJSON
	if err := json.Unmarshal(plain, &conn); err != nil {
		return fmt.Errorf("JSON do token: %w", err)
	}
	if strings.TrimSpace(conn.Host) == "" {
		return fmt.Errorf("token sem host")
	}
	if strings.TrimSpace(conn.Login.Login) == "" {
		return fmt.Errorf("token sem login")
	}
	cfg.BaseURL = strings.TrimSpace(conn.Host)
	if err := urlutil.ValidateHTTPBaseURL(cfg.BaseURL); err != nil {
		return err
	}
	cfg.Login = conn.Login.Login
	cfg.Password = conn.Login.Password
	cfg.PairingJWT = strings.TrimSpace(conn.Destination)
	cfg.SendHeyOnConnect = true
	return nil
}
