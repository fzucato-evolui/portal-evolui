//go:build windows

package sysreq

import (
	"fmt"
	"strconv"
	"strings"

	"golang.org/x/sys/windows/registry"
)

// Windows 10+ expõe CurrentMajorVersionNumber no registo; evita RtlGetVersion (API instável entre versões de x/sys).
func Evaluate() Report {
	k, err := registry.OpenKey(registry.LOCAL_MACHINE, `SOFTWARE\Microsoft\Windows NT\CurrentVersion`, registry.QUERY_VALUE)
	if err != nil {
		return Report{
			Meets:  true,
			Detail: "Não foi possível abrir o registo da versão do Windows; não bloqueamos a instalação. " + err.Error(),
		}
	}
	defer k.Close()

	majorU, _, errMaj := k.GetIntegerValue("CurrentMajorVersionNumber")
	minorU, _, errMin := k.GetIntegerValue("CurrentMinorVersionNumber")
	buildStr, _, errBuild := k.GetStringValue("CurrentBuildNumber")

	if errMaj == nil && errMin == nil {
		major := uint32(majorU)
		minor := uint32(minorU)
		build := buildStr
		if errBuild != nil {
			build = "?"
		}
		if major < 10 {
			return Report{
				Meets: false,
				Detail: fmt.Sprintf(
					"Windows %d.%d (build %s): o pacote oficial do actions/runner requer em geral Windows 10, Windows 11 ou Windows Server 2016+.",
					major, minor, build,
				),
			}
		}
		return Report{
			Meets: true,
			Detail: fmt.Sprintf(
				"Windows %d.%d (build %s): versão compatível com o pacote oficial do runner.",
				major, minor, build,
			),
		}
	}

	// Windows mais antigo (sem DWORD CurrentMajorVersionNumber): tenta CurrentVersion "6.x"
	cv, _, errCv := k.GetStringValue("CurrentVersion")
	if errCv != nil || cv == "" {
		return Report{
			Meets:  true,
			Detail: "Não foi possível determinar a versão do Windows; não bloqueamos a instalação.",
		}
	}
	parts := strings.Split(cv, ".")
	if len(parts) > 0 {
		if maj, err := strconv.Atoi(parts[0]); err == nil && maj >= 10 {
			return Report{Meets: true, Detail: fmt.Sprintf("Windows (CurrentVersion=%s): assumindo compatível.", cv)}
		}
	}
	return Report{
		Meets: false,
		Detail: fmt.Sprintf(
			"Windows (CurrentVersion=%s): ambiente possivelmente antigo; o runner oficial requer em geral Windows 10+.",
			cv,
		),
	}
}
