//go:build !windows

package sysinfo

func servicesSnapshot() []map[string]interface{} {
	return nil
}
