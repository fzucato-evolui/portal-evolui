package wsjson

import "encoding/json"

const (
	AppHello                 = "/app/runner-install-hello"
	AppMachineInfoResponse   = "/app/runner-install-machine-info-response"
	AppWorkdirCheckResponse  = "/app/runner-install-workdir-check-response"
	AppInstallResult         = "/app/runner-install-result"
	QueueBlocked             = "/queue/runner-install-blocked/"
	QueueMachineInfoRequest  = "/queue/runner-install-machine-info-request/"
	QueueWorkdirCheckRequest = "/queue/runner-install-workdir-check-request/"
	QueueInstallConfig       = "/queue/runner-install-config/"
	QueueRoutingFailure      = "/queue/routing-failure/"
)

type Envelope struct {
	From    string          `json:"from"`
	To      string          `json:"to"`
	Message json.RawMessage `json:"message"`
	Error   string          `json:"error,omitempty"`
}

type Hello struct {
	ClientVersion string `json:"clientVersion"`
	OsFamily      string `json:"osFamily"`
	Arch          string `json:"arch"`
	Hostname      string `json:"hostname"`
}

type MachineInfoResponse struct {
	OsFamily                 string `json:"osFamily"`
	Arch                     string `json:"arch"`
	Hostname                 string `json:"hostname"`
	MeetsMinimumRequirements bool   `json:"meetsMinimumRequirements"`
	RequirementsDetail       string `json:"requirementsDetail"`
}

type WorkdirRequest struct {
	RunnerInstallFolder string `json:"runnerInstallFolder"`
	WorkFolder          string `json:"workFolder"`
}

type WorkdirResponse struct {
	Exists   bool   `json:"exists"`
	Writable bool   `json:"writable"`
	Detail   string `json:"detail,omitempty"`
}

type InstallConfig struct {
	RunnerGroup              string `json:"runnerGroup"`
	RunnerName               string `json:"runnerName"`
	RunnerAlias              string `json:"runnerAlias"`
	RunnerInstallFolder      string `json:"runnerInstallFolder"`
	WorkFolder               string `json:"workFolder"`
	InstallAsService         bool   `json:"installAsService"`
	ServiceAccountUser       string `json:"serviceAccountUser"`
	ServiceAccountPassword   string `json:"serviceAccountPassword"`
	RegistrationToken        string `json:"registrationToken"`
	GithubOrganizationURL    string `json:"githubOrganizationUrl"`
	ActionsRunnerDownloadURL string `json:"actionsRunnerDownloadUrl"`
	ActionsRunnerVersion     string `json:"actionsRunnerVersion"`
}

type InstallResult struct {
	Success bool   `json:"success"`
	Message string `json:"message"`
	Detail  string `json:"detail,omitempty"`
}

type Blocked struct {
	Reason               string `json:"reason"`
	MinVersionRequired   string `json:"minVersionRequired"`
	ClientVersion        string `json:"clientVersion"`
	InstallerDownloadURL string `json:"installerDownloadUrl"`
}

func MarshalEnvelope(from, to string, msg interface{}) ([]byte, error) {
	var raw json.RawMessage
	if msg != nil {
		b, err := json.Marshal(msg)
		if err != nil {
			return nil, err
		}
		raw = b
	} else {
		raw = []byte("null")
	}
	return json.Marshal(Envelope{From: from, To: to, Message: raw})
}
