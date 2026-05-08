//go:build windows

package sysreq

import (
	"fmt"
	"strconv"
	"strings"
	"unsafe"

	"golang.org/x/sys/windows"
	"golang.org/x/sys/windows/registry"
)

// Windows 10+ expõe CurrentMajorVersionNumber no registo; evita RtlGetVersion (API instável entre versões de x/sys).
func Evaluate() Report {
	elevated := isElevated()

	k, err := registry.OpenKey(registry.LOCAL_MACHINE, `SOFTWARE\Microsoft\Windows NT\CurrentVersion`, registry.QUERY_VALUE)
	if err != nil {
		return Report{
			Meets:    true,
			Detail:   "Não foi possível abrir o registo da versão do Windows; não bloqueamos a instalação. " + err.Error(),
			Elevated: elevated,
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
				Elevated: elevated,
			}
		}
		return Report{
			Meets: true,
			Detail: fmt.Sprintf(
				"Windows %d.%d (build %s): versão compatível com o pacote oficial do runner.",
				major, minor, build,
			),
			Elevated: elevated,
		}
	}

	// Windows mais antigo (sem DWORD CurrentMajorVersionNumber): tenta CurrentVersion "6.x"
	cv, _, errCv := k.GetStringValue("CurrentVersion")
	if errCv != nil || cv == "" {
		return Report{
			Meets:    true,
			Detail:   "Não foi possível determinar a versão do Windows; não bloqueamos a instalação.",
			Elevated: elevated,
		}
	}
	parts := strings.Split(cv, ".")
	if len(parts) > 0 {
		if maj, err := strconv.Atoi(parts[0]); err == nil && maj >= 10 {
			return Report{
				Meets:    true,
				Detail:   fmt.Sprintf("Windows (CurrentVersion=%s): assumindo compatível.", cv),
				Elevated: elevated,
			}
		}
	}
	return Report{
		Meets: true,
		Detail: fmt.Sprintf(
			"Windows (CurrentVersion=%s): ambiente possivelmente antigo; seguindo com a instalação e delegando validação final ao instalador oficial do runner.",
			cv,
		),
		Elevated: elevated,
	}
}

// isElevated detecta se o processo atual está rodando elevado (Administrator).
// Usa OpenProcessToken + GetTokenInformation(TokenElevation) — mais robusto que IsUserAnAdmin
// por causa do comportamento de Linked Tokens em UAC.
func isElevated() bool {
	var token windows.Token
	if err := windows.OpenProcessToken(windows.CurrentProcess(), windows.TOKEN_QUERY, &token); err != nil {
		return false
	}
	defer token.Close()
	var elevation uint32
	var ret uint32
	err := windows.GetTokenInformation(
		token,
		windows.TokenElevation,
		(*byte)(unsafe.Pointer(&elevation)),
		uint32(unsafe.Sizeof(elevation)),
		&ret,
	)
	if err != nil {
		return false
	}
	return elevation != 0
}
