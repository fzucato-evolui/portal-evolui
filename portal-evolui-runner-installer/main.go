package main

import (
	"bufio"
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"log"
	"net/url"
	"os"
	"os/signal"
	"runtime"
	"strings"
	"time"

	"portal-evolui-runner-installer/internal/install"
	"portal-evolui-runner-installer/internal/portalrc"
	"portal-evolui-runner-installer/internal/stomp"
	"portal-evolui-runner-installer/internal/wsjson"
)

// Version must satisfy o semver mínimo configurado no portal (GithubConfigDTO.runnerInstallerMinVersion).
const Version = "1.0.0"

func main() {
	tokenHex := flag.String("token", "", "token hexadecimal gerado no portal (se vazio: stdin ou prompt interativo)")
	flag.Parse()

	raw, err := readTokenHex(strings.TrimSpace(*tokenHex))
	if err != nil {
		log.Fatal(err)
	}
	if raw == "" {
		printTokenHowTo(os.Stderr)
		fmt.Fprintln(os.Stderr, "\nNenhum token foi informado.")
		os.Exit(1)
	}

	conn, err := portalrc.DecryptPortalToken(raw)
	if err != nil {
		log.Fatalf("token: %v", err)
	}

	wsBase, err := wsURLFromHost(conn.Host)
	if err != nil {
		log.Fatalf("host: %v", err)
	}

	jwt := strings.TrimSpace(conn.Destination)
	uuid := strings.TrimSpace(conn.UUID)
	if jwt == "" || uuid == "" {
		log.Fatal("token decifrado sem destination (JWT) ou uuid")
	}

	client, err := stomp.Dial(wsBase, jwt, uuid)
	if err != nil {
		log.Fatalf("stomp: %v", err)
	}
	defer client.Close()

	fam := osFamily()
	hi := wsjson.Hello{
		ClientVersion: Version,
		OsFamily:      fam,
		Arch:          runtime.GOARCH,
		Hostname:      hostname(),
	}
	helloBody, err := wsjson.MarshalEnvelope(uuid, uuid, hi)
	if err != nil {
		log.Fatalf("hello json: %v", err)
	}
	if err := client.Send(wsjson.AppHello, helloBody); err != nil {
		log.Fatalf("enviar hello: %v", err)
	}
	log.Printf("conectado; hello enviado (uuid=%s)", uuid)

	h := func(topic string) func(m stomp.Message) {
		return func(m stomp.Message) {
			handleTopic(client, topic, m.Body, uuid)
		}
	}

	_, _ = client.Subscribe(wsjson.QueueBlocked+uuid, h("blocked"))
	_, _ = client.Subscribe(wsjson.QueueMachineInfoRequest+uuid, h("machine-info"))
	_, _ = client.Subscribe(wsjson.QueueWorkdirCheckRequest+uuid, h("workdir"))
	_, _ = client.Subscribe(wsjson.QueueInstallConfig+uuid, h("install-config"))
	_, _ = client.Subscribe(wsjson.QueueRoutingFailure+uuid, h("routing-failure"))

	sig := make(chan os.Signal, 1)
	signal.Notify(sig, os.Interrupt)
	<-sig
	log.Println("encerrando")
}

// printTokenHowTo explica onde obter o token no portal (stderr, sem prefixo de log).
func printTokenHowTo(w io.Writer) {
	fmt.Fprintln(w, "Portal Evolui — conexão do instalador de runner")
	fmt.Fprintln(w)
	fmt.Fprintln(w, "Você precisa do token hexadecimal mostrado no portal ao instalar um runner self-hosted.")
	fmt.Fprintln(w)
	fmt.Fprintln(w, "Como obter:")
	fmt.Fprintln(w, "  1) Acesse o portal com um usuário SUPER ou HYPER.")
	fmt.Fprintln(w, "  2) Abra a área de administração do GitHub / runners e inicie o assistente")
	fmt.Fprintln(w, "     de instalação do runner (modal com os passos e o token de conexão).")
	fmt.Fprintln(w, "  3) Na etapa do token, gere ou copie o valor exibido (só caracteres hex).")
	fmt.Fprintln(w)
	fmt.Fprintln(w, "Como informar neste programa:")
	fmt.Fprintln(w, "  • Cole abaixo e pressione Enter, ou")
	fmt.Fprintln(w, "  • Passe na linha de comando:  -token \"<hex>\", ou")
	fmt.Fprintln(w, "  • Redirecione stdin:  echo <hex> | este-programa")
}

// readTokenHex usa -token; se vazio, mostra orientação e lê uma linha de stdin (ou pipe).
func readTokenHex(fromFlag string) (string, error) {
	if s := strings.TrimSpace(fromFlag); s != "" {
		return s, nil
	}
	printTokenHowTo(os.Stderr)
	fmt.Fprint(os.Stderr, "\nCole o token hexadecimal e pressione Enter: ")
	br := bufio.NewReader(os.Stdin)
	line, err := br.ReadString('\n')
	s := strings.TrimSpace(line)
	if err != nil && err != io.EOF {
		return "", fmt.Errorf("ler token: %w", err)
	}
	return s, nil
}

func wsURLFromHost(host string) (string, error) {
	host = strings.TrimSpace(host)
	if host == "" {
		return "", fmt.Errorf("host vazio")
	}
	u, err := url.Parse(host)
	if err != nil {
		return "", err
	}
	switch u.Scheme {
	case "http":
		u.Scheme = "ws"
	case "https":
		u.Scheme = "wss"
	default:
		return "", fmt.Errorf("host deve começar com http:// ou https:// (recebido: %s)", u.Scheme)
	}
	basePath := strings.TrimSuffix(u.Path, "/")
	if basePath == "" {
		u.Path = "/portalEvoluiWebSocket"
	} else {
		u.Path = basePath + "/portalEvoluiWebSocket"
	}
	u.RawQuery = ""
	u.Fragment = ""
	return u.String(), nil
}

func osFamily() string {
	if runtime.GOOS == "windows" {
		return "windows"
	}
	return "linux"
}

func hostname() string {
	h, err := os.Hostname()
	if err != nil {
		return "unknown"
	}
	return h
}

func handleTopic(c *stomp.Client, topic string, body []byte, uuid string) {
	var env wsjson.Envelope
	if err := json.Unmarshal(body, &env); err != nil {
		log.Printf("[%s] json inválido: %v", topic, err)
		return
	}
	switch topic {
	case "blocked":
		var b wsjson.Blocked
		_ = json.Unmarshal(env.Message, &b)
		log.Printf("bloqueado pelo portal: %s (mínimo=%s, sua=%s)", b.Reason, b.MinVersionRequired, b.ClientVersion)
	case "routing-failure":
		log.Printf("falha de roteamento: %s", env.Error)
	case "machine-info":
		resp := wsjson.MachineInfoResponse{
			OsFamily:                 osFamily(),
			Arch:                     runtime.GOARCH,
			Hostname:                 hostname(),
			MeetsMinimumRequirements: true,
			RequirementsDetail:       "ok",
		}
		send(c, wsjson.AppMachineInfoResponse, uuid, uuid, resp)
	case "workdir":
		var req wsjson.WorkdirRequest
		if err := json.Unmarshal(env.Message, &req); err != nil {
			log.Printf("workdir request: %v", err)
			return
		}
		out := install.CheckInstallAndWorkDirs(req.RunnerInstallFolder, req.WorkFolder)
		send(c, wsjson.AppWorkdirCheckResponse, uuid, uuid, out)
	case "install-config":
		var cfg wsjson.InstallConfig
		if err := json.Unmarshal(env.Message, &cfg); err != nil {
			log.Printf("install-config: %v", err)
			return
		}
		ctx, cancel := context.WithTimeout(context.Background(), 90*time.Minute)
		defer cancel()
		err := install.Do(ctx, cfg)
		if err != nil {
			log.Printf("instalação falhou: %v", err)
			send(c, wsjson.AppInstallResult, uuid, uuid, wsjson.InstallResult{
				Success: false,
				Message: "Falha na instalação",
				Detail:  err.Error(),
			})
			_ = c.Close()
			os.Exit(1)
		}
		send(c, wsjson.AppInstallResult, uuid, uuid, wsjson.InstallResult{
			Success: true,
			Message: "Runner configurado com sucesso. O instalador neste terminal encerra; para outro runner, gere nova sessão no portal.",
		})
		_ = c.Close()
		os.Exit(0)
	default:
		log.Printf("tópico desconhecido: %s", topic)
	}
}

func send(c *stomp.Client, dest, from, to string, msg interface{}) {
	b, err := wsjson.MarshalEnvelope(from, to, msg)
	if err != nil {
		log.Printf("marshal resposta: %v", err)
		return
	}
	if err := c.Send(dest, b); err != nil {
		log.Printf("enviar %s: %v", dest, err)
	}
}
