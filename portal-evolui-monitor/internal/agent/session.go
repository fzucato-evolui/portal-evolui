package agent

import "sync"

// Session guarda o peer STOMP (JWT do operador no portal), alinhado a MainController.destination.
type Session struct {
	mu   sync.Mutex
	Peer string
}

func NewSession(initialPeer string) *Session {
	return &Session{Peer: initialPeer}
}

// TryPeerOrBusy aplica a regra do JAR: se já há peer e o from é outro, ocupaço.
// Se ok, atualiza Peer com from quando from != "".
func (s *Session) TryPeerOrBusy(from string) (ok bool, busyMsg string) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.Peer != "" && from != "" && s.Peer != from {
		return false, "HealthChecker ocupado"
	}
	if from != "" {
		s.Peer = from
	}
	return true, ""
}
