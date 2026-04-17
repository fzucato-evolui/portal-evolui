//go:build !windows

package sysinfo

import "github.com/shirou/gopsutil/v3/disk"

func wmiComputerSystem() (manufacturer, model, serial, name string, err error) {
	return "", "", "", "", nil
}

func wmiBIOS() (name, description, version, manufacturer, releaseDate string) {
	return "", "", "", "", ""
}

func wmiBaseboard() (version, manufacturer, serialNumber, model string) {
	return "", "", "", ""
}

func wmiSystemProductUUID() string { return "" }

func wmiPhysicalMemoryList() []map[string]interface{} { return nil }

func wmiNetworkParams(hostname string) (gw4, gw6 string, dns []string, domain string) {
	return "", "", []string{}, ""
}

func wmiServicesList() []map[string]interface{} { return nil }

func wmiSumProcessHandles() int64 { return 0 }

func wmiInternetProtocolStatsAggregate() map[string]interface{} {
	return map[string]interface{}{
		"tcpv4Stats": emptyTcpStats(),
		"tcpv6Stats": emptyTcpStats(),
		"udpv4Stats": emptyUdpStats(),
		"udpv6Stats": emptyUdpStats(),
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

func wmiDiskStoresPhysical(ioByLetter map[string]disk.IOCountersStat) ([]map[string]interface{}, error) {
	_ = ioByLetter
	return nil, nil
}

func wmiSessionsList() []map[string]interface{} { return nil }
