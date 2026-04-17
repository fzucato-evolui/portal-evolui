// Package crypto replica AES/CBC/NoPadding + padding com \\0 de GeradorTokenPortalEvolui (portal-evolui-shared).
package crypto

import (
	"bytes"
	"crypto/aes"
	"crypto/cipher"
	"encoding/hex"
	"errors"
	"fmt"
	"strings"
)

// DefaultKey é o mesmo de GeradorTokenPortalEvolui.DEFAULT_KEY (16 bytes).
const DefaultKey = "KOfqT55eg0bU3mWS"

// DecryptHex descriptografa string hexadecimal gerada por GeradorTokenPortalEvolui.encrypt.
func DecryptHex(hexCipher, key string) ([]byte, error) {
	if key == "" {
		key = DefaultKey
	}
	if len(key) != aes.BlockSize {
		return nil, fmt.Errorf("chave AES-128 deve ter %d bytes", aes.BlockSize)
	}
	raw, err := hex.DecodeString(strings.TrimSpace(hexCipher))
	if err != nil {
		return nil, err
	}
	if len(raw) == 0 || len(raw)%aes.BlockSize != 0 {
		return nil, errors.New("texto hex inválido para AES/CBC")
	}
	block, err := aes.NewCipher([]byte(key))
	if err != nil {
		return nil, err
	}
	iv := make([]byte, aes.BlockSize)
	mode := cipher.NewCBCDecrypter(block, iv)
	plain := make([]byte, len(raw))
	mode.CryptBlocks(plain, raw)
	if i := bytes.IndexByte(plain, 0); i >= 0 {
		plain = plain[:i]
	}
	return plain, nil
}

// EncryptHex criptografa e devolve hex minúsculo (paridade com byteToHex do Java).
func EncryptHex(plain []byte, key string) (string, error) {
	if key == "" {
		key = DefaultKey
	}
	if len(key) != aes.BlockSize {
		return "", fmt.Errorf("chave AES-128 deve ter %d bytes", aes.BlockSize)
	}
	padded := padZeros(plain, aes.BlockSize)
	block, err := aes.NewCipher([]byte(key))
	if err != nil {
		return "", err
	}
	iv := make([]byte, aes.BlockSize)
	mode := cipher.NewCBCEncrypter(block, iv)
	out := make([]byte, len(padded))
	mode.CryptBlocks(out, padded)
	return hex.EncodeToString(out), nil
}

func padZeros(data []byte, block int) []byte {
	r := len(data) % block
	if r == 0 {
		out := make([]byte, len(data))
		copy(out, data)
		return out
	}
	n := block - r
	out := make([]byte, len(data)+n)
	copy(out, data)
	return out
}
