package healthcheck

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"

	"portal-evolui-monitor/internal/config"
	"portal-evolui-monitor/internal/sysinfo"
)

// ModuleConfig espelha HealthCheckerModuleConfigDTO (shared) para probe e POST /check.
type ModuleConfig struct {
	ID                        *int64 `json:"id"`
	ModuleType                string `json:"moduleType"`
	CommandAddress            string `json:"commandAddress"`
	AcceptableResponsePattern string `json:"acceptableResponsePattern"`
}

type snapshotParse struct {
	Modules []ModuleConfig `json:"modules"`
	Alerts  map[string]struct {
		MaxPercentual int `json:"maxPercentual"`
	} `json:"alerts"`
}

// BuildCheckDTO monta o JSON do POST /check: alertas de limiar + resultado dos módulos (paridade HealthCheckController).
func BuildCheckDTO(ctx context.Context, cfg *config.Config, httpClient *http.Client) ([]byte, error) {
	if httpClient == nil {
		httpClient = http.DefaultClient
	}
	if cfg.HealthCheckerID == nil || *cfg.HealthCheckerID <= 0 {
		return nil, fmt.Errorf("health checker id ausente")
	}
	if len(cfg.SnapshotJSON) == 0 {
		return nil, fmt.Errorf("snapshot de config ausente")
	}

	var snap snapshotParse
	if err := json.Unmarshal(cfg.SnapshotJSON, &snap); err != nil {
		return nil, fmt.Errorf("snapshot JSON: %w", err)
	}

	metrics, err := sysinfo.SampleAlertMetrics(ctx)
	if err != nil {
		return nil, err
	}

	dto := checkDTO{
		ID: *cfg.HealthCheckerID,
	}

	// Alertas por limiar (MEMORY, CPU, DISK_USAGE, OPENED_FILES).
	for key, ac := range snap.Alerts {
		if ac.MaxPercentual <= 0 {
			continue
		}
		switch key {
		case "MEMORY":
			if metrics.MemoryUsedPercent > ac.MaxPercentual {
				dto.Alerts = append(dto.Alerts, alertResult{
					AlertType: "MEMORY",
					Health:    false,
					Error:     fmt.Sprintf("Consumo de %d%%", metrics.MemoryUsedPercent),
				})
			}
		case "CPU":
			if metrics.CPUPercent > ac.MaxPercentual {
				dto.Alerts = append(dto.Alerts, alertResult{
					AlertType: "CPU",
					Health:    false,
					Error:     fmt.Sprintf("Consumo de %d%%", metrics.CPUPercent),
				})
			}
		case "DISK_USAGE":
			for label, pct := range metrics.DiskUsagePercent {
				if pct > ac.MaxPercentual {
					dto.Alerts = append(dto.Alerts, alertResult{
						AlertType: "DISK_USAGE",
						Health:    false,
						Error:     fmt.Sprintf("Disco %s: Consumo de %d%%", label, pct),
					})
				}
			}
		case "OPENED_FILES":
			if metrics.OpenFilesPercent >= 0 && metrics.OpenFilesPercent > ac.MaxPercentual {
				dto.Alerts = append(dto.Alerts, alertResult{
					AlertType: "OPENED_FILES",
					Health:    false,
					Error:     fmt.Sprintf("Consumo de %d%%", metrics.OpenFilesPercent),
				})
			}
		}
	}

	for i := range snap.Modules {
		dto.Modules = append(dto.Modules, CheckModule(ctx, httpClient, snap.Modules[i]))
	}

	return json.Marshal(dto)
}
