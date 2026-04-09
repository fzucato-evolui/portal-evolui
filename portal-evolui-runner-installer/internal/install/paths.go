package install

import (
	"path/filepath"
	"strings"
)

// ResolveWorkDirAbs resolve a pasta --work: absoluta ou relativa a runnerInstallFolder.
func ResolveWorkDirAbs(runnerInstall, work string) string {
	runnerInstall = strings.TrimSpace(runnerInstall)
	work = strings.TrimSpace(work)
	if work == "" {
		work = "_work"
	}
	if filepath.IsAbs(work) {
		return filepath.Clean(work)
	}
	if runnerInstall == "" {
		return filepath.Clean(work)
	}
	return filepath.Clean(filepath.Join(runnerInstall, work))
}
