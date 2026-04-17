package logutil

import "log"

var verbose = false

// SetVerbose controla mensagens operacionais (STOMP, handlers, etc.); erros continuam com log.Printf no código.
func SetVerbose(v bool) {
	verbose = v
}

// V emite log.Printf apenas com verbose ligado.
func V(format string, args ...interface{}) {
	if verbose {
		log.Printf(format, args...)
	}
}
