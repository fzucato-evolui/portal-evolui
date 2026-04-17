package wsjson

import (
	"encoding/json"
)

// Envelope espelha WebSocketMessageDTO<T> (shared).
type Envelope struct {
	From    string          `json:"from"`
	To      string          `json:"to"`
	Message json.RawMessage `json:"message"`
	Error   string          `json:"error,omitempty"`
}

func Marshal(env Envelope) ([]byte, error) {
	return json.Marshal(env)
}
