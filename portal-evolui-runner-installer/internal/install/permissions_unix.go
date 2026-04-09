//go:build !windows

package install

import (
	"fmt"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"syscall"
)

// ApplyInstallPermissions garante modo estável na árvore do runner e na pasta de trabalho.
// Se a instalação foi feita com sudo, repassa a posse ao utilizador invocador (SUDO_UID),
// pois o systemd do GitHub Actions costuma correr como esse utilizador — sem isso, _diag e logs falham.
func ApplyInstallPermissions(runnerRoot, workAbs string) error {
	if err := chmodTree(runnerRoot, 0755, 0644); err != nil {
		return fmt.Errorf("permissões na pasta do runner: %w", err)
	}
	if workAbs != "" && !strings.EqualFold(filepath.Clean(workAbs), filepath.Clean(runnerRoot)) {
		if st, err := os.Stat(workAbs); err == nil && st.IsDir() {
			if err := chmodTree(workAbs, 0755, 0644); err != nil {
				return fmt.Errorf("permissões na pasta de trabalho: %w", err)
			}
		}
	}
	uid, gid, ok := sudoInvokerIDs()
	if ok {
		if err := chownTree(runnerRoot, uid, gid); err != nil {
			return fmt.Errorf("chown na pasta do runner (use sem sudo ou defina utilizador do serviço): %w", err)
		}
		if workAbs != "" && !strings.EqualFold(filepath.Clean(workAbs), filepath.Clean(runnerRoot)) {
			if st, err := os.Stat(workAbs); err == nil && st.IsDir() {
				if err := chownTree(workAbs, uid, gid); err != nil {
					return fmt.Errorf("chown na pasta de trabalho: %w", err)
				}
			}
		}
	}
	return nil
}

func chmodTree(root string, dirMode, fileMode os.FileMode) error {
	return filepath.Walk(root, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if info.IsDir() {
			return os.Chmod(path, dirMode)
		}
		mode := fileMode
		if info.Mode().Perm()&0111 != 0 || strings.HasSuffix(strings.ToLower(path), ".sh") {
			mode = 0755
		}
		return os.Chmod(path, mode)
	})
}

func chownTree(root string, uid, gid int) error {
	return filepath.Walk(root, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		return os.Chown(path, uid, gid)
	})
}

func sudoInvokerIDs() (uid, gid int, ok bool) {
	su := strings.TrimSpace(os.Getenv("SUDO_UID"))
	sg := strings.TrimSpace(os.Getenv("SUDO_GID"))
	if su == "" || sg == "" {
		return 0, 0, false
	}
	u, err := strconv.Atoi(su)
	if err != nil || u < 0 {
		return 0, 0, false
	}
	g, err := strconv.Atoi(sg)
	if err != nil || g < 0 {
		return 0, 0, false
	}
	if syscall.Geteuid() != 0 {
		return 0, 0, false
	}
	return u, g, true
}
