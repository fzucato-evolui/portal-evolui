//go:build !windows

package sysinfo

func currentThreadID() int { return 0 }
