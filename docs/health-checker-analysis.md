# Conversa(prompt que gerou a análise)
Olhe o projeto health-checker. Ele é um jar que é executado nos nossos clientes como um service. A idéia é que ele funcione como um runner do github. Como o nome diz, ele serve para coletar a saúde das máquinas dos clientes. Ele faz uma análise de tempos em tempos e manda essa informação para o nosso sistema que é o portal-evolui-backend. A classe mais importante é a portal-evolui-healthchecker/src/main/java/br/com/evolui/healthchecker/controller/MainController.java. Quando em modo run, ele abre ouma conexão websocket com o nosso sistema. Olhe a classe portal-evolui-backend/src/main/java/br/com/evolui/portalevolui/web/config/WebSocketConfig.java.
Outra funcionalidade bem legal, é a possibilidade de simular um prompt no frontend para enviar comandos para a máquina via websocket.
A tela é portal-evolui-frontend/src/app/modules/admin/health-checker/modal/health-checker-modal.component.html e no projeto do healthchecker ele processa em portal-evolui-healthchecker/src/main/java/br/com/evolui/healthchecker/controller/CommandLineController.java. Isso ajuda muito pq, na maioria das vezes, nossos clientes estão numa estrtura coorporativa que exige vpn, proxy e o escambal. Mas não funciona 100%.
No entanto, ele é muito instável. Parece que ele consome muita memória, ele perde a conexão websocket com o sistema principal e não volta a ficar online. Teria que dar uma reavaliada nas duas partes da comunicação (healthchecker e portal-evolui-backend). A ideia é que, futuramente, ele possa substituir os nossos runners e ser o responsável pela execução dos actions que fazem atualizações e compilações dos nossos produtos. Também comecei a implementar ele pra funcionar como um Java Agent pra tentar descobrir picos de processamento nos nossos produtos que são quase todos Java. Mas isso é mais pra frente. Ele está em Java 8 pq nossos sistemas são todos em Java 8, por enquanto, mas já existe um planejamento de upgrade.
Dito tudo isso:
1 - Pra esse tipo de sistema de ter um cliente em comunicação o tempo todo com o nosso portal, o Java é a melhor opção? O runner github, por exemplo, é em node.
2 - O que voce acha dessa ideia e o que poderíamos melhorar? Hoje ele não mantém um histórico da saúde do que é enviado das máquinas. Ele só salva a saúde atual e encvia alertas se o threshold configurado é alcançado de memória, de disco...
3 - O sistema não deve sobrecarregar nem o cliente e nem o nosso portal. Deve ser uma comunicação persistente (always on) mas que não prejudique todas as outras funcionalidades. Entende?

Então na sua opinião, isso ficaria muito melhor se não fosse java, né? Pq o Java Agent eu poderia construir em separado não precisaria ficar nele. O importante é cumprir o papel que eu quero. Monitorar a saúde do sistema emitindo alertas, poder verificar em tempo real quando preciso (se viu a tela que falei, sabe do que estou falando) e queria muito ter uma interface funcional que simulasse um prompt na máquina
E na parte do backend que está recebendo o stomp e manda as mensagens, verifica se o websocker está online, faz os broadcasts... Está ok?

# Health-Checker - Analise Completa e Plano de Reescrita

## Contexto
O health-checker e um JAR executado como service nos clientes. Coleta saude das maquinas e se comunica com o portal-evolui-backend via WebSocket STOMP. Tambem permite executar comandos remotos (shell) via frontend.

## Decisao: Reescrever o agente em Go
- Java e overkill para agente leve com WebSocket persistente
- Go: binario unico, sem runtime, ~5-10MB de memoria, WebSocket nativo excelente, cross-compile trivial
- Java Agent para profiling sera um projeto separado
- Backend (portal-evolui-backend) precisa de revisao junto

## Funcionalidades do agente
1. Monitoramento de saude (CPU, memoria, disco, rede, processos, servicos)
2. Emissao de alertas quando thresholds sao alcancados
3. Consulta em tempo real via WebSocket (frontend pede, agente responde)
4. Shell remoto - simula prompt no frontend, executa comandos na maquina via WebSocket
5. Futuro: substituir runners GitHub para execucao de actions de atualizacao/compilacao

## Problemas encontrados no agente Java atual

### Memoria
- `System.gc()` chamado explicitamente no scheduler (~720x/dia) - extremamente caro
- `CommandLineController` cria `newSingleThreadExecutor()` por comando e NUNCA faz shutdown
- `HealthCheckerSystemInfoDTO` acumula lista de processos sem limpeza
- ObjectMapper recriado por mensagem em vez de reutilizado

### Conexao WebSocket
- Reconexao so tenta apos 3x o intervalo de health check (~6+ minutos)
- Sem exponential backoff
- `isWebsocketConnected()` timeout hardcoded 40 segundos sem TCP keep-alive
- Cleanup incompleto na reconexao (race condition sessao antiga vs nova)
- `destination` compartilhado entre threads sem `volatile`

### Dependencias obsoletas
- Tyrus 1.13.1 (2015) - vulnerabilidades conhecidas
- SLF4J 2.0.0-alpha0 em producao
- Spring WebFlux 6.0.12 misturado com Spring 5.3.23

### Execucao de comandos
- Sem timeout por processo - comando travado bloqueia health-checker inteiro
- `Process.destroy()` sem `Process.waitFor()` - processos zumbi
- Busy lock single-destination impede processamento paralelo

## Problemas encontrados no backend (portal-evolui-backend)

### Sessoes orfas
- `ConcurrentHashMap<String, WebSocketSession> connectedDevices` e estatico
- So remove sessoes no `afterConnectionClosed()` - se conexao cai sem close limpo, entrada fica para sempre
- Device aparece "online" mas esta morto
- Sem tarefa agendada para purgar sessoes stale

### Sem heartbeat/ping-pong
- Nenhum keep-alive configurado no broker STOMP
- Conexoes mortas so detectadas na tentativa de envio
- Firewalls/proxies corporativos cortam conexoes ociosas silenciosamente

### Zero tratamento de excecao no WebSocket controller
- `HealthCheckerWebsocketController` nao tem nenhum try-catch
- Erro no processamento = cliente espera resposta para sempre
- Sem log, sem resposta de erro

### Buffers excessivos
- 10MB texto, 10MB binario, send timeout 3.000.000ms (~833 horas)
- System info nao deveria passar de 512KB-1MB

### Outros
- `setAllowedOrigins("*")` sem CSRF
- `@PostLoad` para online status tem race condition
- Nao compativel com multiplas instancias (map estatico local ao JVM)
- Sem logging de conexao/desconexao

## O que esta OK no backend
- ConcurrentHashMap thread-safe
- Roteamento por queue (`/queue/TOPIC/USER_TOKEN`) correto (point-to-point)
- Broadcast de desconexao em `/topic/CLIENT_DISCONECTION`
- `@Transactional` nos endpoints REST

## Plano de melhoria no backend (independente da reescrita do agente)
1. Configurar heartbeat STOMP: `.setTaskScheduler()` com `setHeartbeatServer([10000, 10000])`
2. Cleanup agendado: `@Scheduled` que percorre `connectedDevices` e remove sessoes mortas
3. Try-catch nos handlers `@MessageMapping` com resposta de erro ao client
4. Logs de conexao/desconexao
5. Reduzir buffers para 512KB-1MB
6. Guardar historico de saude no banco (hoje so salva estado atual)

## Arquivos importantes

### Agente (healthchecker/)
- `portal-evolui-healthchecker/src/main/java/br/com/evolui/healthchecker/controller/MainController.java` - classe principal, WebSocket, scheduler
- `portal-evolui-healthchecker/src/main/java/br/com/evolui/healthchecker/controller/CommandLineController.java` - execucao de comandos remotos
- `healthchecker/pom.xml` - dependencias

### Backend (portal-evolui-backend/)
- `portal-evolui-backend/src/main/java/br/com/evolui/portalevolui/web/config/WebSocketConfig.java` - config WebSocket + connectedDevices
- Controller WebSocket do health-checker (grep por HealthCheckerWebsocketController)
- `portal-evolui-backend/src/main/java/br/com/evolui/portalevolui/web/rest/controller/admin/HealthCheckerAdminRestController.java`

### Frontend
- `portal-evolui-frontend/src/app/modules/admin/health-checker/modal/health-checker-modal.component.html` - tela principal
- `portal-evolui-frontend/src/app/modules/admin/health-checker/modal/health-checker-modal.component.ts`
- `portal-evolui-frontend/src/app/modules/admin/health-checker/modal/health-checker-monitor-modal.component.ts` - monitor tempo real
- `portal-evolui-frontend/src/app/modules/admin/health-checker/health-checker.service.ts`

## Topicos STOMP
- HEY, START_REQUEST/RESPONSE, EXECUTE_COMMAND_REQUEST/RESPONSE
- SYSTEM_INFO_REQUEST_START/STOP, SYSTEM_INFO_RESPONSE
- TEST_CONFIG_REQUEST/RESPONSE, SAVE_CONFIG_REQUEST/RESPONSE
- CLIENT_DISCONECTION (broadcast)
