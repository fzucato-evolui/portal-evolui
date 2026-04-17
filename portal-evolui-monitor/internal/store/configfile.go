// Package store persiste configuration.hck cifrado (paridade UtilController no JAR).
package store

import (
	"os"

	"portal-evolui-monitor/internal/crypto"
)

// ReadEncryptedConfigFile lê o hex do disco e descriptografa para JSON do HealthCheckerConfigDTO.
func ReadEncryptedConfigFile(path string) ([]byte, error) {
	b, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	return crypto.DecryptHex(string(b), crypto.DefaultKey)
}

// WriteEncryptedConfigFile grava o JSON do DTO cifrado em hex (mesmo algoritmo do Java).
func WriteEncryptedConfigFile(path string, plainJSON []byte) error {
	hex, err := crypto.EncryptHex(plainJSON, crypto.DefaultKey)
	if err != nil {
		return err
	}
	return os.WriteFile(path, []byte(hex), 0600)
}
