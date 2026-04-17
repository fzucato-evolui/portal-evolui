package config

import (
	"encoding/json"
	"fmt"
	"sort"
	"strings"
)

// VerifySummary é um resumo seguro do HealthCheckerConfigDTO (sem senhas nem certificados).
type VerifySummary struct {
	ID                  *int64
	Host                string
	Identifier          string
	Description         string
	HealthCheckInterval int
	Login               string
	LoginExtraInfo      *int64
	ModuleCount         int
	Modules             []VerifyModuleSummary
	AlertTypes          []string
	AlertDetails        []string // ex.: "CPU(max=85%)"
	HasSystemInfo       bool
	SystemInfoPreview   string // uma linha curta se existir
}

// VerifyModuleSummary campos não sensíveis por módulo.
type VerifyModuleSummary struct {
	ID          *int64
	ModuleType  string
	Identifier  string
	Description string
	// CommandAddress truncado só para confirmar leitura (pode ser URL interna).
	CommandAddressPreview string
	// AcceptablePatternLen confirma que o padrão regex veio no JSON (não exibe o padrão).
	AcceptablePatternLen int
}

type verifyLoginJSON struct {
	Login     string `json:"login"`
	ExtraInfo *int64 `json:"extraInfo"`
}

type verifyModuleJSON struct {
	ID                        *int64 `json:"id"`
	ModuleType                string `json:"moduleType"`
	Identifier                string `json:"identifier"`
	Description               string `json:"description"`
	CommandAddress            string `json:"commandAddress"`
	AcceptableResponsePattern string `json:"acceptableResponsePattern"`
}

type verifyRootJSON struct {
	ID                  *int64                     `json:"id"`
	Host                string                     `json:"host"`
	Identifier          string                     `json:"identifier"`
	Description         string                     `json:"description"`
	HealthCheckInterval *int                       `json:"healthCheckInterval"`
	Login               verifyLoginJSON            `json:"login"`
	Modules             []verifyModuleJSON         `json:"modules"`
	Alerts              map[string]json.RawMessage `json:"alerts"`
	SystemInfo          json.RawMessage            `json:"systemInfo"`
}

// ParseVerifySummary decodifica o JSON completo do DTO e monta resumo sem senha.
func ParseVerifySummary(raw []byte) (*VerifySummary, error) {
	var root verifyRootJSON
	if err := json.Unmarshal(raw, &root); err != nil {
		return nil, fmt.Errorf("JSON config: %w", err)
	}
	out := &VerifySummary{
		ID:             root.ID,
		Host:           strings.TrimSpace(root.Host),
		Identifier:     strings.TrimSpace(root.Identifier),
		Description:    strings.TrimSpace(root.Description),
		Login:          strings.TrimSpace(root.Login.Login),
		LoginExtraInfo: root.Login.ExtraInfo,
	}
	if root.HealthCheckInterval != nil && *root.HealthCheckInterval > 0 {
		out.HealthCheckInterval = *root.HealthCheckInterval
	} else {
		out.HealthCheckInterval = 2
	}
	if len(root.Modules) > 0 {
		out.ModuleCount = len(root.Modules)
		for _, m := range root.Modules {
			prev := strings.TrimSpace(m.CommandAddress)
			if len(prev) > 96 {
				prev = prev[:96] + "…"
			}
			pat := strings.TrimSpace(m.AcceptableResponsePattern)
			out.Modules = append(out.Modules, VerifyModuleSummary{
				ID:                    m.ID,
				ModuleType:            strings.TrimSpace(m.ModuleType),
				Identifier:            strings.TrimSpace(m.Identifier),
				Description:           strings.TrimSpace(m.Description),
				CommandAddressPreview: prev,
				AcceptablePatternLen:  len(pat),
			})
		}
	}
	if len(root.Alerts) > 0 {
		for k := range root.Alerts {
			out.AlertTypes = append(out.AlertTypes, k)
		}
		sort.Strings(out.AlertTypes)
		for _, k := range out.AlertTypes {
			rawAlert := root.Alerts[k]
			var ac struct {
				MaxPercentual int `json:"maxPercentual"`
			}
			_ = json.Unmarshal(rawAlert, &ac)
			out.AlertDetails = append(out.AlertDetails, fmt.Sprintf("%s(max=%d%%)", k, ac.MaxPercentual))
		}
	}
	if len(root.SystemInfo) > 0 && string(root.SystemInfo) != "null" {
		out.HasSystemInfo = true
		out.SystemInfoPreview = truncateOneLine(string(root.SystemInfo), 120)
	}
	return out, nil
}

func truncateOneLine(s string, n int) string {
	s = strings.ReplaceAll(strings.TrimSpace(s), "\n", " ")
	s = strings.ReplaceAll(s, "\r", "")
	if len(s) <= n {
		return s
	}
	return s[:n] + "…"
}
