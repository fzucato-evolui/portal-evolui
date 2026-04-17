//go:build !windows

package sysinfo

import "os"

// IsProcessElevated no Linux/Unix: considera elevado apenas root (euid 0), paridade prática com “privilégios completos” para leitura de sistema.
func IsProcessElevated() bool {
	return os.Geteuid() == 0
}
