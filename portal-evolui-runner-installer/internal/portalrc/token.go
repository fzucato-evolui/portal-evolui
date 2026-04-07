package portalrc

import (
	"crypto/aes"
	"crypto/cipher"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"strings"
)

// Mesmo DEFAULT_KEY e IV zero que GeradorTokenPortalEvolui.java
const defaultKey = "KOfqT55eg0bU3mWS"

type Connection struct {
	UUID string `json:"uuid"`
	Host string `json:"host"`
	// Destination é o JWT (query Authorization no WebSocket); filas STOMP usam UUID, não este valor.
	Destination string `json:"destination"`
}

func DecryptPortalToken(hexCipher string) (*Connection, error) {
	plain, err := decryptHex(hexCipher, defaultKey)
	if err != nil {
		return nil, err
	}
	var c Connection
	if err := json.Unmarshal([]byte(plain), &c); err != nil {
		return nil, fmt.Errorf("json após decrypt: %w", err)
	}
	if c.UUID == "" || c.Host == "" || c.Destination == "" {
		return nil, fmt.Errorf("token inválido: faltam uuid, host ou destination")
	}
	return &c, nil
}

func decryptHex(hexStr, keyStr string) (string, error) {
	raw, err := hex.DecodeString(strings.TrimSpace(hexStr))
	if err != nil {
		return "", fmt.Errorf("hex: %w", err)
	}
	if len(raw)%aes.BlockSize != 0 {
		return "", fmt.Errorf("tamanho ciphertext inválido")
	}
	k := []byte(keyStr)
	if len(k) != 16 {
		return "", fmt.Errorf("chave deve ter 16 bytes")
	}
	block, err := aes.NewCipher(k)
	if err != nil {
		return "", err
	}
	iv := make([]byte, aes.BlockSize)
	mode := cipher.NewCBCDecrypter(block, iv)
	mode.CryptBlocks(raw, raw)
	if i := strings.IndexByte(string(raw), 0); i >= 0 {
		return string(raw[:i]), nil
	}
	return string(raw), nil
}
