//go:build windows

package sysinfo

import "golang.org/x/sys/windows"

func currentThreadID() int {
	return int(windows.GetCurrentThreadId())
}
