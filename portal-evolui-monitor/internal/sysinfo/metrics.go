package sysinfo

import (
	"context"
	"fmt"
	"time"

	"github.com/shirou/gopsutil/v3/cpu"
	"github.com/shirou/gopsutil/v3/disk"
	"github.com/shirou/gopsutil/v3/mem"
)

// AlertMetrics amostras usadas nas regras de alerta (paridade HealthCheckController Java).
type AlertMetrics struct {
	MemoryUsedPercent int
	CPUPercent        int
	// DiskUsagePercent por label/mount (consumo %).
	DiskUsagePercent map[string]int
	// OpenFilesPercent -1 = indisponível nesta plataforma/coleta.
	OpenFilesPercent int
}

// SampleAlertMetrics coleta CPU (janela ~1s), memória e discos. Leve o suficiente para rodar em loop periódico.
func SampleAlertMetrics(ctx context.Context) (*AlertMetrics, error) {
	_ = ctx
	v, err := mem.VirtualMemory()
	if err != nil {
		return nil, err
	}
	memPct := 0
	if v.Total > 0 {
		memPct = int(100 * (v.Total - v.Available) / v.Total)
	}

	cpuPct, err := sampleCPUPercent()
	if err != nil {
		return nil, err
	}

	partitions, err := disk.Partitions(false)
	if err != nil {
		return nil, err
	}
	diskMap := make(map[string]int)
	for _, p := range partitions {
		if p.Mountpoint == "" {
			continue
		}
		u, err := disk.Usage(p.Mountpoint)
		if err != nil {
			continue
		}
		if u.Total == 0 {
			continue
		}
		label := p.Mountpoint
		if p.Device != "" {
			label = fmt.Sprintf("%s (%s)", p.Mountpoint, p.Device)
		}
		pct := int(100 * (u.Total - u.Free) / u.Total)
		diskMap[label] = pct
		if len(diskMap) >= 8 {
			break
		}
	}

	return &AlertMetrics{
		MemoryUsedPercent: memPct,
		CPUPercent:        cpuPct,
		DiskUsagePercent:  diskMap,
		OpenFilesPercent:  -1,
	}, nil
}

func sampleCPUPercent() (int, error) {
	p, err := cpu.Percent(1*time.Second, false)
	if err != nil {
		return 0, err
	}
	if len(p) == 0 {
		return 0, nil
	}
	var sum float64
	for _, x := range p {
		sum += x
	}
	return int(sum / float64(len(p))), nil
}
