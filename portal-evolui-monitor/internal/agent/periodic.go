package agent

import (
	"context"
	"log"
	"net/http"
	"time"

	"portal-evolui-monitor/internal/config"
	"portal-evolui-monitor/internal/healthcheck"
	"portal-evolui-monitor/internal/logutil"
	"portal-evolui-monitor/internal/portalauth"
	"portal-evolui-monitor/internal/portalcheck"
)

// RunPeriodicHealthCheck agenda POST /api/admin/health-checker/check a cada healthCheckInterval (minutos).
// Deve ser chamado após RegisterSubscriptions; não bloqueia (inicia goroutine interna via Loops.StartPeriodic).
func RunPeriodicHealthCheck(ctx context.Context, loops *Loops, httpClient *http.Client, cfg *config.Config) {
	if httpClient == nil {
		httpClient = http.DefaultClient
	}
	interval := time.Duration(cfg.HealthCheckIntervalMinutes) * time.Minute
	if interval <= 0 {
		interval = 2 * time.Minute
	}

	loops.SetPeriodicFunc(func(loopCtx context.Context) {
		ticker := time.NewTicker(interval)
		defer ticker.Stop()

		runOnce := func() {
			if cfg.HealthCheckerID == nil || *cfg.HealthCheckerID <= 0 {
				return
			}
			if len(cfg.SnapshotJSON) == 0 {
				return
			}
			tok, err := portalauth.LoginHealthChecker(loopCtx, httpClient, cfg.BaseURL, cfg.Login, cfg.Password, cfg.HealthCheckerID)
			if err != nil {
				log.Printf("monitoramento periódico: login: %v", err)
				return
			}
			body, err := healthcheck.BuildCheckDTO(loopCtx, cfg, httpClient)
			if err != nil {
				log.Printf("monitoramento periódico: montar check: %v", err)
				return
			}
			if err := portalcheck.PostCheck(loopCtx, httpClient, cfg.BaseURL, tok, body); err != nil {
				log.Printf("monitoramento periódico: POST /check: %v", err)
				return
			}
			logutil.V("monitoramento periódico: estado enviado ao portal (intervalo %s)", interval)
		}

		runOnce()
		for {
			select {
			case <-loopCtx.Done():
				return
			case <-ticker.C:
				runOnce()
			}
		}
	})

	loops.StartPeriodic(ctx)
}
