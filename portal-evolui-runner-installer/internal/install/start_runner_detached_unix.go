//go:build !windows

package install

import (
	"fmt"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"syscall"
)

// startRunnerDetached inicia run.sh sem bloquear (runner fica online sem serviço).
func startRunnerDetached(runnerRoot string) error {
	run := filepath.Join(runnerRoot, "run.sh")
	if _, err := os.Stat(run); err != nil {
		return fmt.Errorf("run.sh: %w", err)
	}
	_ = os.Chmod(run, 0755)
	cmd := exec.Command(run)
	cmd.Dir = runnerRoot
	cmd.Env = append(os.Environ(), "RUNNER_ALLOW_RUNASROOT=1")
	cmd.SysProcAttr = &syscall.SysProcAttr{Setsid: true}
	if err := cmd.Start(); err != nil {
		return err
	}
	log.Printf("[install] run.sh iniciado em segundo plano")
	return cmd.Process.Release()
}
