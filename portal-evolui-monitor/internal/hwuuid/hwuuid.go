// Package hwuuid reproduz o critério do cliente Java (OSHI):
// siDTO.getHardware().getComputerSystem().getHardwareUUID() — ver MainController no healthchecker.
package hwuuid

import (
	"fmt"
	"os"
	"os/exec"
	"runtime"
	"strings"
)

// FromOS lê o UUID de hardware do SO (Linux: DMI sysfs; Windows: Win32_ComputerSystemProduct).
func FromOS() (string, error) {
	switch runtime.GOOS {
	case "linux":
		return fromLinux()
	case "windows":
		return fromWindows()
	default:
		return "", fmt.Errorf("GOOS=%s: use -identifier ou EVOLUI_MONITOR_IDENTIFIER", runtime.GOOS)
	}
}

func fromLinux() (string, error) {
	// OSHI / sysfs — mesmo valor que MainController obtém via SystemInfo.
	b, err := os.ReadFile("/sys/class/dmi/id/product_uuid")
	if err != nil {
		return "", fmt.Errorf("ler /sys/class/dmi/id/product_uuid: %w", err)
	}
	s := strings.TrimSpace(string(b))
	if s == "" || strings.Contains(strings.ToLower(s), "not specified") {
		return "", fmt.Errorf("product_uuid vazio ou inválido")
	}
	return s, nil
}

func fromWindows() (string, error) {
	// Preferência: CIM (equivalente WMI). Fallback: wmic.
	ps := exec.Command(
		"powershell", "-NoProfile", "-NonInteractive",
		"-Command", "(Get-CimInstance -Class Win32_ComputerSystemProduct).UUID",
	)
	out, err := ps.Output()
	s := strings.TrimSpace(string(out))
	if err == nil && looksLikeUUID(s) {
		return s, nil
	}

	wmic := exec.Command("cmd", "/c", "wmic path win32_computersystemproduct get uuid")
	out2, err2 := wmic.Output()
	if err2 != nil {
		if err != nil {
			return "", fmt.Errorf("powershell: %v; wmic: %w", err, err2)
		}
		return "", fmt.Errorf("wmic: %w", err2)
	}
	lines := strings.FieldsFunc(strings.TrimSpace(string(out2)), func(r rune) bool {
		return r == '\n' || r == '\r'
	})
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if line == "" || strings.EqualFold(line, "UUID") {
			continue
		}
		if looksLikeUUID(line) {
			return line, nil
		}
	}
	return "", fmt.Errorf("não foi possível obter UUID de hardware no Windows")
}

func looksLikeUUID(s string) bool {
	s = strings.TrimSpace(s)
	if len(s) < 32 {
		return false
	}
	return true
}
