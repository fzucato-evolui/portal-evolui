package healthcheck

// Corpo JSON de POST /api/admin/health-checker/check (HealthCheckerDTO no shared).

type checkDTO struct {
	ID      int64          `json:"id"`
	Modules []ModuleResult `json:"modules,omitempty"`
	Alerts  []alertResult  `json:"alerts,omitempty"`
}

// ModuleResult espelha HealthCheckerModuleDTO (alertType MODULE) no POST /check e em test-config-response.
// id omitido quando o módulo ainda não foi persistido (teste no modal antes do save).
type ModuleResult struct {
	ID        *int64 `json:"id,omitempty"`
	AlertType string `json:"alertType"`
	Health    bool   `json:"health"`
	Error     string `json:"error,omitempty"`
}

type alertResult struct {
	AlertType string `json:"alertType"`
	Health    bool   `json:"health"`
	Error     string `json:"error,omitempty"`
}
