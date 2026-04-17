package hc

// Tópicos STOMP (HealthCheckerMessageTopicConstants) para os quais o agente assina filas
// /queue/{nome}/{identifier} quando o portal encaminha mensagens com destino = identifier do monitor.
var IncomingQueueTopics = []string{
	"hey",
	"start-request",
	"execute-command-request",
	"system-info-request-start",
	"system-info-request-stop",
	"test-config-request",
	"save-config-request",
}

const (
	ClientDisconnectionTopic = "client-disconnection"
	RoutingFailureTopic      = "routing-failure"
)

const (
	TopicHey                    = "hey"
	TopicResponseStart          = "start-response"
	TopicResponseSaveConfig     = "save-config-response"
	TopicSystemInfoResponse     = "system-info-response"
	TopicResponseTestConfig     = "test-config-response"
	TopicResponseExecuteCommand = "execute-command-response"
)
