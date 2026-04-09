//go:build linux

package sysreq

import (
	"fmt"
	"os/exec"
	"regexp"
	"runtime"
	"strconv"
	"strings"
)

// Binários Linux oficiais do Node (em externals/node*) no actions/runner exigem glibc >= 2.28 (Node 18+ pré-compilado; CentOS/RHEL 7 com 2.17 falham).
// Distros com musl (Alpine) não usam o pacote linux-x64/linux-arm64 glibc do GitHub sem camada de compatibilidade.
const minGlibcMajor = 2
const minGlibcMinor = 28

// Evaluate valida arquitetura, deteta musl e compara glibc.
func Evaluate() Report {
	switch runtime.GOARCH {
	case "amd64", "arm64":
	default:
		return Report{
			Meets:  false,
			Detail: fmt.Sprintf("Arquitetura %q: os pacotes oficiais do runner em Linux são amd64 e arm64.", runtime.GOARCH),
		}
	}

	if out, err := exec.Command("ldd", "--version").CombinedOutput(); err == nil && len(out) > 0 {
		low := strings.ToLower(string(out))
		if strings.Contains(low, "musl") {
			return Report{
				Meets:  false,
				Detail: "C runtime musl (ex.: Alpine): o pacote oficial do GitHub para Linux é feito para glibc. Use uma distro com glibc ou fluxo de instalação manual/alternativo.",
			}
		}
	}

	maj, min, ok := detectGlibc()
	if !ok {
		return Report{
			Meets: false,
			Detail: "Não foi possível detectar a versão do glibc (tente getconf GNU_LIBC_VERSION ou ldd --version). " +
				"Os binários Node oficiais do runner exigem normalmente glibc >= 2.28.",
		}
	}
	if maj < minGlibcMajor || (maj == minGlibcMajor && min < minGlibcMinor) {
		return Report{
			Meets: false,
			Detail: fmt.Sprintf(
				"glibc %d.%d é antigo demais: o pacote oficial do runner (Node em externals/node*) precisa em geral de glibc >= %d.%d.",
				maj, min, minGlibcMajor, minGlibcMinor,
			),
		}
	}
	return Report{
		Meets:  true,
		Detail: fmt.Sprintf("Linux %s, glibc %d.%d: OK para o pacote oficial do actions/runner.", runtime.GOARCH, maj, min),
	}
}

func detectGlibc() (maj, min int, ok bool) {
	if out, err := exec.Command("getconf", "GNU_LIBC_VERSION").Output(); err == nil {
		if m, n, ok := parseGlibcVersion(string(out)); ok {
			return m, n, true
		}
	}
	out, err := exec.Command("ldd", "--version").CombinedOutput()
	if err != nil || len(out) == 0 {
		return 0, 0, false
	}
	first := strings.Split(string(out), "\n")[0]
	return parseGlibcVersion(first)
}

// Aceita linhas como "glibc 2.35" ou "ldd (Ubuntu GLIBC 2.35) 2.35".
func parseGlibcVersion(s string) (maj, min int, ok bool) {
	re := regexp.MustCompile(`(\d+)\.(\d+)`)
	matches := re.FindAllStringSubmatch(s, -1)
	if len(matches) == 0 {
		return 0, 0, false
	}
	// Usa a última ocorrência (ex.: "... 2.35)" no fim da linha do ldd).
	last := matches[len(matches)-1]
	major, err1 := strconv.Atoi(last[1])
	minor, err2 := strconv.Atoi(last[2])
	if err1 != nil || err2 != nil {
		return 0, 0, false
	}
	return major, minor, true
}
