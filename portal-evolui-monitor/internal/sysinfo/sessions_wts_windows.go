//go:build windows

package sysinfo

import (
	"strings"
	"unsafe"

	"golang.org/x/sys/windows"
)

// WTS_INFO_CLASS (trecho): https://learn.microsoft.com/en-us/windows/win32/api/wtsapi32/ne-wtsapi32-wts_info_class
const (
	wtsUserNameClass   uint32 = 5
	wtsWinStationClass uint32 = 6
	wtsDomainNameClass uint32 = 7
)

var procWTSQuerySessionInformationW = windows.NewLazySystemDLL("wtsapi32.dll").NewProc("WTSQuerySessionInformationW")

func wtsQuerySessionInfoString(sessionID uint32, infoClass uint32) string {
	var buf *uint16
	var n uint32
	r1, _, _ := procWTSQuerySessionInformationW.Call(
		uintptr(0),
		uintptr(sessionID),
		uintptr(infoClass),
		uintptr(unsafe.Pointer(&buf)),
		uintptr(unsafe.Pointer(&n)),
	)
	if r1 == 0 || buf == nil || n < 2 {
		return ""
	}
	defer windows.WTSFreeMemory(uintptr(unsafe.Pointer(buf)))
	return windows.UTF16PtrToString(buf)
}

// sessionsFromWTS lista sessões interactivas (Terminal Services), alinhado ao que o OSHI costuma expor melhor que Win32_LoggedOnUser sozinho.
func sessionsFromWTS() []map[string]interface{} {
	var sessionsPtr *windows.WTS_SESSION_INFO
	var count uint32
	err := windows.WTSEnumerateSessions(windows.Handle(0), 0, 1, &sessionsPtr, &count)
	if err != nil || count == 0 || sessionsPtr == nil {
		return nil
	}
	defer windows.WTSFreeMemory(uintptr(unsafe.Pointer(sessionsPtr)))

	slice := unsafe.Slice(sessionsPtr, count)
	var out []map[string]interface{}
	seen := map[string]struct{}{}
	for i := range slice {
		si := &slice[i]
		if si.State != windows.WTSActive && si.State != windows.WTSConnected {
			continue
		}
		name := strings.TrimSpace(wtsQuerySessionInfoString(si.SessionID, wtsUserNameClass))
		if name == "" {
			continue
		}
		domain := strings.TrimSpace(wtsQuerySessionInfoString(si.SessionID, wtsDomainNameClass))
		host := domain
		if host == "" {
			host = "LOCAL"
		}
		term := "Console"
		if si.WindowStationName != nil {
			if ws := windows.UTF16PtrToString(si.WindowStationName); ws != "" {
				term = ws
			}
		} else if ws := strings.TrimSpace(wtsQuerySessionInfoString(si.SessionID, wtsWinStationClass)); ws != "" {
			term = ws
		}
		key := strings.ToLower(host + "|" + name)
		if _, ok := seen[key]; ok {
			continue
		}
		seen[key] = struct{}{}
		out = append(out, map[string]interface{}{
			"userName":       name,
			"terminalDevice": term,
			"loginTime":      int64(0),
			"host":           host,
		})
	}
	if len(out) == 0 {
		return nil
	}
	return out
}
