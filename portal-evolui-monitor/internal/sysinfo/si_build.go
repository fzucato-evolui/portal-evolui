package sysinfo

import (
	"context"
	"os"
	"runtime"
	"strconv"
	"strings"
	"time"

	"github.com/shirou/gopsutil/v3/cpu"
	"github.com/shirou/gopsutil/v3/disk"
	"github.com/shirou/gopsutil/v3/host"
	"github.com/shirou/gopsutil/v3/mem"
	gopsutilnet "github.com/shirou/gopsutil/v3/net"
)

func buildHealthCheckerSystemInfoMap(ctx context.Context, hardwareUUID string) (map[string]interface{}, error) {
	_ = ctx
	h, err := host.Info()
	if err != nil {
		return nil, err
	}
	v, err := mem.VirtualMemory()
	if err != nil {
		return nil, err
	}
	swap, _ := mem.SwapMemory()
	cpus, err := cpu.Info()
	if err != nil {
		return nil, err
	}
	logicalN, _ := cpu.Counts(true)
	physN, _ := cpu.Counts(false)
	if logicalN <= 0 {
		logicalN = runtime.NumCPU()
	}
	if physN <= 0 {
		physN = logicalN
	}

	cpuName := runtime.GOARCH
	cpuFamily := ""
	cpuModel := ""
	cpuStepping := ""
	cpuVendor := ""
	cpuMHz := int64(0)
	if len(cpus) > 0 {
		cpuName = cpus[0].ModelName
		cpuFamily = cpus[0].Family
		cpuModel = cpus[0].Model
		cpuStepping = strconv.Itoa(int(cpus[0].Stepping))
		cpuVendor = cpus[0].VendorID
		cpuMHz = int64(cpus[0].Mhz)
	}
	cpuLoad := sampleCPULoadPercent()

	wmiManu, wmiModel, wmiSerial, wmiName, _ := wmiComputerSystem()
	csManufacturer := wmiManu
	csModel := wmiModel
	csSerial := wmiSerial
	if csManufacturer == "" {
		csManufacturer = h.PlatformFamily
		if csManufacturer == "" {
			hn, _ := os.Hostname()
			csManufacturer = hn
		}
	}
	if csModel == "" {
		csModel = h.Platform
	}
	if wmiName != "" {
		_ = wmiName
	}

	fwName, fwDesc, fwVer, fwManu, fwRel := wmiBIOS()
	bbVer, bbManu, bbSerial, bbModel := wmiBaseboard()
	firmware := map[string]interface{}{
		"name": fwName, "description": fwDesc, "version": fwVer, "manufacturer": fwManu, "releaseDate": fwRel,
	}
	baseboard := map[string]interface{}{
		"version": bbVer, "manufacturer": bbManu, "serialNumber": bbSerial, "model": bbModel,
	}

	computerSystem := map[string]interface{}{
		"manufacturer": csManufacturer,
		"hardwareUUID": hardwareUUID,
		"serialNumber": csSerial,
		"model":        csModel,
		"firmware":     firmware,
		"baseboard":    baseboard,
	}

	cpu64 := runtime.GOARCH == "amd64" || runtime.GOARCH == "arm64"
	procID := map[string]interface{}{
		"vendor":            cpuVendor,
		"name":              cpuName,
		"family":            cpuFamily,
		"model":             cpuModel,
		"stepping":          cpuStepping,
		"processorID":       "",
		"identifier":        cpuName,
		"cpu64bit":          cpu64,
		"vendorFreq":        cpuMHz * 1e6,
		"microarchitecture": "",
	}

	processor := map[string]interface{}{
		"processorIdentifier":    procID,
		"logicalProcessorCount":  logicalN,
		"physicalProcessorCount": physN,
		"physicalPackageCount":   physN,
		"cpuLoad":                cpuLoad,
	}

	pageSize := int64(4096)
	if runtime.GOOS == "linux" {
		pageSize = int64(os.Getpagesize())
	}

	vmDTO := map[string]interface{}{
		"swapTotal":    int64(swap.Total),
		"swapUsed":     int64(swap.Used),
		"virtualMax":   int64(v.Total),
		"virtualInUse": int64(v.Total - v.Available),
		"swapPagesIn":  int64(0),
		"swapPagesOut": int64(0),
	}
	phyMem := wmiPhysicalMemoryList()
	if phyMem == nil {
		phyMem = []map[string]interface{}{}
	}

	memoryHW := map[string]interface{}{
		"total":          v.Total,
		"available":      v.Available,
		"pageSize":       pageSize,
		"virtualMemory":  vmDTO,
		"physicalMemory": phyMem,
	}

	ioMap, _ := disk.IOCounters()
	var diskStores []map[string]interface{}
	if runtime.GOOS == "windows" {
		ds, err := wmiDiskStoresPhysical(ioMap)
		if err != nil || len(ds) == 0 {
			diskStores = fallbackDiskStoresFromPartitions()
		} else {
			diskStores = ds
		}
	} else {
		diskStores = fallbackDiskStoresFromPartitions()
	}

	nifs, _ := buildNetworkIFs()
	if nifs == nil {
		nifs = []map[string]interface{}{}
	}

	hardware := map[string]interface{}{
		"computerSystem":      computerSystem,
		"processor":           processor,
		"memory":              memoryHW,
		"diskStores":          diskStores,
		"logicalVolumeGroups": []interface{}{},
		"networkIFs":          nifs,
	}

	procCount := int(h.Procs)
	threadCount := countTotalThreads()

	gw4, gw6, dns, domain := wmiNetworkParams(h.Hostname)
	np := map[string]interface{}{
		"hostName":           h.Hostname,
		"ipv4DefaultGateway": gw4,
		"ipv6DefaultGateway": gw6,
		"dnsServers":         dns,
		"domainName":         domain,
	}

	partitions, _ := disk.Partitions(true)
	fileStores := buildFileStores(partitions)
	syncFileStoreUUIDsFromDiskStores(fileStores, diskStores)
	openFD := int64(0)
	maxFD := int64(16711680)
	maxFDProc := int64(16711680)
	if runtime.GOOS == "windows" {
		openFD = wmiSumProcessHandles()
	}

	ver := map[string]interface{}{
		"version":     h.PlatformVersion,
		"codeName":    "",
		"buildNumber": "",
		"versionStr":  h.PlatformVersion,
	}

	limit := procCount
	if limit < 1 {
		limit = 200
	}
	if limit > 500 {
		limit = 500
	}
	procs, _ := buildProcessesList(v.Total, limit)
	if procs == nil {
		procs = []map[string]interface{}{}
	}
	cur, _ := buildCurrentProcess(v.Total)

	ips := wmiInternetProtocolStatsAggregate()
	conns, err := buildConnectionsInet()
	if err != nil {
		conns = []map[string]interface{}{}
	}
	if runtime.GOOS == "linux" {
		fillProtoStatsLinux(ips)
	}
	ips["connections"] = conns

	svcList := wmiServicesList()
	if len(svcList) == 0 {
		svcList = []map[string]interface{}{}
		if ss := servicesSnapshot(); len(ss) > 0 {
			for _, m := range ss {
				svcList = append(svcList, m)
			}
		}
	}

	sessions := wmiSessionsList()
	if sessions == nil {
		sessions = []map[string]interface{}{}
	}
	if runtime.GOOS != "windows" {
		if u, err := host.Users(); err == nil {
			for _, x := range u {
				sessions = append(sessions, map[string]interface{}{
					"userName":       x.User,
					"terminalDevice": x.Terminal,
					"loginTime":      int64(x.Started),
					"host":           x.Host,
				})
			}
		}
	}

	bootSec := int64(h.BootTime)

	osManu := csManufacturer
	if runtime.GOOS == "windows" {
		osManu = "Microsoft"
	}

	operatingSystem := map[string]interface{}{
		"family":         osFamilyString(),
		"manufacturer":   osManu,
		"bitness":        64,
		"systemBootTime": bootSec,
		"elevated":       IsProcessElevated(),
		"systemUptime":   int64(h.Uptime),
		"processCount":   procCount,
		"threadCount":    threadCount,
		"processId":      os.Getpid(),
		"threadId":       currentThreadID(),
		"versionInfo":    ver,
		"fileSystem": map[string]interface{}{
			"fileStores":                   fileStores,
			"openFileDescriptors":          openFD,
			"maxFileDescriptors":           maxFD,
			"maxFileDescriptorsPerProcess": maxFDProc,
		},
		"networkParams":         np,
		"services":              svcList,
		"internetProtocolStats": ips,
		"sessions":              sessions,
		"processes":             procs,
		"currentProcess":        cur,
	}

	return map[string]interface{}{
		"lastUpdate":      time.Now().UnixMilli(),
		"operatingSystem": operatingSystem,
		"hardware":        hardware,
	}, nil
}

func buildFileStores(partitions []disk.PartitionStat) []map[string]interface{} {
	var fs []map[string]interface{}
	for _, p := range partitions {
		if p.Mountpoint == "" {
			continue
		}
		usage, err := disk.Usage(p.Mountpoint)
		if err != nil {
			continue
		}
		label := ""
		opts := strings.Join(p.Opts, ",")
		desc := "Fixed drive"
		if strings.TrimSpace(p.Fstype) != "" {
			desc = strings.TrimSpace(p.Fstype)
		}
		fs = append(fs, map[string]interface{}{
			"name":             p.Device,
			"volume":           "",
			"label":            label,
			"mount":            p.Mountpoint,
			"options":          opts,
			"uuid":             "",
			"logicalVolume":    "",
			"description":      desc,
			"freeSpace":        int64(usage.Free),
			"usableSpace":      int64(usage.Free),
			"totalSpace":       int64(usage.Total),
			"freeInodes":       int64(usage.InodesFree),
			"totalInodes":      int64(usage.InodesTotal),
			"type":             p.Fstype,
			"updateAttributes": false,
		})
	}
	if len(fs) == 0 {
		return []map[string]interface{}{}
	}
	return fs
}

// syncFileStoreUUIDsFromDiskStores alinha uuid do fileStore ao da partição em hardware.diskStores
// (o dashboard usa HealthCheckerSimpleSystemInfoModel.getDiskDataLabel, que cruza por uuid).
func syncFileStoreUUIDsFromDiskStores(fileStores []map[string]interface{}, diskStores []map[string]interface{}) {
	mountToUUID := map[string]string{}
	for _, ds := range diskStores {
		parts, ok := ds["partitions"].([]map[string]interface{})
		if !ok {
			continue
		}
		for _, part := range parts {
			mp, _ := part["mountPoint"].(string)
			u, _ := part["uuid"].(string)
			if strings.TrimSpace(mp) == "" || strings.TrimSpace(u) == "" {
				continue
			}
			for _, key := range mountKeyVariants(mp) {
				mountToUUID[key] = u
			}
		}
	}
	for _, fs := range fileStores {
		mount, _ := fs["mount"].(string)
		if mount == "" {
			continue
		}
		var u string
		for _, key := range mountKeyVariants(mount) {
			if v, ok := mountToUUID[key]; ok {
				u = v
				break
			}
		}
		if u != "" {
			fs["uuid"] = u
		}
	}
}

func mountKeyVariants(mount string) []string {
	m := strings.TrimSpace(strings.ReplaceAll(mount, `/`, `\`))
	var out []string
	add := func(s string) {
		s = strings.TrimSpace(s)
		if s == "" {
			return
		}
		for _, x := range out {
			if x == s {
				return
			}
		}
		out = append(out, s)
	}
	add(m)
	if len(m) >= 2 && m[1] == ':' {
		drive := strings.ToUpper(m[:2])
		add(drive + `\`)
		add(drive)
	}
	return out
}

func fallbackDiskStoresFromPartitions() []map[string]interface{} {
	partitions, err := disk.Partitions(true)
	if err != nil {
		return []map[string]interface{}{}
	}
	partUUID := "00000000-0000-0000-0000-000000000001"
	var stores []map[string]interface{}
	for _, p := range partitions {
		if p.Mountpoint == "" {
			continue
		}
		usage, err := disk.Usage(p.Mountpoint)
		if err != nil {
			continue
		}
		uuid := partUUID
		stores = append(stores, map[string]interface{}{
			"name":  p.Device,
			"model": "unknown",
			"partitions": []map[string]interface{}{
				{
					"uuid":           uuid,
					"identification": p.Mountpoint,
					"name":           p.Fstype,
					"type":           p.Fstype,
					"size":           int64(usage.Total),
					"major":          0,
					"minor":          0,
					"mountPoint":     p.Mountpoint,
				},
			},
		})
		partUUID = "00000000-0000-0000-0000-000000000002"
	}
	if len(stores) == 0 {
		return []map[string]interface{}{{
			"name": "disk", "model": "unknown",
			"partitions": []map[string]interface{}{{
				"uuid": "00000000-0000-0000-0000-000000000001",
			}},
		}}
	}
	return stores
}

func fillProtoStatsLinux(ips map[string]interface{}) {
	if runtime.GOOS != "linux" {
		return
	}
	stats, err := gopsutilnet.ProtoCounters([]string{"tcp", "udp"})
	if err != nil {
		return
	}
	var tcpM, udpM map[string]int64
	for _, s := range stats {
		switch s.Protocol {
		case "tcp":
			tcpM = s.Stats
		case "udp":
			udpM = s.Stats
		}
	}
	if tcpM != nil {
		ips["tcpv4Stats"] = map[string]interface{}{
			"connectionsEstablished": tcpM["CurrEstab"],
			"connectionsActive":      tcpM["ActiveOpens"],
			"connectionsPassive":     tcpM["PassiveOpens"],
			"connectionFailures":     tcpM["AttemptFails"],
			"connectionsReset":       tcpM["EstabResets"],
			"segmentsSent":           tcpM["OutSegs"],
			"segmentsReceived":       tcpM["InSegs"],
			"segmentsRetransmitted":  tcpM["RetransSegs"],
			"inErrors":               tcpM["InErrs"],
			"outResets":              tcpM["OutRsts"],
		}
		ips["tcpv6Stats"] = ips["tcpv4Stats"]
	}
	if udpM != nil {
		ips["udpv4Stats"] = map[string]interface{}{
			"datagramsSent":           udpM["OutDatagrams"],
			"datagramsReceived":       udpM["InDatagrams"],
			"datagramsNoPort":         udpM["NoPorts"],
			"datagramsReceivedErrors": udpM["InErrors"],
		}
		ips["udpv6Stats"] = ips["udpv4Stats"]
	}
}
