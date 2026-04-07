//go:build windows

package install

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"syscall"
)

// startRunnerDetached inicia run.cmd sem bloquear (runner fica online sem serviço).
func startRunnerDetached(runnerRoot string) error {
	run := filepath.Join(runnerRoot, "run.cmd")
	if _, err := os.Stat(run); err != nil {
		return fmt.Errorf("run.cmd: %w", err)
	}
	// start "" /B — sem nova janela; Dir define a pasta de trabalho do runner.
	cmd := exec.Command("cmd.exe", "/c", "start", "", "/B", run)
	cmd.Dir = runnerRoot
	cmd.SysProcAttr = &syscall.SysProcAttr{
		CreationFlags: syscall.CREATE_NEW_PROCESS_GROUP,
	}
	if err := cmd.Start(); err != nil {
		return err
	}
	return cmd.Process.Release()
}
