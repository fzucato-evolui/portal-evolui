//go:build windows

package sysinfo

import (
	"encoding/binary"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"strings"
	"time"
	"unicode/utf16"

	"github.com/shirou/gopsutil/v3/disk"
	"github.com/yusufpapurcu/wmi"
)

// --- WMI structs (property names match CIM) ---

type win32ComputerSystem struct {
	Manufacturer string
	Model        string
	Name         string
	SerialNumber string
}

type win32BIOS struct {
	Manufacturer      string
	SerialNumber      string
	SMBIOSBIOSVersion string
	ReleaseDate       string
	Description       string
	Name              string
}

type win32BaseBoard struct {
	Manufacturer string
	Product      string
	SerialNumber string
	Version      string
}

type win32ComputerSystemProduct struct {
	UUID string
}

type win32PhysicalMemory struct {
	BankLabel    string
	Capacity     uint64
	Speed        uint32
	Manufacturer string
	MemoryType   uint16
}

type win32NetworkAdapterConfiguration struct {
	DNSHostName          string
	DefaultIPGateway     []string
	DNSServerSearchOrder []string
	DNSDomain            string
}

type win32ComputerSystemDNS struct {
	Domain      string
	DNSHostName string
}

type win32Service struct {
	Name        string
	DisplayName string
	State       string
	ProcessId   uint32
}

type win32ProcessHandles struct {
	Handles uint32
}

type win32LogicalDiskToPartition struct {
	Antecedent string
	Dependent  string
}

type win32DiskDrive struct {
	Index      uint32
	DeviceID   string
	Model      string
	Size       uint64
	Partitions uint32
}

type win32DiskDriveSerial struct {
	DeviceID     string
	SerialNumber string
}

type win32IP4Route struct {
	Destination string
	Mask        string
	NextHop     string
	Metric1     int32
}

type win32DiskPartition struct {
	DiskIndex        uint32
	Index            uint32
	Size             uint64
	BootPartition    bool
	PrimaryPartition bool
	Name             string
}

type win32LogicalDisk struct {
	DeviceID           string
	Size               uint64
	FreeSpace          uint64
	FileSystem         string
	VolumeName         string
	VolumeSerialNumber uint32
}

type perfTCP struct {
	Name                   string
	ConnectionsEstablished uint64
	ConnectionsActive      uint64
	ConnectionsPassive     uint64
	ConnectionFailures     uint64
	ConnectionsReset       uint64
	SegmentsSent           uint64
	SegmentsReceived       uint64
	SegmentsRetransmitted  uint64
	InErrors               uint64
	OutResets              uint64
}

type perfUDP struct {
	Name                    string
	DatagramsSent           uint64
	DatagramsReceived       uint64
	DatagramsNoPort         uint64
	DatagramsReceivedErrors uint64
}

type win32LoggedOnUser struct {
	Antecedent string
	Dependent  string
}

func wmiComputerSystem() (manufacturer, model, serial, name string, err error) {
	var dst []win32ComputerSystem
	q := "SELECT Manufacturer, Model, Name, SerialNumber FROM Win32_ComputerSystem"
	if e := wmi.Query(q, &dst); e != nil {
		return "", "", "", "", e
	}
	if len(dst) == 0 {
		return "", "", "", "", fmt.Errorf("Win32_ComputerSystem vazio")
	}
	r := dst[0]
	return strings.TrimSpace(r.Manufacturer), strings.TrimSpace(r.Model), strings.TrimSpace(r.SerialNumber), strings.TrimSpace(r.Name), nil
}

func wmiBIOS() (name, description, version, manufacturer, releaseDate string) {
	var dst []win32BIOS
	if err := wmi.Query("SELECT Name, Description, SMBIOSBIOSVersion, Manufacturer, ReleaseDate, SerialNumber FROM Win32_BIOS", &dst); err != nil || len(dst) == 0 {
		return "", "", "", "", ""
	}
	r := dst[0]
	return strings.TrimSpace(r.Name), strings.TrimSpace(r.Description), strings.TrimSpace(r.SMBIOSBIOSVersion), strings.TrimSpace(r.Manufacturer), strings.TrimSpace(r.ReleaseDate)
}

func wmiBaseboard() (version, manufacturer, serialNumber, model string) {
	var dst []win32BaseBoard
	if err := wmi.Query("SELECT Version, Manufacturer, SerialNumber, Product FROM Win32_BaseBoard", &dst); err != nil || len(dst) == 0 {
		return "", "", "", ""
	}
	r := dst[0]
	return strings.TrimSpace(r.Version), strings.TrimSpace(r.Manufacturer), strings.TrimSpace(r.SerialNumber), strings.TrimSpace(r.Product)
}

func wmiSystemProductUUID() string {
	var dst []win32ComputerSystemProduct
	if err := wmi.Query("SELECT UUID FROM Win32_ComputerSystemProduct", &dst); err != nil || len(dst) == 0 {
		return ""
	}
	return strings.TrimSpace(dst[0].UUID)
}

func wmiPhysicalMemoryList() []map[string]interface{} {
	var rows []win32PhysicalMemory
	if err := wmi.Query("SELECT BankLabel, Capacity, Speed, Manufacturer, MemoryType FROM Win32_PhysicalMemory", &rows); err != nil {
		return nil
	}
	out := make([]map[string]interface{}, 0, len(rows))
	for _, r := range rows {
		mt := ""
		if r.MemoryType != 0 {
			mt = fmt.Sprintf("%d", r.MemoryType)
		}
		out = append(out, map[string]interface{}{
			"bankLabel":    strings.TrimSpace(r.BankLabel),
			"capacity":     int64(r.Capacity),
			"clockSpeed":   int64(r.Speed),
			"manufacturer": strings.TrimSpace(r.Manufacturer),
			"memoryType":   mt,
		})
	}
	return out
}

func wmiComputerSystemFQDN() string {
	var rows []win32ComputerSystemDNS
	if err := wmi.Query("SELECT Domain, DNSHostName FROM Win32_ComputerSystem", &rows); err != nil || len(rows) == 0 {
		return ""
	}
	r := rows[0]
	domain := strings.TrimSpace(r.Domain)
	host := strings.TrimSpace(r.DNSHostName)
	if host == "" || domain == "" || strings.EqualFold(domain, "WORKGROUP") {
		return ""
	}
	if strings.Contains(domain, ".") {
		return host + "." + domain
	}
	return domain
}

func wmiNetworkParams(hostname string) (gw4, gw6 string, dns []string, domain string) {
	// Sempre slice (não nil): json.Marshal de []string nil vira "dnsServers": null e quebra o front.
	dns = make([]string, 0)
	seenDNS := map[string]struct{}{}
	var rows []win32NetworkAdapterConfiguration
	err := wmi.Query("SELECT DefaultIPGateway, DNSServerSearchOrder, DNSDomain FROM Win32_NetworkAdapterConfiguration WHERE IPEnabled = TRUE", &rows)
	if err == nil {
		for _, r := range rows {
			if len(r.DefaultIPGateway) > 0 {
				for _, g := range r.DefaultIPGateway {
					g = strings.TrimSpace(g)
					if g == "" {
						continue
					}
					if strings.Contains(g, ":") {
						if gw6 == "" {
							gw6 = g
						}
					} else {
						if gw4 == "" {
							gw4 = g
						}
					}
				}
			}
			if domain == "" && strings.TrimSpace(r.DNSDomain) != "" {
				domain = strings.TrimSpace(r.DNSDomain)
			}
			for _, srv := range r.DNSServerSearchOrder {
				srv = strings.TrimSpace(srv)
				if srv == "" {
					continue
				}
				if _, ok := seenDNS[srv]; ok {
					continue
				}
				seenDNS[srv] = struct{}{}
				dns = append(dns, srv)
			}
		}
	}
	if gw4 == "" || gw6 == "" {
		g4, g6 := defaultGatewaysFromRegistry()
		if gw4 == "" {
			gw4 = g4
		}
		if gw6 == "" {
			gw6 = g6
		}
	}
	if len(dns) == 0 {
		for _, s := range dnsFromRegistryWindows() {
			if s == "" {
				continue
			}
			if _, ok := seenDNS[s]; ok {
				continue
			}
			seenDNS[s] = struct{}{}
			dns = append(dns, s)
		}
	}
	if domain == "" {
		domain = wmiComputerSystemFQDN()
	}
	if domain == "" {
		domain = fqdnFromRegistryTcpip()
	}
	if gw4 == "" || gw6 == "" {
		rg4, rg6 := wmiDefaultGatewaysFromRouteTable()
		if gw4 == "" {
			gw4 = rg4
		}
		if gw6 == "" {
			gw6 = rg6
		}
	}
	_ = hostname
	return gw4, gw6, dns, domain
}

// wmiDefaultGatewaysFromRouteTable usa Win32_IP4RouteTable / Win32_IP6RouteTable (fallback quando adaptador/registo não trazem gateway).
func wmiDefaultGatewaysFromRouteTable() (gw4, gw6 string) {
	var r4 []win32IP4Route
	if err := wmi.Query("SELECT Destination, Mask, NextHop, Metric1 FROM Win32_IP4RouteTable WHERE Destination='0.0.0.0'", &r4); err == nil && len(r4) > 0 {
		bestMetric := int32(2147483647)
		for _, r := range r4 {
			nh := strings.TrimSpace(r.NextHop)
			if nh == "" || nh == "0.0.0.0" {
				continue
			}
			if r.Metric1 >= 0 && r.Metric1 < bestMetric {
				bestMetric = r.Metric1
				gw4 = nh
			}
		}
	}
	type win32IP6Route struct {
		Destination string
		NextHop     string
		Metric1     int32
	}
	var r6 []win32IP6Route
	if err := wmi.Query("SELECT Destination, NextHop, Metric1 FROM Win32_IP6RouteTable WHERE Destination='::'", &r6); err == nil && len(r6) > 0 {
		bestMetric := int32(2147483647)
		for _, r := range r6 {
			nh := strings.TrimSpace(r.NextHop)
			if nh == "" || nh == "::" {
				continue
			}
			if r.Metric1 >= 0 && r.Metric1 < bestMetric {
				bestMetric = r.Metric1
				gw6 = nh
			}
		}
	}
	return gw4, gw6
}

func wmiServicesList() []map[string]interface{} {
	var rows []win32Service
	if err := wmi.Query("SELECT Name, DisplayName, State, ProcessId FROM Win32_Service", &rows); err != nil {
		return nil
	}
	out := make([]map[string]interface{}, 0, len(rows))
	for _, r := range rows {
		name := strings.TrimSpace(r.DisplayName)
		if name == "" {
			name = strings.TrimSpace(r.Name)
		}
		out = append(out, map[string]interface{}{
			"name":      name,
			"processID": int(r.ProcessId),
			"state":     mapWmiServiceState(r.State),
		})
	}
	return out
}

func mapWmiServiceState(s string) string {
	switch strings.ToLower(strings.TrimSpace(s)) {
	case "running":
		return "RUNNING"
	case "stopped":
		return "STOPPED"
	case "start pending":
		return "START_PENDING"
	case "stop pending":
		return "STOP_PENDING"
	case "paused":
		return "STOPPED"
	case "continue pending", "pause pending":
		return "START_PENDING"
	default:
		return "UNKNOWN"
	}
}

func wmiSumProcessHandles() int64 {
	var rows []win32ProcessHandles
	if err := wmi.Query("SELECT Handles FROM Win32_Process", &rows); err != nil {
		return 0
	}
	var sum int64
	for _, r := range rows {
		sum += int64(r.Handles)
	}
	return sum
}

func wmiPerfTCP(table string) map[string]interface{} {
	var rows []perfTCP
	q := fmt.Sprintf("SELECT Name, ConnectionsEstablished, ConnectionsActive, ConnectionsPassive, ConnectionFailures, ConnectionsReset, SegmentsSent, SegmentsReceived, SegmentsRetransmitted, InErrors, OutResets FROM %s", table)
	if err := wmi.Query(q, &rows); err != nil || len(rows) == 0 {
		return emptyTcpStats()
	}
	var r perfTCP
	for _, row := range rows {
		if strings.EqualFold(strings.TrimSpace(row.Name), "_Total") {
			r = row
			break
		}
	}
	if r.Name == "" {
		r = rows[0]
	}
	return map[string]interface{}{
		"connectionsEstablished": int64(r.ConnectionsEstablished),
		"connectionsActive":      int64(r.ConnectionsActive),
		"connectionsPassive":     int64(r.ConnectionsPassive),
		"connectionFailures":     int64(r.ConnectionFailures),
		"connectionsReset":       int64(r.ConnectionsReset),
		"segmentsSent":           int64(r.SegmentsSent),
		"segmentsReceived":       int64(r.SegmentsReceived),
		"segmentsRetransmitted":  int64(r.SegmentsRetransmitted),
		"inErrors":               int64(r.InErrors),
		"outResets":              int64(r.OutResets),
	}
}

func wmiPerfUDP(table string) map[string]interface{} {
	var rows []perfUDP
	q := fmt.Sprintf("SELECT Name, DatagramsSent, DatagramsReceived, DatagramsNoPort, DatagramsReceivedErrors FROM %s", table)
	if err := wmi.Query(q, &rows); err != nil || len(rows) == 0 {
		return emptyUdpStats()
	}
	var r perfUDP
	for _, row := range rows {
		if strings.EqualFold(strings.TrimSpace(row.Name), "_Total") {
			r = row
			break
		}
	}
	if r.Name == "" {
		r = rows[0]
	}
	return map[string]interface{}{
		"datagramsSent":           int64(r.DatagramsSent),
		"datagramsReceived":       int64(r.DatagramsReceived),
		"datagramsNoPort":         int64(r.DatagramsNoPort),
		"datagramsReceivedErrors": int64(r.DatagramsReceivedErrors),
	}
}

func emptyTcpStats() map[string]interface{} {
	return map[string]interface{}{
		"connectionsEstablished": int64(0), "connectionsActive": int64(0), "connectionsPassive": int64(0),
		"connectionFailures": int64(0), "connectionsReset": int64(0), "segmentsSent": int64(0), "segmentsReceived": int64(0),
		"segmentsRetransmitted": int64(0), "inErrors": int64(0), "outResets": int64(0),
	}
}

func emptyUdpStats() map[string]interface{} {
	return map[string]interface{}{
		"datagramsSent": int64(0), "datagramsReceived": int64(0), "datagramsNoPort": int64(0), "datagramsReceivedErrors": int64(0),
	}
}

func wmiInternetProtocolStatsAggregate() map[string]interface{} {
	return map[string]interface{}{
		"tcpv4Stats": wmiPerfTCP("Win32_PerfRawData_Tcpip_TCPv4"),
		"tcpv6Stats": wmiPerfTCP("Win32_PerfRawData_Tcpip_TCPv6"),
		"udpv4Stats": wmiPerfUDP("Win32_PerfRawData_Tcpip_UDPv4"),
		"udpv6Stats": wmiPerfUDP("Win32_PerfRawData_Tcpip_UDPv6"),
	}
}

// parseDependentDrive extrai "C:" de Dependent WMI.
func parseDependentDrive(dependent string) string {
	dependent = strings.TrimSpace(dependent)
	if i := strings.Index(dependent, `"`); i >= 0 {
		j := strings.LastIndex(dependent, `"`)
		if j > i {
			s := dependent[i+1 : j]
			if strings.HasSuffix(s, ":") && len(s) <= 3 {
				return strings.ToUpper(s)
			}
		}
	}
	return ""
}

func wmiPartitionToDrive() map[string]string {
	var rows []win32LogicalDiskToPartition
	if err := wmi.Query("SELECT Antecedent, Dependent FROM Win32_LogicalDiskToPartition", &rows); err != nil {
		return nil
	}
	m := make(map[string]string)
	for _, r := range rows {
		drv := parseDependentDrive(r.Dependent)
		if drv == "" {
			continue
		}
		key := ""
		if idx := strings.Index(r.Antecedent, `"`); idx >= 0 {
			rest := r.Antecedent[idx+1:]
			if j := strings.Index(rest, `"`); j > 0 {
				key = rest[:j]
			}
		}
		if key != "" {
			m[key] = drv
		}
	}
	return m
}

func wmiDiskDriveSerialByDeviceID() map[string]string {
	var rows []win32DiskDriveSerial
	if err := wmi.Query("SELECT DeviceID, SerialNumber FROM Win32_DiskDrive", &rows); err != nil {
		return nil
	}
	m := make(map[string]string)
	for _, r := range rows {
		id := strings.TrimSpace(r.DeviceID)
		if id == "" {
			continue
		}
		m[id] = strings.TrimSpace(r.SerialNumber)
	}
	return m
}

func wmiDiskStoresPhysical(ioByLetter map[string]disk.IOCountersStat) ([]map[string]interface{}, error) {
	var drives []win32DiskDrive
	if err := wmi.Query("SELECT Index, DeviceID, Model, Size, Partitions FROM Win32_DiskDrive", &drives); err != nil {
		return nil, err
	}
	serialByID := wmiDiskDriveSerialByDeviceID()
	part2drive := wmiPartitionToDrive()
	var parts []win32DiskPartition
	_ = wmi.Query("SELECT DiskIndex, Index, Size, BootPartition, PrimaryPartition, Name FROM Win32_DiskPartition", &parts)

	var logical []win32LogicalDisk
	_ = wmi.Query("SELECT DeviceID, Size, FreeSpace, FileSystem, VolumeName, VolumeSerialNumber FROM Win32_LogicalDisk WHERE DriveType = 3", &logical)
	logByLetter := map[string]win32LogicalDisk{}
	for _, ld := range logical {
		logByLetter[strings.TrimSpace(ld.DeviceID)] = ld
	}

	out := make([]map[string]interface{}, 0, len(drives))
	now := time.Now().UnixMilli()
	for _, dr := range drives {
		diskIndex := dr.Index
		serial := ""
		if serialByID != nil {
			serial = serialByID[strings.TrimSpace(dr.DeviceID)]
		}
		var plist []map[string]interface{}
		for _, p := range parts {
			if p.DiskIndex != diskIndex {
				continue
			}
			pKey := strings.TrimSpace(p.Name)
			if pKey == "" {
				pKey = fmt.Sprintf("Disk #%d, Partition #%d", p.DiskIndex, p.Index)
			}
			driveLetter := ""
			if part2drive != nil {
				if dl, ok := part2drive[pKey]; ok {
					driveLetter = dl
				} else {
					for k, v := range part2drive {
						if strings.Contains(strings.ToLower(k), strings.ToLower(fmt.Sprintf("Disk #%d", p.DiskIndex))) &&
							strings.Contains(k, fmt.Sprintf("Partition #%d", p.Index)) {
							driveLetter = v
							break
						}
					}
				}
			}
			mount := ""
			uuid := ""
			desc := "Sistema de Arquivos Instalável"
			fsType := ""
			if driveLetter != "" {
				mount = driveLetter
				if len(mount) == 2 {
					mount += `\`
				}
				if ld, ok := logByLetter[driveLetter]; ok {
					fsType = ld.FileSystem
					if ld.VolumeSerialNumber != 0 {
						uuid = fmt.Sprintf("%08X-0000-0000-0000-100000000000", ld.VolumeSerialNumber)
					}
				}
			}
			if fsType != "" {
				desc = fsType
			}
			plist = append(plist, map[string]interface{}{
				"identification": pKey,
				"name":           "Installable File System",
				"type":           desc,
				"uuid":           uuid,
				"size":           int64(p.Size),
				"major":          0,
				"minor":          0,
				"mountPoint":     mount,
			})
		}
		var reads, readBytes, writes, writeBytes uint64
		var xfer uint64
		for _, pl := range plist {
			mp := pl["mountPoint"].(string)
			if mp == "" {
				continue
			}
			letter := strings.TrimSuffix(strings.TrimSpace(mp), `\`)
			if letter != "" && !strings.HasSuffix(letter, ":") {
				letter += ":"
			}
			if ioc, ok := ioByLetter[letter]; ok {
				reads += ioc.ReadCount
				readBytes += ioc.ReadBytes
				writes += ioc.WriteCount
				writeBytes += ioc.WriteBytes
				xfer += ioc.ReadTime + ioc.WriteTime
			}
		}
		out = append(out, map[string]interface{}{
			"name":               strings.TrimSpace(dr.DeviceID),
			"model":              strings.TrimSpace(dr.Model),
			"serial":             serial,
			"size":               int64(dr.Size),
			"reads":              int64(reads),
			"readBytes":          int64(readBytes),
			"writes":             int64(writes),
			"writeBytes":         int64(writeBytes),
			"currentQueueLength": int64(0),
			"transferTime":       int64(xfer),
			"partitions":         plist,
			"timeStamp":          now,
		})
	}
	return out, nil
}

var (
	wmiLoggedOnUserNameRE   = regexp.MustCompile(`Name="([^"]+)"`)
	wmiLoggedOnUserDomainRE = regexp.MustCompile(`Domain="([^"]+)"`)
)

func decodeWindowsTextBytes(b []byte) string {
	if len(b) >= 2 && b[0] == 0xff && b[1] == 0xfe {
		u := make([]uint16, (len(b)-2)/2)
		for i := range u {
			u[i] = binary.LittleEndian.Uint16(b[2+i*2:])
		}
		return string(utf16.Decode(u))
	}
	if len(b) >= 2 && b[0] == 0xfe && b[1] == 0xff {
		u := make([]uint16, (len(b)-2)/2)
		for i := range u {
			u[i] = binary.BigEndian.Uint16(b[2+i*2:])
		}
		return string(utf16.Decode(u))
	}
	if len(b) >= 3 && b[0] == 0xef && b[1] == 0xbb && b[2] == 0xbf {
		b = b[3:]
	}
	return string(b)
}

func sessionsFromQueryUser() []map[string]interface{} {
	windir := os.Getenv("SystemRoot")
	if windir == "" {
		windir = os.Getenv("windir")
	}
	if windir == "" {
		windir = `C:\Windows`
	}
	queryExe := filepath.Join(windir, "System32", "query.exe")
	raw, err := exec.Command(queryExe, "user").Output()
	if err != nil {
		return nil
	}
	text := decodeWindowsTextBytes(raw)
	var sessions []map[string]interface{}
	seen := map[string]struct{}{}
	for _, line := range strings.Split(text, "\n") {
		line = strings.TrimSpace(line)
		if line == "" {
			continue
		}
		u := strings.ToUpper(line)
		if strings.Contains(u, "USERNAME") || strings.Contains(u, "NOME DE USU") || strings.Contains(line, "---") {
			continue
		}
		parts := strings.Fields(line)
		if len(parts) < 3 {
			continue
		}
		user := strings.TrimPrefix(parts[0], ">")
		if user == "" {
			continue
		}
		if _, ok := seen[user]; ok {
			continue
		}
		seen[user] = struct{}{}
		sessions = append(sessions, map[string]interface{}{
			"userName":       user,
			"terminalDevice": "Console",
			"loginTime":      int64(0),
			"host":           "LOCAL",
		})
	}
	if len(sessions) == 0 {
		return nil
	}
	return sessions
}

func sessionDedupKey(host, userName string) string {
	return strings.ToLower(strings.TrimSpace(host) + "|" + strings.TrimSpace(userName))
}

func appendSessionDedup(out *[]map[string]interface{}, seen map[string]struct{}, host, userName, term string) {
	userName = strings.TrimSpace(userName)
	if userName == "" {
		return
	}
	k := sessionDedupKey(host, userName)
	if _, ok := seen[k]; ok {
		return
	}
	seen[k] = struct{}{}
	if strings.TrimSpace(host) == "" {
		host = "LOCAL"
	}
	if term == "" {
		term = "Console"
	}
	*out = append(*out, map[string]interface{}{
		"userName":       userName,
		"terminalDevice": term,
		"loginTime":      int64(0),
		"host":           host,
	})
}

func wmiSessionsList() []map[string]interface{} {
	seen := map[string]struct{}{}
	var out []map[string]interface{}

	if wts := sessionsFromWTS(); len(wts) > 0 {
		for _, s := range wts {
			host, _ := s["host"].(string)
			un, _ := s["userName"].(string)
			term, _ := s["terminalDevice"].(string)
			appendSessionDedup(&out, seen, host, un, term)
		}
	}

	var users []win32LoggedOnUser
	if err := wmi.Query("SELECT Antecedent, Dependent FROM Win32_LoggedOnUser", &users); err == nil {
		for _, u := range users {
			dep := u.Dependent
			mName := wmiLoggedOnUserNameRE.FindStringSubmatch(dep)
			if len(mName) < 2 {
				continue
			}
			name := strings.TrimSpace(mName[1])
			if name == "" {
				continue
			}
			domain := ""
			if mDom := wmiLoggedOnUserDomainRE.FindStringSubmatch(dep); len(mDom) > 1 {
				domain = strings.TrimSpace(mDom[1])
			}
			host := domain
			if host == "" {
				host = "LOCAL"
			}
			appendSessionDedup(&out, seen, host, name, "Console")
		}
	}

	if len(out) == 0 {
		if q := sessionsFromQueryUser(); len(q) > 0 {
			return q
		}
	}
	return out
}
