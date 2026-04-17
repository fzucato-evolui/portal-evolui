//go:build windows

package sysinfo

import (
	"sync"
	"time"

	"github.com/shirou/gopsutil/v3/winservices"
	"golang.org/x/sys/windows/svc"
)

const (
	servicesCacheTTL  = 90 * time.Second
	maxServicesInJSON = 40
)

var (
	servicesCacheMu  sync.Mutex
	servicesCached   []map[string]interface{}
	servicesCachedAt time.Time
)

func servicesSnapshot() []map[string]interface{} {
	servicesCacheMu.Lock()
	defer servicesCacheMu.Unlock()
	if time.Since(servicesCachedAt) < servicesCacheTTL && len(servicesCached) > 0 {
		return servicesCached
	}
	list, err := winservices.ListServices()
	if err != nil {
		servicesCached = nil
		servicesCachedAt = time.Now()
		return nil
	}
	n := len(list)
	if n > maxServicesInJSON {
		n = maxServicesInJSON
	}
	out := make([]map[string]interface{}, 0, n)
	for i := 0; i < n; i++ {
		s, err := winservices.NewService(list[i].Name)
		if err != nil {
			continue
		}
		st, err := s.QueryStatus()
		if err != nil {
			continue
		}
		out = append(out, map[string]interface{}{
			"name":      list[i].Name,
			"processID": int(st.Pid),
			"state":     mapSvcStateToOshi(st.State),
		})
	}
	servicesCached = out
	servicesCachedAt = time.Now()
	return out
}

func mapSvcStateToOshi(st svc.State) string {
	switch st {
	case svc.Running:
		return "RUNNING"
	case svc.Stopped:
		return "STOPPED"
	case svc.StartPending:
		return "START_PENDING"
	case svc.StopPending:
		return "STOP_PENDING"
	case svc.Paused:
		return "STOPPED"
	case svc.PausePending, svc.ContinuePending:
		return "START_PENDING"
	default:
		return "UNKNOWN"
	}
}
