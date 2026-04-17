package crypto

import (
	"bytes"
	"testing"
)

func TestDecryptEncryptRoundtrip(t *testing.T) {
	plain := []byte(`{"destination":"x","host":"http://localhost:8080","login":{"login":"u","password":"p"}}`)
	cipher, err := EncryptHex(plain, DefaultKey)
	if err != nil {
		t.Fatal(err)
	}
	out, err := DecryptHex(cipher, DefaultKey)
	if err != nil {
		t.Fatal(err)
	}
	if !bytes.Equal(out, plain) {
		t.Fatalf("roundtrip mismatch:\n%q\n%q", plain, out)
	}
}
