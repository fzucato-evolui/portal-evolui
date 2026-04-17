package agent

import (
	"context"
	"sync"
)

// Loops coordena exclusão mútua entre monitoramento REST periódico e stream STOMP system-info (paridade MainController).
type Loops struct {
	mu sync.Mutex

	parentCtx context.Context

	periodicFunc   func(context.Context)
	periodicCancel context.CancelFunc

	realtimeCancel context.CancelFunc
}

// NewLoops cria o coordenador de timers.
func NewLoops() *Loops {
	return &Loops{}
}

// SetParent guarda o contexto do supervisor (reinício do REST após parar tempo real).
func (l *Loops) SetParent(ctx context.Context) {
	l.mu.Lock()
	defer l.mu.Unlock()
	l.parentCtx = ctx
}

// ParentCtx retorna o contexto do supervisor.
func (l *Loops) ParentCtx() context.Context {
	l.mu.Lock()
	defer l.mu.Unlock()
	return l.parentCtx
}

// SetPeriodicFunc define o loop REST (chamado uma vez antes de StartPeriodic).
func (l *Loops) SetPeriodicFunc(fn func(context.Context)) {
	l.mu.Lock()
	defer l.mu.Unlock()
	l.periodicFunc = fn
}

// StopPeriodic cancela o ciclo REST sem encerrar o processo.
func (l *Loops) StopPeriodic() {
	l.mu.Lock()
	if l.periodicCancel != nil {
		l.periodicCancel()
		l.periodicCancel = nil
	}
	l.mu.Unlock()
}

// StartPeriodic inicia (ou reinicia) o ciclo REST filho de ctx.
func (l *Loops) StartPeriodic(ctx context.Context) {
	l.mu.Lock()
	l.parentCtx = ctx
	if l.periodicCancel != nil {
		l.periodicCancel()
		l.periodicCancel = nil
	}
	if l.periodicFunc == nil {
		l.mu.Unlock()
		return
	}
	ctx2, cancel := context.WithCancel(ctx)
	l.periodicCancel = cancel
	fn := l.periodicFunc
	l.mu.Unlock()
	go fn(ctx2)
}

// StopRealtime encerra o stream system-info (2s).
func (l *Loops) StopRealtime() {
	l.mu.Lock()
	if l.realtimeCancel != nil {
		l.realtimeCancel()
		l.realtimeCancel = nil
	}
	l.mu.Unlock()
}

// RealtimeActive indica se o stream system-info (tempo real) está ativo — alinhado à ideia de não competir com o prompt.
func (l *Loops) RealtimeActive() bool {
	l.mu.Lock()
	defer l.mu.Unlock()
	return l.realtimeCancel != nil
}

// StartRealtime para o REST e inicia o loop de tempo real (ex.: system-info a cada 2s).
func (l *Loops) StartRealtime(parent context.Context, run func(ctx context.Context)) {
	l.StopPeriodic()
	l.mu.Lock()
	if l.realtimeCancel != nil {
		l.realtimeCancel()
	}
	ctx, cancel := context.WithCancel(parent)
	l.realtimeCancel = cancel
	l.mu.Unlock()
	go run(ctx)
}
