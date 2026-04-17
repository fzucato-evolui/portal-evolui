package config

import (
	"flag"
	"fmt"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"portal-evolui-monitor/internal/urlutil"
)

// Config agrega flags e variáveis de ambiente (README).
type Config struct {
	BaseURL         string
	WSURL           string
	Login           string
	Password        string
	JWT             string
	Identifier      string
	HealthCheckerID *int64
	MinBackoff      time.Duration
	MaxBackoff      time.Duration
	SkipTLSVerify   bool
	HTTPTimeout     time.Duration
	// PairingJWT é o JWT do operador no portal (campo destination do token init); usado em HEY e roteamento.
	PairingJWT string
	// SendHeyOnConnect envia /app/hey após assinar (fluxo init, paridade MainController.init).
	SendHeyOnConnect bool
	// Init ativa modo token colado do modal (decrypt + preenchimento automático).
	Init          bool
	InitToken     string
	InitTokenFile string
	// ConfigFile caminho do configuration.hck (encrypt) para modo run ou destino após save (UtilController).
	ConfigFile string
	// VerifyConfigPath, se definido, faz só validação local (decrypt + JSON) e sai — não conecta ao portal.
	VerifyConfigPath string
	// DumpSystemInfoPath, se definido, grava uma leitura JSON (HealthCheckerSystemInfoDTO) e sai — para comparar com o JAR/OSHI.
	DumpSystemInfoPath string
	// AllowNonElevated permite executar sem administrador (Windows) ou root (Linux); só desenvolvimento — o JAR exige privilégios elevados.
	AllowNonElevated bool
	// Verbose liga logs operacionais (STOMP, handlers); erros críticos continuam em log padrão.
	Verbose bool
	// SnapshotJSON cópia do JSON HealthCheckerConfigDTO (após save ou -config) para avaliação de módulos/alertas e POST /check.
	SnapshotJSON []byte
	// HealthCheckIntervalMinutes intervalo do monitoramento REST (minutos); padrão 2 se não vier na config.
	HealthCheckIntervalMinutes int
}

// EffectiveConfigPath retorna o path usado ao gravar; default ./configuration.hck.
func (c *Config) EffectiveConfigPath() string {
	if c != nil && c.ConfigFile != "" {
		return c.ConfigFile
	}
	return "configuration.hck"
}

func env(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}

func envBool(key string) bool {
	v := os.Getenv(key)
	return v == "1" || v == "true" || v == "TRUE" || v == "yes"
}

func envInt64(key string) (*int64, error) {
	s := os.Getenv(key)
	if s == "" {
		return nil, nil
	}
	n, err := strconv.ParseInt(s, 10, 64)
	if err != nil {
		return nil, fmt.Errorf("%s: %w", key, err)
	}
	if n <= 0 {
		return nil, nil
	}
	return &n, nil
}

// envVerboseDefault: sem EVOLUI_MONITOR_VERBOSE → false (silente). 1/true/yes/on → true; 0/false/no/off → false.
func envVerboseDefault() bool {
	s := strings.TrimSpace(strings.ToLower(os.Getenv("EVOLUI_MONITOR_VERBOSE")))
	if s == "" {
		return false
	}
	if s == "1" || s == "true" || s == "yes" || s == "on" {
		return true
	}
	return false
}

// resolveDefaultConfigFile tenta configuration.hck ao lado do executável, depois no cwd.
func resolveDefaultConfigFile() string {
	name := "configuration.hck"
	if exe, err := os.Executable(); err == nil {
		exe = filepath.Clean(exe)
		next := filepath.Join(filepath.Dir(exe), name)
		if st, err := os.Stat(next); err == nil && !st.IsDir() {
			return next
		}
	}
	if cwd, err := os.Getwd(); err == nil {
		next := filepath.Join(cwd, name)
		if st, err := os.Stat(next); err == nil && !st.IsDir() {
			return next
		}
	}
	return ""
}

// Parse lê flags e complementa com env quando o flag está vazio.
func Parse(args []string) (*Config, error) {
	fs := flag.NewFlagSet("portal-evolui-monitor", flag.ContinueOnError)
	var (
		baseURL          = fs.String("base-url", env("EVOLUI_MONITOR_BASE_URL", ""), "URL HTTP ou HTTPS raiz do portal (ex.: http://host/app ou https://host/app)")
		wsURL            = fs.String("ws-url", env("EVOLUI_MONITOR_WS_URL", ""), "URL WebSocket STOMP ws:// ou wss:// (opcional; default derivado de -base-url)")
		login            = fs.String("login", env("EVOLUI_MONITOR_LOGIN", ""), "usuário ROLE_HEALTHCHECKER")
		password         = fs.String("password", env("EVOLUI_MONITOR_PASSWORD", ""), "senha")
		jwt              = fs.String("jwt", env("EVOLUI_MONITOR_JWT", ""), "JWT fixo (depuração; pula login HTTP)")
		identifier       = fs.String("identifier", env("EVOLUI_MONITOR_IDENTIFIER", ""), "identificador STOMP; se vazio, usa hardware UUID (OSHI)")
		healthCheckerID  = fs.Int64("health-checker-id", 0, "id do cadastro health-checker (extraInfo no login); 0 = omitir")
		minBackoff       = fs.Duration("min-backoff", 1*time.Second, "backoff mínimo entre reconexões")
		maxBackoff       = fs.Duration("max-backoff", 60*time.Second, "backoff máximo entre reconexões")
		verifyTLS        = fs.Bool("verify-tls", envBool("EVOLUI_MONITOR_VERIFY_TLS"), "exigir certificado TLS válido em HTTPS e WSS (padrão: não verificar)")
		httpTimeout      = fs.Duration("http-timeout", 60*time.Second, "timeout do cliente HTTP (login)")
		init             = fs.Bool("init", envBool("EVOLUI_MONITOR_INIT"), "modo configuração: usar token do modal (decrypt GeradorTokenPortalEvolui)")
		initToken        = fs.String("init-token", env("EVOLUI_MONITOR_INIT_TOKEN", ""), "token hexadecimal (alternativa a stdin ou -init-token-file)")
		initTokenFile    = fs.String("init-token-file", env("EVOLUI_MONITOR_INIT_TOKEN_FILE", ""), "arquivo com o token hexadecimal")
		configFile       = fs.String("config", env("EVOLUI_MONITOR_CONFIG", ""), "configuration.hck cifrado (carrega host/login/identifier para run sem flags manuais)")
		verifyConfig     = fs.String("verify-config", "", "valida decrypt+JSON do arquivo; imprime host/identifier/login (senha omitida) e sai")
		dumpSystemInfo   = fs.String("dump-system-info", "", "grava JSON de system info (paridade portal) no caminho e sai; use com -identifier ou UUID automático")
		allowNonElevated = fs.Bool("allow-non-elevated", envBool("EVOLUI_MONITOR_ALLOW_NON_ELEVATED"), "permite executar sem admin/root (apenas dev; padrão: exigir mesmo que o health-checker Java)")
		verbose          = fs.Bool("verbose", envVerboseDefault(), "logs operacionais (STOMP, handlers); padrão: desligado; use -verbose ou EVOLUI_MONITOR_VERBOSE=1")
	)

	if err := fs.Parse(args); err != nil {
		return nil, err
	}

	if strings.TrimSpace(*verifyConfig) != "" {
		if *init {
			return nil, fmt.Errorf("não combine -init com -verify-config")
		}
		return &Config{VerifyConfigPath: strings.TrimSpace(*verifyConfig), Verbose: *verbose}, nil
	}

	configPath := strings.TrimSpace(*configFile)
	if configPath == "" {
		configPath = env("EVOLUI_MONITOR_CONFIG", "")
	}
	if configPath == "" && !*init {
		if p := resolveDefaultConfigFile(); p != "" {
			configPath = p
		}
	}

	cfg := &Config{
		BaseURL:            *baseURL,
		WSURL:              *wsURL,
		Login:              *login,
		Password:           *password,
		JWT:                *jwt,
		Identifier:         *identifier,
		MinBackoff:         *minBackoff,
		MaxBackoff:         *maxBackoff,
		SkipTLSVerify:      !*verifyTLS,
		HTTPTimeout:        *httpTimeout,
		Init:               *init,
		InitToken:          *initToken,
		InitTokenFile:      *initTokenFile,
		ConfigFile:         configPath,
		DumpSystemInfoPath: strings.TrimSpace(*dumpSystemInfo),
		AllowNonElevated:   *allowNonElevated,
		Verbose:            *verbose,
	}

	if *healthCheckerID > 0 {
		id := *healthCheckerID
		cfg.HealthCheckerID = &id
	} else if hid, err := envInt64("EVOLUI_MONITOR_HEALTH_CHECKER_ID"); err != nil {
		return nil, err
	} else if hid != nil {
		cfg.HealthCheckerID = hid
	}

	if cfg.Init && cfg.JWT != "" {
		return nil, fmt.Errorf("não combine -init com -jwt")
	}
	if cfg.Init && cfg.ConfigFile != "" {
		return nil, fmt.Errorf("não combine -init com -config")
	}
	if cfg.DumpSystemInfoPath != "" && cfg.Init {
		return nil, fmt.Errorf("não combine -init com -dump-system-info")
	}

	if !cfg.Init && cfg.DumpSystemInfoPath == "" {
		hasConfigFile := cfg.ConfigFile != ""
		hasJWT := cfg.JWT != ""
		hasPasswordPair := cfg.Login != "" && cfg.Password != ""
		hasURL := cfg.BaseURL != "" || cfg.WSURL != ""

		if hasConfigFile {
			// host/login virão do arquivo em main (ApplyFromHealthCheckerConfigJSON)
		} else {
			if !hasURL {
				return nil, fmt.Errorf("informe -base-url/-ws-url, ou use -config, ou deixe configuration.hck ao lado do executável / na pasta atual")
			}
			if !hasJWT && !hasPasswordPair {
				return nil, fmt.Errorf("informe -login/-password, ou -jwt, ou -config com arquivo completo")
			}
			if !hasJWT && cfg.BaseURL == "" {
				return nil, fmt.Errorf("login HTTP requer -base-url (URL do portal)")
			}
		}
	}
	// modo -init: base-url/login vêm do token depois (main chama initflow.ApplyEncryptedToken)

	if cfg.MaxBackoff < cfg.MinBackoff {
		cfg.MaxBackoff = cfg.MinBackoff
	}
	if cfg.BaseURL != "" {
		if err := urlutil.ValidateHTTPBaseURL(cfg.BaseURL); err != nil {
			return nil, err
		}
	}
	if cfg.WSURL != "" {
		if err := urlutil.ValidateWebSocketURL(cfg.WSURL); err != nil {
			return nil, err
		}
	}
	return cfg, nil
}
