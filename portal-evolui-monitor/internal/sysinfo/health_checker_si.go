package sysinfo

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"net"
	"strings"
	"sync"
	"time"

	"github.com/shirou/gopsutil/v3/cpu"
	"github.com/shirou/gopsutil/v3/process"
)

// BuildHealthCheckerSystemInfoMap monta JSON compatível com HealthCheckerSystemInfoDTO para o frontend (parseFromSystemInfo).
func BuildHealthCheckerSystemInfoMap(ctx context.Context, hardwareUUID string) (map[string]interface{}, error) {
	return buildHealthCheckerSystemInfoMap(ctx, hardwareUUID)
}

// encodeIPForJacksonByteArray devolve string Base64 dos octetos do IP (contrato Jackson para byte[] no JSON).
func encodeIPForJacksonByteArray(s string) string {
	s = strings.TrimSpace(s)
	if s == "" {
		return ""
	}
	ip := net.ParseIP(s)
	if ip == nil {
		return base64.StdEncoding.EncodeToString([]byte(s))
	}
	return base64.StdEncoding.EncodeToString(ipTo4or16(ip))
}

func ipTo4or16(ip net.IP) []byte {
	if v4 := ip.To4(); v4 != nil {
		return v4
	}
	return ip.To16()
}

// normalizeTcpStateForOshi alinha com oshi.software.os.InternetProtocolStats.TcpState (enum no DTO Java).
func normalizeTcpStateForOshi(status string) string {
	s := strings.ToUpper(strings.TrimSpace(status))
	switch s {
	case "NONE":
		return "NONE"
	case "LISTENING":
		return "LISTEN"
	case "FIN_WAIT1":
		return "FIN_WAIT_1"
	case "FIN_WAIT2":
		return "FIN_WAIT_2"
	// OSHI enum TcpState usa SYN_RECV (Jackson deserializa no backend).
	case "SYN_RECEIVED":
		return "SYN_RECV"
	case "CLOSED", "LISTEN", "SYN_SENT", "SYN_RECV", "ESTABLISHED",
		"FIN_WAIT_1", "FIN_WAIT_2", "CLOSE_WAIT", "CLOSING", "LAST_ACK", "TIME_WAIT", "UNKNOWN":
		return s
	case "SYN_SENT2", "DELETE_TCB":
		return "UNKNOWN"
	default:
		return "UNKNOWN"
	}
}

func sampleCPULoadPercent() float64 {
	pcts, err := cpu.Percent(0, false)
	if err == nil && len(pcts) > 0 {
		var sum float64
		for _, p := range pcts {
			sum += p
		}
		return sum / float64(len(pcts))
	}
	// Primeira amostra ou delta vazio: janela curta (evita NaN no gráfico de pizza).
	pcts, err = cpu.Percent(80*time.Millisecond, false)
	if err != nil || len(pcts) == 0 {
		return 0
	}
	var sum float64
	for _, p := range pcts {
		sum += p
	}
	return sum / float64(len(pcts))
}

var (
	threadCountMu     sync.Mutex
	threadCountCached int
	threadCountAt     time.Time
)

func countTotalThreads() int {
	threadCountMu.Lock()
	defer threadCountMu.Unlock()
	if !threadCountAt.IsZero() && time.Since(threadCountAt) < 2*time.Second {
		return threadCountCached
	}
	plist, err := process.Processes()
	if err != nil {
		return 0
	}
	n := 0
	for _, p := range plist {
		if nt, err := p.NumThreads(); err == nil {
			n += int(nt)
		}
	}
	threadCountCached = n
	threadCountAt = time.Now()
	return n
}

// BuildHealthCheckerSystemInfoJSON retorna o JSON do DTO (objeto raiz).
func BuildHealthCheckerSystemInfoJSON(ctx context.Context, hardwareUUID string) (json.RawMessage, error) {
	m, err := BuildHealthCheckerSystemInfoMap(ctx, hardwareUUID)
	if err != nil {
		return nil, err
	}
	b, err := json.Marshal(m)
	if err != nil {
		return nil, fmt.Errorf("marshal system info: %w", err)
	}
	return b, nil
}
