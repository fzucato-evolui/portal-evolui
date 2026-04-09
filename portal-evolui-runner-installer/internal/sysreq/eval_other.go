//go:build !linux && !windows

package sysreq

import (
	"fmt"
	"runtime"
)

func Evaluate() Report {
	return Report{
		Meets:  false,
		Detail: fmt.Sprintf("SO %q não é alvo suportado pelo instalador automático (use Linux ou Windows).", runtime.GOOS),
	}
}
