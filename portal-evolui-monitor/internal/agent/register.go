package agent

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"sync"
	"time"

	"portal-evolui-monitor/internal/commandline"
	"portal-evolui-monitor/internal/config"
	"portal-evolui-monitor/internal/hc"
	"portal-evolui-monitor/internal/healthcheck"
	"portal-evolui-monitor/internal/logutil"
	"portal-evolui-monitor/internal/stomp"
	"portal-evolui-monitor/internal/store"
	"portal-evolui-monitor/internal/sysinfo"
	"portal-evolui-monitor/internal/wsjson"
)

// shellClient estado do prompt remoto (um por processo do monitor).
var shellClient = commandline.New()

// RegisterSubscriptions assina filas /queue/{topic}/{identifier} e tópicos como no MainController.
func RegisterSubscriptions(ctx context.Context, cl *stomp.Client, cfg *config.Config, session *Session, loops *Loops, httpClient *http.Client) error {
	loops.SetParent(ctx)

	id := cfg.Identifier
	logutil.V("[monitor] registro de filas STOMP com identifier=%q", id)
	for _, topic := range hc.IncomingQueueTopics {
		topic := topic
		switch topic {
		case "start-request":
			_, err := cl.Subscribe("/queue/"+topic+"/"+id, func(m stomp.Message) {
				logutil.V("[monitor] handler start-request disparado bodyLen=%d", len(m.Body))
				handleStartRequest(ctx, cl, cfg, session, m.Body)
			})
			if err != nil {
				return err
			}
		case "save-config-request":
			_, err := cl.Subscribe("/queue/"+topic+"/"+id, func(m stomp.Message) {
				logutil.V("[monitor] handler save-config-request disparado bodyLen=%d", len(m.Body))
				handleSaveConfigRequest(cl, cfg, session, m.Body)
			})
			if err != nil {
				return err
			}
		case "system-info-request-start":
			_, err := cl.Subscribe("/queue/"+topic+"/"+id, func(m stomp.Message) {
				logutil.V("[monitor] handler system-info-request-start disparado bodyLen=%d", len(m.Body))
				handleSystemInfoStart(ctx, cl, cfg, session, loops, m.Body)
			})
			if err != nil {
				return err
			}
		case "system-info-request-stop":
			_, err := cl.Subscribe("/queue/"+topic+"/"+id, func(m stomp.Message) {
				logutil.V("[monitor] handler system-info-request-stop disparado bodyLen=%d", len(m.Body))
				handleSystemInfoStop(ctx, cl, cfg, session, loops, m.Body)
			})
			if err != nil {
				return err
			}
		case "test-config-request":
			_, err := cl.Subscribe("/queue/"+topic+"/"+id, func(m stomp.Message) {
				logutil.V("[monitor] handler test-config-request disparado bodyLen=%d", len(m.Body))
				handleTestConfigRequest(ctx, cl, cfg, session, httpClient, m.Body)
			})
			if err != nil {
				return err
			}
		case "execute-command-request":
			_, err := cl.Subscribe("/queue/"+topic+"/"+id, func(m stomp.Message) {
				logutil.V("[monitor] handler execute-command-request bodyLen=%d", len(m.Body))
				handleExecuteCommandRequest(ctx, cl, cfg, session, loops, m.Body)
			})
			if err != nil {
				return err
			}
		default:
			_, err := cl.Subscribe("/queue/"+topic+"/"+id, func(m stomp.Message) {
				logutil.V("[monitor] mensagem fila %s bodyLen=%d body=%s", topic, len(m.Body), string(m.Body))
			})
			if err != nil {
				return err
			}
		}
	}

	rf := "/queue/" + hc.RoutingFailureTopic + "/" + id
	if _, err := cl.Subscribe(rf, func(m stomp.Message) {
		log.Printf("routing-failure: %s", string(m.Body))
	}); err != nil {
		return err
	}

	// WebSocketConfig: ao fechar qualquer sessão STOMP, broadcast com client = Identifier ou JWT (query Authorization).
	// Fechar o modal do admin desconecta o WebSocket do operador → client == JWT do operador (campo destination do token init),
	// não o hardware UUID do monitor. Paridade MainController: destination.equals(msg.get("client")).
	topic := "/topic/" + hc.ClientDisconnectionTopic
	if _, err := cl.Subscribe(topic, func(m stomp.Message) {
		var payload struct {
			Client string `json:"client"`
		}
		if err := json.Unmarshal(m.Body, &payload); err != nil {
			log.Printf("client-disconnection: JSON: %v", err)
			return
		}
		switch {
		case payload.Client == id:
			log.Printf("client-disconnection: sessão deste monitor encerrada no portal (identifier=%s)", id)
		case cfg.PairingJWT != "" && payload.Client == cfg.PairingJWT:
			log.Printf("client-disconnection: operador encerrou a sessão WebSocket (ex.: modal fechado); destino do token init não está mais conectado")
		default:
			// Outro peer na mesma instância — ignorar silenciosamente.
		}
	}); err != nil {
		return err
	}
	return nil
}

func handleTestConfigRequest(ctx context.Context, cl *stomp.Client, cfg *config.Config, session *Session, httpClient *http.Client, body []byte) {
	var envelope struct {
		From    string          `json:"from"`
		To      string          `json:"to"`
		Message json.RawMessage `json:"message"`
	}
	if err := json.Unmarshal(body, &envelope); err != nil {
		log.Printf("test-config-request JSON: %v", err)
		return
	}
	from := envelope.From
	ok, busyMsg := session.TryPeerOrBusy(from)
	if !ok {
		sendErr(cl, cfg.Identifier, from, hc.TopicResponseTestConfig, busyMsg)
		return
	}
	if len(envelope.Message) == 0 || string(envelope.Message) == "null" {
		sendErr(cl, cfg.Identifier, from, hc.TopicResponseTestConfig, "mensagem vazia")
		return
	}
	var mod healthcheck.ModuleConfig
	if err := json.Unmarshal(envelope.Message, &mod); err != nil {
		sendErr(cl, cfg.Identifier, from, hc.TopicResponseTestConfig, fmt.Sprintf("módulo JSON: %v", err))
		return
	}
	if httpClient == nil {
		httpClient = http.DefaultClient
	}
	result := healthcheck.CheckModule(ctx, httpClient, mod)
	msgBytes, err := json.Marshal(result)
	if err != nil {
		sendErr(cl, cfg.Identifier, from, hc.TopicResponseTestConfig, err.Error())
		return
	}
	reply := wsjson.Envelope{
		From:    cfg.Identifier,
		To:      from,
		Message: json.RawMessage(msgBytes),
	}
	raw, err := wsjson.Marshal(reply)
	if err != nil {
		return
	}
	if err := cl.Send("/app/"+hc.TopicResponseTestConfig, raw); err != nil {
		log.Printf("test-config-response: %v", err)
	}
}

func handleSystemInfoStart(ctx context.Context, cl *stomp.Client, cfg *config.Config, session *Session, loops *Loops, body []byte) {
	from, _, msg, err := parseBoolMessage(body)
	if err != nil {
		log.Printf("[monitor] system-info-request-start: JSON: %v body=%s", err, string(body))
		return
	}
	if from == "" {
		logutil.V("[monitor] system-info-request-start: ignorado (from vazio) body=%s", string(body))
		return
	}
	ok, busyMsg := session.TryPeerOrBusy(from)
	if !ok {
		log.Printf("[monitor] system-info-request-start: ocupado from=%q: %s", from, busyMsg)
		sendErr(cl, cfg.Identifier, from, hc.TopicSystemInfoResponse, busyMsg)
		return
	}
	realtime := msg != nil && *msg
	logutil.V("[monitor] system-info-request-start: from=%q realtime=%v", from, realtime)

	send := func() {
		si, err := sysinfo.BuildHealthCheckerSystemInfoJSON(ctx, cfg.Identifier)
		if err != nil {
			log.Printf("[monitor] system-info: BuildHealthCheckerSystemInfoJSON: %v", err)
			sendErr(cl, cfg.Identifier, from, hc.TopicSystemInfoResponse, err.Error())
			return
		}
		reply := wsjson.Envelope{
			From:    cfg.Identifier,
			To:      from,
			Message: si,
		}
		raw, err := wsjson.Marshal(reply)
		if err != nil {
			log.Printf("[monitor] system-info: marshal: %v", err)
			return
		}
		if err := cl.Send("/app/"+hc.TopicSystemInfoResponse, raw); err != nil {
			log.Printf("[monitor] system-info-response Send: %v", err)
			return
		}
		logutil.V("[monitor] system-info-response enviado ok para to=%q payloadLen=%d", from, len(raw))
	}

	if !realtime {
		send()
		return
	}

	logutil.V("[monitor] system-info: iniciando loop tempo real (2s)")
	loops.StartRealtime(ctx, func(rtCtx context.Context) {
		send()
		ticker := time.NewTicker(2 * time.Second)
		defer ticker.Stop()
		for {
			select {
			case <-rtCtx.Done():
				return
			case <-ticker.C:
				if rtCtx.Err() != nil {
					return
				}
				si, err := sysinfo.BuildHealthCheckerSystemInfoJSON(ctx, cfg.Identifier)
				if err != nil {
					log.Printf("system info (tempo real): %v", err)
					continue
				}
				reply := wsjson.Envelope{
					From:    cfg.Identifier,
					To:      from,
					Message: si,
				}
				raw, err := wsjson.Marshal(reply)
				if err != nil {
					continue
				}
				if err := cl.Send("/app/"+hc.TopicSystemInfoResponse, raw); err != nil {
					log.Printf("system-info-response: %v", err)
				}
			}
		}
	})
}

func handleSystemInfoStop(ctx context.Context, cl *stomp.Client, cfg *config.Config, session *Session, loops *Loops, body []byte) {
	var raw struct {
		From string `json:"from"`
	}
	if err := json.Unmarshal(body, &raw); err != nil {
		log.Printf("[monitor] system-info-request-stop JSON: %v", err)
		return
	}
	from := raw.From
	if from == "" {
		logutil.V("[monitor] system-info-request-stop: from vazio")
		return
	}
	logutil.V("[monitor] system-info-request-stop: from=%q", from)
	ok, busyMsg := session.TryPeerOrBusy(from)
	if !ok {
		sendErr(cl, cfg.Identifier, from, hc.TopicSystemInfoResponse, busyMsg)
		return
	}
	loops.StopRealtime()
	if p := loops.ParentCtx(); p != nil {
		loops.StartPeriodic(p)
	}
	si, err := sysinfo.BuildHealthCheckerSystemInfoJSON(ctx, cfg.Identifier)
	if err != nil {
		log.Printf("system info: %v", err)
		sendErr(cl, cfg.Identifier, from, hc.TopicSystemInfoResponse, err.Error())
		return
	}
	reply := wsjson.Envelope{
		From:    cfg.Identifier,
		To:      from,
		Message: si,
	}
	out, err := wsjson.Marshal(reply)
	if err != nil {
		return
	}
	if err := cl.Send("/app/"+hc.TopicSystemInfoResponse, out); err != nil {
		log.Printf("system-info-response: %v", err)
	}
}

func parseBoolMessage(body []byte) (from, to string, msg *bool, err error) {
	var raw struct {
		From    string          `json:"from"`
		To      string          `json:"to"`
		Message json.RawMessage `json:"message"`
	}
	if err := json.Unmarshal(body, &raw); err != nil {
		return "", "", nil, err
	}
	if len(raw.Message) == 0 || string(raw.Message) == "null" {
		return raw.From, raw.To, nil, nil
	}
	var b bool
	if err := json.Unmarshal(raw.Message, &b); err != nil {
		return raw.From, raw.To, nil, err
	}
	return raw.From, raw.To, &b, nil
}

func handleStartRequest(ctx context.Context, cl *stomp.Client, cfg *config.Config, session *Session, body []byte) {
	var env wsjson.Envelope
	if err := json.Unmarshal(body, &env); err != nil {
		log.Printf("[monitor] start-request: JSON inválido: %v body=%s", err, string(body))
		return
	}
	from := env.From
	logutil.V("[monitor] start-request: from=%q to=%q", from, env.To)
	if from == "" {
		logutil.V("[monitor] start-request: ignorado (from vazio)")
		return
	}
	ok, busyMsg := session.TryPeerOrBusy(from)
	if !ok {
		log.Printf("[monitor] start-request: sessão ocupada ou recusada para from=%q: %s", from, busyMsg)
		sendErr(cl, cfg.Identifier, from, hc.TopicResponseStart, busyMsg)
		return
	}

	si, err := sysinfo.BuildHealthCheckerSystemInfoJSON(ctx, cfg.Identifier)
	if err != nil {
		log.Printf("[monitor] start-request: BuildHealthCheckerSystemInfoJSON: %v", err)
		sendErr(cl, cfg.Identifier, from, hc.TopicResponseStart, err.Error())
		return
	}
	reply := wsjson.Envelope{
		From:    cfg.Identifier,
		To:      from,
		Message: si,
	}
	raw, err := wsjson.Marshal(reply)
	if err != nil {
		log.Printf("[monitor] start-request: marshal resposta: %v", err)
		return
	}
	if err := cl.Send("/app/"+hc.TopicResponseStart, raw); err != nil {
		log.Printf("[monitor] start-request: Send start-response falhou: %v", err)
		return
	}
	logutil.V("[monitor] start-request: start-response enviado ok para to=%q payloadLen=%d", from, len(raw))
}

func handleSaveConfigRequest(cl *stomp.Client, cfg *config.Config, session *Session, body []byte) {
	var raw struct {
		From    string          `json:"from"`
		To      string          `json:"to"`
		Message json.RawMessage `json:"message"`
	}
	if err := json.Unmarshal(body, &raw); err != nil {
		log.Printf("save-config-request JSON: %v", err)
		return
	}
	from := raw.From
	ok, busyMsg := session.TryPeerOrBusy(from)
	if !ok {
		sendErr(cl, cfg.Identifier, from, hc.TopicResponseSaveConfig, busyMsg)
		return
	}
	if len(raw.Message) == 0 {
		sendErr(cl, cfg.Identifier, from, hc.TopicResponseSaveConfig, "mensagem vazia")
		return
	}
	path := cfg.EffectiveConfigPath()
	if err := store.WriteEncryptedConfigFile(path, raw.Message); err != nil {
		log.Printf("gravar config: %v", err)
		sendErr(cl, cfg.Identifier, from, hc.TopicResponseSaveConfig, err.Error())
		return
	}
	logutil.V("configuration salva em %s", path)
	if cfg.ConfigFile == "" {
		cfg.ConfigFile = path
	}
	if err := config.ApplyFromHealthCheckerConfigJSON(cfg, raw.Message); err != nil {
		log.Printf("aplicar config: %v", err)
	}
	env := wsjson.Envelope{
		From:    cfg.Identifier,
		To:      from,
		Message: json.RawMessage("null"),
	}
	out, err := wsjson.Marshal(env)
	if err != nil {
		return
	}
	if err := cl.Send("/app/"+hc.TopicResponseSaveConfig, out); err != nil {
		log.Printf("save-config-response: %v", err)
	}
}

func handleExecuteCommandRequest(ctx context.Context, cl *stomp.Client, cfg *config.Config, session *Session, loops *Loops, body []byte) {
	var raw struct {
		From    string          `json:"from"`
		To      string          `json:"to"`
		Message json.RawMessage `json:"message"`
	}
	if err := json.Unmarshal(body, &raw); err != nil {
		log.Printf("execute-command-request JSON: %v", err)
		return
	}
	from := raw.From
	if from == "" {
		logutil.V("[monitor] execute-command-request: from vazio")
		return
	}
	var line string
	if len(raw.Message) > 0 && string(raw.Message) != "null" {
		if err := json.Unmarshal(raw.Message, &line); err != nil {
			log.Printf("execute-command-request message: %v", err)
			return
		}
	}
	ok, busyMsg := session.TryPeerOrBusy(from)
	if !ok {
		sendErr(cl, cfg.Identifier, from, hc.TopicResponseExecuteCommand, busyMsg)
		return
	}
	if loops.RealtimeActive() {
		sendErr(cl, cfg.Identifier, from, hc.TopicResponseExecuteCommand, "HealthChecker ocupado")
		return
	}
	var emitMu sync.Mutex
	emit := func(resp commandline.ConsoleResponse) {
		emitMu.Lock()
		defer emitMu.Unlock()
		dto := struct {
			CurrentDirectory string `json:"currentDirectory,omitempty"`
			Finished         bool   `json:"finished"`
			Output           string `json:"output,omitempty"`
			OutputError      string `json:"outputError,omitempty"`
			Sequence         int    `json:"sequence"`
		}{
			CurrentDirectory: resp.CurrentDirectory,
			Finished:         resp.Finished,
			Output:           resp.Output,
			OutputError:      resp.OutputError,
			Sequence:         resp.Sequence,
		}
		msgBytes, err := json.Marshal(dto)
		if err != nil {
			return
		}
		env := wsjson.Envelope{
			From:    cfg.Identifier,
			To:      from,
			Message: json.RawMessage(msgBytes),
		}
		out, err := wsjson.Marshal(env)
		if err != nil {
			return
		}
		if err := cl.Send("/app/"+hc.TopicResponseExecuteCommand, out); err != nil {
			log.Printf("execute-command-response Send: %v", err)
			return
		}
		logutil.V("[monitor] execute-command-response seq=%d finished=%v", resp.Sequence, resp.Finished)
	}
	if err := shellClient.Execute(ctx, line, emit); err != nil {
		sendErr(cl, cfg.Identifier, from, hc.TopicResponseExecuteCommand, err.Error())
	}
}

func sendErr(cl *stomp.Client, fromID, to, appSuffix, msg string) {
	log.Printf("[monitor] enviando erro STOMP topic=%s from=%q to=%q err=%q", appSuffix, fromID, to, msg)
	env := wsjson.Envelope{
		From:    fromID,
		To:      to,
		Message: json.RawMessage("null"),
		Error:   msg,
	}
	raw, err := json.Marshal(env)
	if err != nil {
		log.Printf("[monitor] sendErr: marshal: %v", err)
		return
	}
	if err := cl.Send("/app/"+appSuffix, raw); err != nil {
		log.Printf("[monitor] sendErr: Send falhou: %v", err)
	}
}

// SendHey envia HEY para o JWT do operador (destination do token init).
func SendHey(cl *stomp.Client, cfg *config.Config) error {
	if cfg.PairingJWT == "" {
		return nil
	}
	env := wsjson.Envelope{
		From:    cfg.Identifier,
		To:      cfg.PairingJWT,
		Message: json.RawMessage("null"),
	}
	raw, err := wsjson.Marshal(env)
	if err != nil {
		return err
	}
	return cl.Send("/app/"+hc.TopicHey, raw)
}
