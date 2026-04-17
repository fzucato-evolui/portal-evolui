package main

import (
	"bufio"
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"os/signal"
	"strings"
	"syscall"

	"portal-evolui-monitor/internal/config"
	"portal-evolui-monitor/internal/hwuuid"
	"portal-evolui-monitor/internal/initflow"
	"portal-evolui-monitor/internal/logutil"
	"portal-evolui-monitor/internal/store"
	"portal-evolui-monitor/internal/supervisor"
	"portal-evolui-monitor/internal/sysinfo"
)

func main() {
	cfg, err := config.Parse(os.Args[1:])
	if err != nil {
		log.Fatalf("config: %v", err)
	}
	logutil.SetVerbose(cfg.Verbose)

	// Paridade MainController (health-checker Java): exige privilégios elevados para WMI/sessões/etc.
	// -verify-config não precisa (só valida ficheiro local).
	if cfg.VerifyConfigPath == "" {
		if cfg.AllowNonElevated {
			log.Printf("aviso: execução sem privilégios elevados permitida (-allow-non-elevated / EVOLUI_MONITOR_ALLOW_NON_ELEVATED); coleta pode ficar incompleta")
		} else if !sysinfo.IsProcessElevated() {
			log.Fatal("Deve ser executado como administrador (Windows) ou root (Linux), com todos os privilégios, como no health-checker Java. " +
				"Para desenvolvimento: -allow-non-elevated ou EVOLUI_MONITOR_ALLOW_NON_ELEVATED=1.")
		}
	}

	if cfg.VerifyConfigPath != "" {
		raw, err := store.ReadEncryptedConfigFile(cfg.VerifyConfigPath)
		if err != nil {
			log.Fatalf("verify-config: ler/descriptografar %s: %v", cfg.VerifyConfigPath, err)
		}
		tmp := &config.Config{}
		if err := config.ApplyFromHealthCheckerConfigJSON(tmp, raw); err != nil {
			log.Fatalf("verify-config: JSON inválido: %v", err)
		}
		sum, err := config.ParseVerifySummary(raw)
		if err != nil {
			log.Fatalf("verify-config: resumo do DTO: %v", err)
		}
		var hid string
		if tmp.HealthCheckerID != nil {
			hid = fmt.Sprintf("%d", *tmp.HealthCheckerID)
		} else {
			hid = "(omitido)"
		}
		log.Printf("verify-config OK: arquivo=%s (senha do login não exibida)", cfg.VerifyConfigPath)
		log.Printf("  host=%s identifier=%s description=%q", tmp.BaseURL, tmp.Identifier, sum.Description)
		log.Printf("  id=%s health_checker_id(aplicado)=%s login=%s extraInfo(login)=%s",
			formatOptionalID64(sum.ID), hid, sum.Login, formatOptionalID64(sum.LoginExtraInfo))
		log.Printf("  healthCheckInterval=%d minutos", sum.HealthCheckInterval)
		log.Printf("  alertas (%d): %v", len(sum.AlertDetails), sum.AlertDetails)
		log.Printf("  módulos: %d", sum.ModuleCount)
		for i, m := range sum.Modules {
			log.Printf("    [%d] id=%s type=%s identifier=%q description=%q commandAddress=%q acceptablePatternLen=%d",
				i+1, formatOptionalID64(m.ID), m.ModuleType, m.Identifier, m.Description, m.CommandAddressPreview, m.AcceptablePatternLen)
		}
		if sum.HasSystemInfo {
			log.Printf("  systemInfo: presente prévia=%q", sum.SystemInfoPreview)
		} else {
			log.Printf("  systemInfo: ausente ou null")
		}
		os.Exit(0)
	}

	if cfg.ConfigFile != "" && !cfg.Init {
		raw, err := store.ReadEncryptedConfigFile(cfg.ConfigFile)
		if err != nil {
			log.Fatalf("ler %s: %v", cfg.ConfigFile, err)
		}
		if err := config.ApplyFromHealthCheckerConfigJSON(cfg, raw); err != nil {
			log.Fatalf("config: %v", err)
		}
		logutil.V("config carregada de %s (modo run)", cfg.ConfigFile)
	}

	if cfg.DumpSystemInfoPath != "" {
		if cfg.Identifier == "" {
			id, err := hwuuid.FromOS()
			if err != nil {
				log.Fatalf("dump-system-info: identifier: %v (informe -identifier ou EVOLUI_MONITOR_IDENTIFIER)", err)
			}
			cfg.Identifier = id
			log.Printf("dump-system-info: identifier hardware (OSHI): %s", cfg.Identifier)
		} else {
			log.Printf("dump-system-info: identifier: %s", cfg.Identifier)
		}
		ctx := context.Background()
		raw, err := sysinfo.BuildHealthCheckerSystemInfoJSON(ctx, cfg.Identifier)
		if err != nil {
			log.Fatalf("dump-system-info: leitura: %v", err)
		}
		var buf bytes.Buffer
		if err := json.Indent(&buf, raw, "", "  "); err != nil {
			log.Fatalf("dump-system-info: indent: %v", err)
		}
		if err := os.WriteFile(cfg.DumpSystemInfoPath, buf.Bytes(), 0644); err != nil {
			log.Fatalf("dump-system-info: gravar %s: %v", cfg.DumpSystemInfoPath, err)
		}
		logutil.V("dump-system-info: gravado %s (%d bytes)", cfg.DumpSystemInfoPath, buf.Len())
		os.Exit(0)
	}

	if cfg.Init {
		token := strings.TrimSpace(cfg.InitToken)
		if token == "" && cfg.InitTokenFile != "" {
			b, err := os.ReadFile(cfg.InitTokenFile)
			if err != nil {
				log.Fatalf("init-token-file: %v", err)
			}
			token = strings.TrimSpace(string(b))
		}
		if token == "" {
			log.Println("Cole o token exibido no portal (generate-token) e pressione Enter:")
			sc := bufio.NewScanner(os.Stdin)
			if sc.Scan() {
				token = strings.TrimSpace(sc.Text())
			}
		}
		if token == "" {
			log.Fatal("token vazio: use -init-token, -init-token-file, EVOLUI_MONITOR_INIT_TOKEN ou stdin")
		}
		if err := initflow.ApplyEncryptedToken(cfg, token); err != nil {
			log.Fatalf("token init: %v", err)
		}
		log.Printf("init: host=%s login=%s (senha definida pelo token)", cfg.BaseURL, cfg.Login)
	}

	if cfg.Identifier == "" {
		id, err := hwuuid.FromOS()
		if err != nil {
			log.Fatalf("identifier: %v (informe -identifier ou EVOLUI_MONITOR_IDENTIFIER)", err)
		}
		cfg.Identifier = id
		logutil.V("identifier hardware (OSHI): %s", cfg.Identifier)
	} else {
		logutil.V("identifier: %s", cfg.Identifier)
	}

	ctx, cancel := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer cancel()

	if err := supervisor.Run(ctx, cfg); err != nil && err != context.Canceled {
		log.Fatalf("supervisor: %v", err)
	}
}

func formatOptionalID64(p *int64) string {
	if p == nil {
		return "(null)"
	}
	return fmt.Sprintf("%d", *p)
}
