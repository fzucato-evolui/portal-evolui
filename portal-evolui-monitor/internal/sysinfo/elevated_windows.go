//go:build windows

package sysinfo

import "golang.org/x/sys/windows"

// IsProcessElevated indica se o processo corre com token elevado (UAC), alinhado a oshi.software.os.windows.WindowsOperatingSystem.isElevated / Advapi32.
func IsProcessElevated() bool {
	var token windows.Token
	err := windows.OpenProcessToken(windows.CurrentProcess(), windows.TOKEN_QUERY, &token)
	if err != nil {
		return false
	}
	defer token.Close()
	return token.IsElevated()
}
