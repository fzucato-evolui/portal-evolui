package supervisor

import (
	"context"
	"crypto/tls"
	"log"
	"math/rand"
	"net/http"
	"time"

	"portal-evolui-monitor/internal/agent"
	"portal-evolui-monitor/internal/config"
	"portal-evolui-monitor/internal/logutil"
	"portal-evolui-monitor/internal/portalauth"
	"portal-evolui-monitor/internal/stomp"
	"portal-evolui-monitor/internal/urlutil"
)

// Run mantém login + STOMP com reconexão (backoff + jitter).
func Run(ctx context.Context, cfg *config.Config) error {
	rng := rand.New(rand.NewSource(time.Now().UnixNano()))
	backoff := cfg.MinBackoff

	httpTransport := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: cfg.SkipTLSVerify},
	}
	httpClient := &http.Client{
		Timeout:   cfg.HTTPTimeout,
		Transport: httpTransport,
	}

	loops := agent.NewLoops()

	for {
		select {
		case <-ctx.Done():
			return ctx.Err()
		default:
		}

		token := cfg.JWT
		if token == "" {
			var err error
			token, err = portalauth.LoginHealthChecker(ctx, httpClient, cfg.BaseURL, cfg.Login, cfg.Password, cfg.HealthCheckerID)
			if err != nil {
				log.Printf("login-health-checker: %v", err)
				sleepBackoff(ctx, &backoff, cfg.MinBackoff, cfg.MaxBackoff, rng)
				continue
			}
			logutil.V("login OK (token obtido)")
		}

		wsURL := cfg.WSURL
		if wsURL == "" {
			var err error
			wsURL, err = urlutil.DeriveWebSocketURL(cfg.BaseURL)
			if err != nil {
				log.Printf("URL WebSocket: %v", err)
				sleepBackoff(ctx, &backoff, cfg.MinBackoff, cfg.MaxBackoff, rng)
				continue
			}
		}

		stompClient, err := stomp.Dial(wsURL, stomp.DialOpts{
			JWT:           token,
			Identifier:    cfg.Identifier,
			SkipTLSVerify: cfg.SkipTLSVerify,
		})
		if err != nil {
			log.Printf("STOMP: %v", err)
			sleepBackoff(ctx, &backoff, cfg.MinBackoff, cfg.MaxBackoff, rng)
			continue
		}

		backoff = cfg.MinBackoff
		sess := agent.NewSession(cfg.PairingJWT)
		if err := agent.RegisterSubscriptions(ctx, stompClient, cfg, sess, loops, httpClient); err != nil {
			log.Printf("subscribe: %v", err)
			_ = stompClient.Close()
			sleepBackoff(ctx, &backoff, cfg.MinBackoff, cfg.MaxBackoff, rng)
			continue
		}

		if cfg.SendHeyOnConnect {
			if err := agent.SendHey(stompClient, cfg); err != nil {
				log.Printf("HEY: %v", err)
			} else {
				logutil.V("HEY enviado para o operador (fluxo init)")
			}
		}

		agent.RunPeriodicHealthCheck(ctx, loops, httpClient, cfg)

		logutil.V("STOMP conectado; identifier=%q", cfg.Identifier)

		select {
		case <-ctx.Done():
			loops.StopRealtime()
			loops.StopPeriodic()
			_ = stompClient.Close()
			return ctx.Err()
		case <-stompClient.Closed():
			loops.StopRealtime()
			loops.StopPeriodic()
			logutil.V("conexão STOMP encerrada; reconectando…")
			sleepBackoff(ctx, &backoff, cfg.MinBackoff, cfg.MaxBackoff, rng)
		}
	}
}

func sleepBackoff(ctx context.Context, cur *time.Duration, min, max time.Duration, rng *rand.Rand) {
	j := time.Duration(1+rng.Intn(500)) * time.Millisecond
	d := *cur + j
	select {
	case <-ctx.Done():
	case <-time.After(d):
	}
	*cur *= 2
	if *cur > max {
		*cur = max
	}
	if *cur < min {
		*cur = min
	}
}
