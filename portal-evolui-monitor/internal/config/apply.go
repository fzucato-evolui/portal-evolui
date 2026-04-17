package config

import (
	"encoding/json"
	"fmt"

	"portal-evolui-monitor/internal/urlutil"
)

// healthCheckerConfigJSON subconjunto do HealthCheckerConfigDTO (shared) necessário ao agente.
type healthCheckerConfigJSON struct {
	ID                  *int64 `json:"id"`
	Host                string `json:"host"`
	Identifier          string `json:"identifier"`
	HealthCheckInterval *int   `json:"healthCheckInterval"`
	Login               struct {
		Login     string `json:"login"`
		Password  string `json:"password"`
		ExtraInfo *int64 `json:"extraInfo"`
	} `json:"login"`
}

// ApplyFromHealthCheckerConfigJSON aplica host, credenciais e ids ao Config após ler arquivo ou STOMP.
func ApplyFromHealthCheckerConfigJSON(cfg *Config, raw []byte) error {
	var m healthCheckerConfigJSON
	cfg.SnapshotJSON = append([]byte(nil), raw...)

	if err := json.Unmarshal(raw, &m); err != nil {
		return fmt.Errorf("JSON HealthCheckerConfigDTO: %w", err)
	}
	if m.Host == "" {
		return fmt.Errorf("config sem host")
	}
	if err := urlutil.ValidateHTTPBaseURL(m.Host); err != nil {
		return err
	}
	cfg.BaseURL = m.Host
	if m.Identifier != "" {
		cfg.Identifier = m.Identifier
	}
	if m.ID != nil && *m.ID > 0 {
		id := *m.ID
		cfg.HealthCheckerID = &id
	} else if m.Login.ExtraInfo != nil && *m.Login.ExtraInfo > 0 {
		e := *m.Login.ExtraInfo
		cfg.HealthCheckerID = &e
	} else {
		cfg.HealthCheckerID = nil
	}
	cfg.Login = m.Login.Login
	cfg.Password = m.Login.Password

	if m.HealthCheckInterval != nil && *m.HealthCheckInterval > 0 {
		cfg.HealthCheckIntervalMinutes = *m.HealthCheckInterval
	} else {
		cfg.HealthCheckIntervalMinutes = 2
	}
	return nil
}
