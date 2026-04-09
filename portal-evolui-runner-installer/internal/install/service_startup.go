package install

import (
	"context"
	"fmt"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"

	"portal-evolui-runner-installer/internal/wsjson"
)

func serviceStartAtBoot(cfg wsjson.InstallConfig) bool {
	if cfg.ServiceStartAtBoot == nil {
		return true
	}
	return *cfg.ServiceStartAtBoot
}

// applyRunnerServiceStartAtBoot lê o nome da unidade/serviço no ficheiro .service criado pelo pacote oficial
// (após config com serviço no Windows; após svc.sh install no Linux) e ajusta início automático no boot.
func applyRunnerServiceStartAtBoot(ctx context.Context, runnerRoot string, startAtBoot bool) error {
	path := filepath.Join(runnerRoot, ".service")
	b, err := os.ReadFile(path)
	if err != nil {
		return fmt.Errorf("ler .service em %s: %w", runnerRoot, err)
	}
	name := parseDotServiceFileName(string(b))
	if name == "" {
		return fmt.Errorf(".service vazio")
	}
	log.Printf("[install] serviço ou unidade em .service: %s", name)
	if runtime.GOOS == "windows" {
		return windowsServiceSetStartMode(ctx, name, startAtBoot)
	}
	return linuxServiceSetEnabled(ctx, name, startAtBoot)
}

// parseDotServiceFileName extrai o nome da unidade/serviço do ficheiro criado pelo runner (uma linha, UTF-8 sem BOM).
func parseDotServiceFileName(raw string) string {
	s := strings.TrimSpace(raw)
	s = strings.TrimPrefix(s, "\uFEFF")
	if i := strings.IndexAny(s, "\r\n"); i >= 0 {
		s = strings.TrimSpace(s[:i])
	}
	return s
}

func linuxServiceSetEnabled(ctx context.Context, unit string, enable bool) error {
	args := []string{"disable", unit}
	if enable {
		args = []string{"enable", unit}
		log.Printf("[install] systemctl enable %s (início no boot)", unit)
	} else {
		log.Printf("[install] systemctl disable %s (sem início automático no boot)", unit)
	}
	cmd := exec.CommandContext(ctx, "systemctl", args...)
	out, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("systemctl %v: %w\n%s", args, err, string(out))
	}
	return nil
}

func windowsServiceSetStartMode(ctx context.Context, serviceName string, auto bool) error {
	if auto {
		// O config.cmd do runner já regista o serviço como Automatic (delayed start) via APIs Win32,
		// o que cumpre “iniciar no arranque”. Forçar `sc config ... start= auto` depois disso falha
		// em alguns hosts (sc exit 1639, “campo start= inválido”) por interação com início diferido.
		log.Printf("[install] início no boot solicitado: mantendo configuração do config.cmd (Automatic delayed)")
		return nil
	}
	log.Printf("[install] definindo serviço Windows como Manual (sem início automático no boot): sc config … start= demand")
	// sc.exe exige espaço após "start=" (ver ajuda: um espaço entre = e o valor).
	cmd := exec.CommandContext(ctx, "sc.exe", "config", serviceName, "start= demand")
	out, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("sc config: %w\n%s", err, string(out))
	}
	log.Printf("[install] sc config concluído (serviço em Manual)")
	return nil
}
