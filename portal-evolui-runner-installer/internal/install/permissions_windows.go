//go:build windows

package install

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
)

// ApplyInstallPermissions normaliza atributos de leitura/execução na árvore (melhor esforço no Windows).
func ApplyInstallPermissions(runnerRoot, workAbs string) error {
	if err := chmodTreeWindows(runnerRoot); err != nil {
		return fmt.Errorf("permissões na pasta do runner: %w", err)
	}
	if workAbs != "" && !strings.EqualFold(filepath.Clean(workAbs), filepath.Clean(runnerRoot)) {
		if st, err := os.Stat(workAbs); err == nil && st.IsDir() {
			if err := chmodTreeWindows(workAbs); err != nil {
				return fmt.Errorf("permissões na pasta de trabalho: %w", err)
			}
		}
	}
	return nil
}

func chmodTreeWindows(root string) error {
	return filepath.Walk(root, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if info.IsDir() {
			return os.Chmod(path, 0755)
		}
		mode := os.FileMode(0644)
		if info.Mode().Perm()&0111 != 0 || strings.HasSuffix(strings.ToLower(path), ".sh") ||
			strings.HasSuffix(strings.ToLower(path), ".cmd") || strings.HasSuffix(strings.ToLower(path), ".exe") {
			mode = 0755
		}
		return os.Chmod(path, mode)
	})
}
