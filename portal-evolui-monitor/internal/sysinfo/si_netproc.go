package sysinfo

import (
	"net"
	"os"
	"runtime"
	"sort"
	"strings"
	"syscall"

	gopsutilnet "github.com/shirou/gopsutil/v3/net"
	"github.com/shirou/gopsutil/v3/process"
)

// SOCK_STREAM / SOCK_DGRAM / AF_INET — valores Win32 (winsock2.h); em Linux batem com syscall.
const (
	winSockStream = 1
	winSockDgram  = 2
	winAFInet     = 2
	winAFInet6    = 23
)

const maxConnectionsPayload = 5000

func buildNetworkIFs() ([]map[string]interface{}, error) {
	ifs, err := gopsutilnet.Interfaces()
	if err != nil {
		return nil, err
	}
	var out []map[string]interface{}
	for _, iface := range ifs {
		if iface.Name == "lo" || strings.HasPrefix(iface.Name, "lo") {
			continue
		}
		var ipv4, ipv6 []string
		var masks []int
		var prefixes []int
		for _, a := range iface.Addrs {
			ip := strings.TrimSpace(a.Addr)
			if ip == "" {
				continue
			}
			if strings.Contains(ip, "/") {
				hip, ipnet, err := net.ParseCIDR(ip)
				if err == nil && hip != nil {
					if hip.To4() != nil {
						ipv4 = append(ipv4, hip.String())
						if ones, _ := ipnet.Mask.Size(); ones > 0 {
							masks = append(masks, ones)
						}
					} else {
						ipv6 = append(ipv6, hip.String())
						if ones, _ := ipnet.Mask.Size(); ones > 0 {
							prefixes = append(prefixes, ones)
						}
					}
				}
			} else if ip := net.ParseIP(ip); ip != nil {
				if ip.To4() != nil {
					ipv4 = append(ipv4, ip.String())
				} else {
					ipv6 = append(ipv6, ip.String())
				}
			}
		}
		if len(ipv4) == 0 && len(ipv6) == 0 && iface.HardwareAddr == "" {
			continue
		}
		oper := "UNKNOWN"
		for _, f := range iface.Flags {
			if strings.EqualFold(f, "up") {
				oper = "UP"
				break
			}
		}
		out = append(out, map[string]interface{}{
			"name":                   iface.Name,
			"index":                  iface.Index,
			"displayName":            iface.Name,
			"ifAlias":                "",
			"ifOperStatus":           oper,
			"mtu":                    int64(iface.MTU),
			"macaddr":                iface.HardwareAddr,
			"ipv4addr":               ipv4,
			"subnetMasks":            masks,
			"ipv6addr":               ipv6,
			"prefixLengths":          prefixes,
			"ifType":                 0,
			"ndisPhysicalMediumType": 0,
			"connectorPresent":       false,
			"speed":                  int64(0),
			"timeStamp":              0,
			"knownVmMacAddr":         false,
		})
	}
	return out, nil
}

func connectionKind(c gopsutilnet.ConnectionStat) string {
	isTCP := c.Type == syscall.SOCK_STREAM || int(c.Type) == winSockStream
	isUDP := c.Type == syscall.SOCK_DGRAM || int(c.Type) == winSockDgram
	if runtime.GOOS == "windows" {
		isTCP = int(c.Type) == winSockStream
		isUDP = int(c.Type) == winSockDgram
	}
	is4 := c.Family == syscall.AF_INET || int(c.Family) == winAFInet
	is6 := c.Family == syscall.AF_INET6 || int(c.Family) == winAFInet6
	if isTCP && is4 {
		return "tcp4"
	}
	if isTCP && is6 {
		return "tcp6"
	}
	if isUDP && is4 {
		return "udp4"
	}
	if isUDP && is6 {
		return "udp6"
	}
	if isTCP {
		return "tcp4"
	}
	if isUDP {
		return "udp4"
	}
	return "tcp4"
}

func buildConnectionsInet() ([]map[string]interface{}, error) {
	conns, err := gopsutilnet.Connections("inet")
	if err != nil {
		return nil, err
	}
	n := len(conns)
	if n > maxConnectionsPayload {
		n = maxConnectionsPayload
	}
	out := make([]map[string]interface{}, 0, n)
	for i := 0; i < n; i++ {
		c := conns[i]
		kind := connectionKind(c)
		st := normalizeTcpStateForOshi(c.Status)
		if strings.HasPrefix(kind, "udp") {
			st = "NONE"
		}
		out = append(out, map[string]interface{}{
			"type":            kind,
			"localAddress":    encodeIPForJacksonByteArray(c.Laddr.IP),
			"localPort":       int(c.Laddr.Port),
			"foreignAddress":  encodeIPForJacksonByteArray(c.Raddr.IP),
			"foreignPort":     int(c.Raddr.Port),
			"state":           st,
			"transmitQueue":   0,
			"receiveQueue":    0,
			"owningProcessId": int(c.Pid),
		})
	}
	return out, nil
}

type procCPU struct {
	p   *process.Process
	cpu float64
}

func buildProcessesList(memTotal uint64, limit int) ([]map[string]interface{}, error) {
	list, err := process.Processes()
	if err != nil {
		return nil, err
	}
	var scored []procCPU
	for _, p := range list {
		cpu, err := p.CPUPercent()
		if err != nil {
			cpu = 0
		}
		scored = append(scored, procCPU{p: p, cpu: cpu})
	}
	sort.Slice(scored, func(i, j int) bool { return scored[i].cpu > scored[j].cpu })
	if limit <= 0 {
		limit = len(scored)
	}
	if len(scored) > limit {
		scored = scored[:limit]
	}
	out := make([]map[string]interface{}, 0, len(scored))
	for _, sc := range scored {
		p := sc.p
		pid := int32(p.Pid)
		name, _ := p.Name()
		cmd, _ := p.Cmdline()
		cwd, _ := p.Cwd()
		ppid, _ := p.Ppid()
		nt, _ := p.NumThreads()
		pr, _ := p.Nice()
		memRss, _ := p.MemoryInfo()
		rss := uint64(0)
		if memRss != nil {
			rss = memRss.RSS
		}
		vsz := uint64(0)
		if memRss != nil {
			vsz = memRss.VMS
		}
		times, _ := p.Times()
		var ut, kt int64
		if times != nil {
			ut = int64(times.User * 1e6)
			kt = int64(times.System * 1e6)
		}
		ct, _ := p.CreateTime()
		user := ""
		if u, err := p.Username(); err == nil {
			user = u
		}
		st := "RUNNING"
		if s, err := p.Status(); err == nil && len(s) > 0 {
			st = mapProcState(s[0])
		}
		memPct := 0.0
		if memTotal > 0 {
			memPct = 100.0 * float64(rss) / float64(memTotal)
		}
		cpuNum := float64(runtime.NumCPU())
		if cpuNum < 1 {
			cpuNum = 1
		}
		cpuU := sc.cpu / cpuNum
		if runtime.GOOS != "windows" {
			cpuU = sc.cpu
		}
		out = append(out, map[string]interface{}{
			"processID":                int(pid),
			"currentWorkingDirectory":  cwd,
			"commandLine":              cmd,
			"name":                     name,
			"path":                     "",
			"state":                    st,
			"parentProcessID":          int(ppid),
			"threadCount":              int(nt),
			"priority":                 int(pr),
			"virtualSize":              int64(vsz),
			"residentSetSize":          int64(rss),
			"kernelTime":               kt,
			"userTime":                 ut,
			"startTime":                ct,
			"upTime":                   0,
			"group":                    "",
			"userID":                   user,
			"user":                     user,
			"groupID":                  "",
			"processCpuLoadCumulative": 0.0,
			"cpuUsagePercent":          cpuU,
			"memoryUsagePercent":       memPct,
		})
	}
	return out, nil
}

func mapProcState(s string) string {
	switch strings.ToLower(strings.TrimSpace(s)) {
	case "running", "r":
		return "RUNNING"
	case "sleep", "s":
		return "SLEEPING"
	case "sleeping":
		return "SLEEPING"
	case "stop", "t":
		return "STOPPED"
	case "zombie", "z":
		return "ZOMBIE"
	}
	return strings.ToUpper(s)
}

func buildCurrentProcess(memTotal uint64) (map[string]interface{}, error) {
	pid := int32(os.Getpid())
	p, err := process.NewProcess(pid)
	if err != nil {
		return map[string]interface{}{
			"processID": int(pid),
			"name":      "portal-evolui-monitor",
		}, nil
	}
	name, _ := p.Name()
	cmd, _ := p.Cmdline()
	cwd, _ := p.Cwd()
	ppid, _ := p.Ppid()
	nt, _ := p.NumThreads()
	pr, _ := p.Nice()
	memRss, _ := p.MemoryInfo()
	rss := uint64(0)
	vsz := uint64(0)
	if memRss != nil {
		rss = memRss.RSS
		vsz = memRss.VMS
	}
	times, _ := p.Times()
	var ut, kt int64
	if times != nil {
		ut = int64(times.User * 1e6)
		kt = int64(times.System * 1e6)
	}
	ct, _ := p.CreateTime()
	user := ""
	if u, err := p.Username(); err == nil {
		user = u
	}
	memPct := 0.0
	if memTotal > 0 {
		memPct = 100.0 * float64(rss) / float64(memTotal)
	}
	return map[string]interface{}{
		"processID":                int(pid),
		"currentWorkingDirectory":  cwd,
		"commandLine":              cmd,
		"name":                     name,
		"path":                     "",
		"state":                    "RUNNING",
		"parentProcessID":          int(ppid),
		"threadCount":              int(nt),
		"priority":                 int(pr),
		"virtualSize":              int64(vsz),
		"residentSetSize":          int64(rss),
		"kernelTime":               kt,
		"userTime":                 ut,
		"startTime":                ct,
		"upTime":                   0,
		"group":                    "",
		"userID":                   user,
		"user":                     user,
		"groupID":                  "",
		"processCpuLoadCumulative": 0.0,
		"cpuUsagePercent":          0.0,
		"memoryUsagePercent":       memPct,
	}, nil
}

func osFamilyString() string {
	switch runtime.GOOS {
	case "windows":
		return "Windows"
	case "linux":
		return "Linux"
	case "darwin":
		return "macOS"
	default:
		return runtime.GOOS
	}
}
