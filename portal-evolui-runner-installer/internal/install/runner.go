package install

import (
	"archive/tar"
	"archive/zip"
	"compress/gzip"
	"context"
	"fmt"
	"io"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"time"

	"portal-evolui-runner-installer/internal/wsjson"
)

func Do(ctx context.Context, cfg wsjson.InstallConfig) error {
	if cfg.RegistrationToken == "" || cfg.ActionsRunnerDownloadURL == "" {
		return fmt.Errorf("token de registro ou URL de download ausentes")
	}
	githubURL := strings.TrimSpace(cfg.GithubOrganizationURL)
	if githubURL == "" {
		return fmt.Errorf("githubOrganizationUrl ausente no payload (portal deve enviar a URL da org, ex.: https://github.com/minha-org)")
	}
	installDir := strings.TrimSpace(cfg.RunnerInstallFolder)
	if installDir == "" {
		return fmt.Errorf("runnerInstallFolder vazio (pasta onde extrair e configurar o runner)")
	}
	workArg := strings.TrimSpace(cfg.WorkFolder)
	if workArg == "" {
		workArg = "_work"
	}
	if err := os.MkdirAll(installDir, 0755); err != nil {
		return fmt.Errorf("criar pasta de instalação: %w", err)
	}

	tmp, err := os.CreateTemp("", "actions-runner-*")
	if err != nil {
		return err
	}
	tmpPath := tmp.Name()
	_ = tmp.Close()
	defer os.Remove(tmpPath)

	if err := downloadFile(ctx, cfg.ActionsRunnerDownloadURL, tmpPath); err != nil {
		return fmt.Errorf("download: %w", err)
	}

	if strings.HasSuffix(strings.ToLower(cfg.ActionsRunnerDownloadURL), ".zip") {
		if err := unzip(tmpPath, installDir); err != nil {
			return fmt.Errorf("descompactar zip: %w", err)
		}
	} else {
		if err := untargz(tmpPath, installDir); err != nil {
			return fmt.Errorf("descompactar tar.gz: %w", err)
		}
	}

	runnerRoot, err := findRunnerExtractRoot(installDir)
	if err != nil {
		return err
	}

	configExe := filepath.Join(runnerRoot, "config.cmd")
	configSh := filepath.Join(runnerRoot, "config.sh")
	var args []string
	if runtime.GOOS == "windows" {
		if _, err := os.Stat(configExe); err != nil {
			return fmt.Errorf("config.cmd não encontrado em %s", runnerRoot)
		}
		args = buildConfigArgs(cfg, githubURL, workArg, true)
		cmd := exec.CommandContext(ctx, configExe, args...)
		cmd.Dir = runnerRoot
		out, err := cmd.CombinedOutput()
		if err != nil {
			return fmt.Errorf("config.cmd: %w\n%s", err, string(out))
		}
	} else {
		if _, err := os.Stat(configSh); err != nil {
			return fmt.Errorf("config.sh não encontrado em %s", runnerRoot)
		}
		_ = os.Chmod(configSh, 0755)
		args = buildConfigArgs(cfg, githubURL, workArg, false)
		cmd := exec.CommandContext(ctx, configSh, args...)
		cmd.Dir = runnerRoot
		cmd.Env = append(os.Environ(), "RUNNER_ALLOW_RUNASROOT=1")
		out, err := cmd.CombinedOutput()
		if err != nil {
			return fmt.Errorf("config.sh: %w\n%s", err, string(out))
		}
	}

	// Linux: serviço via svc.sh após o config; é preciso também svc.sh start (install não inicia sozinho).
	// Windows com serviço: config.cmd com --runasservice regista e inicia o serviço (pacote oficial).
	if cfg.InstallAsService && runtime.GOOS != "windows" {
		if err := installLinuxRunnerService(ctx, runnerRoot, cfg); err != nil {
			return fmt.Errorf("runner configurado, mas serviço não instalado: %w (execute ./svc.sh install manualmente, em geral como root)", err)
		}
		if err := startLinuxRunnerService(ctx, runnerRoot); err != nil {
			return fmt.Errorf("serviço instalado, mas não iniciado: %w (tente ./svc.sh start como root)", err)
		}
		return nil
	}
	// Sem serviço: o config só regista o runner; é necessário executar run.cmd / run.sh (segundo plano).
	if !cfg.InstallAsService {
		if err := startRunnerDetached(runnerRoot); err != nil {
			return fmt.Errorf("runner configurado, mas falha ao iniciar em segundo plano: %w", err)
		}
	}
	return nil
}

// findRunnerExtractRoot localiza a pasta que contém config.sh / config.cmd após extrair o pacote oficial.
func findRunnerExtractRoot(work string) (string, error) {
	var configFile string
	if runtime.GOOS == "windows" {
		configFile = "config.cmd"
	} else {
		configFile = "config.sh"
	}
	if _, err := os.Stat(filepath.Join(work, configFile)); err == nil {
		return work, nil
	}
	entries, err := os.ReadDir(work)
	if err != nil {
		return "", fmt.Errorf("ler pasta de instalação: %w", err)
	}
	for _, e := range entries {
		if !e.IsDir() {
			continue
		}
		sub := filepath.Join(work, e.Name())
		if _, err := os.Stat(filepath.Join(sub, configFile)); err == nil {
			return sub, nil
		}
	}
	return "", fmt.Errorf("não foi encontrado %s após extrair o pacote (estrutura do zip/tar inesperada)", configFile)
}

func buildConfigArgs(cfg wsjson.InstallConfig, githubURL, workDir string, win bool) []string {
	name := strings.TrimSpace(cfg.RunnerName)
	labels := strings.TrimSpace(cfg.RunnerAlias)
	if labels == "" {
		labels = name
	}
	group := strings.TrimSpace(cfg.RunnerGroup)
	args := []string{
		"--url", githubURL,
		"--token", cfg.RegistrationToken,
		"--name", name,
		"--work", workDir,
		"--unattended",
	}
	if group != "" {
		args = append(args, "--runnergroup", group)
	}
	if labels != "" {
		args = append(args, "--labels", labels)
	}
	// Windows: serviço faz parte do config interativo/unattended (não há svc.cmd no pacote).
	if win && cfg.InstallAsService {
		args = append(args, "--runasservice")
		if u := strings.TrimSpace(cfg.ServiceAccountUser); u != "" {
			args = append(args, "--windowslogonaccount", u)
			if p := cfg.ServiceAccountPassword; p != "" {
				args = append(args, "--windowslogonpassword", p)
			}
		}
	}
	return args
}

func installLinuxRunnerService(ctx context.Context, runnerRoot string, cfg wsjson.InstallConfig) error {
	svc := filepath.Join(runnerRoot, "svc.sh")
	if _, err := os.Stat(svc); err != nil {
		return err
	}
	_ = os.Chmod(svc, 0755)
	svcArgs := []string{"install"}
	if u := strings.TrimSpace(cfg.ServiceAccountUser); u != "" {
		svcArgs = append(svcArgs, u)
	}
	cmd := exec.CommandContext(ctx, svc, svcArgs...)
	cmd.Dir = runnerRoot
	out, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("%s: %w", string(out), err)
	}
	return nil
}

func startLinuxRunnerService(ctx context.Context, runnerRoot string) error {
	svc := filepath.Join(runnerRoot, "svc.sh")
	if _, err := os.Stat(svc); err != nil {
		return err
	}
	_ = os.Chmod(svc, 0755)
	cmd := exec.CommandContext(ctx, svc, "start")
	cmd.Dir = runnerRoot
	out, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("%s: %w", string(out), err)
	}
	return nil
}

func downloadFile(ctx context.Context, url, dest string) error {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
	if err != nil {
		return err
	}
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("HTTP %d", resp.StatusCode)
	}
	f, err := os.Create(dest)
	if err != nil {
		return err
	}
	defer f.Close()
	_, err = io.Copy(f, resp.Body)
	return err
}

func unzip(src, dest string) error {
	r, err := zip.OpenReader(src)
	if err != nil {
		return err
	}
	defer r.Close()
	for _, f := range r.File {
		if !filepath.IsLocal(f.Name) {
			continue
		}
		p := filepath.Join(dest, f.Name)
		if f.FileInfo().IsDir() {
			_ = os.MkdirAll(p, f.Mode())
			continue
		}
		_ = os.MkdirAll(filepath.Dir(p), 0755)
		rc, err := f.Open()
		if err != nil {
			return err
		}
		out, err := os.OpenFile(p, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, f.Mode())
		if err != nil {
			_ = rc.Close()
			return err
		}
		_, err = io.Copy(out, rc)
		_ = rc.Close()
		_ = out.Close()
		if err != nil {
			return err
		}
	}
	return nil
}

func untargz(src, dest string) error {
	f, err := os.Open(src)
	if err != nil {
		return err
	}
	defer f.Close()
	gz, err := gzip.NewReader(f)
	if err != nil {
		return err
	}
	defer gz.Close()
	tr := tar.NewReader(gz)
	for {
		h, err := tr.Next()
		if err == io.EOF {
			break
		}
		if err != nil {
			return err
		}
		if !filepath.IsLocal(h.Name) {
			continue
		}
		p := filepath.Join(dest, h.Name)
		switch h.Typeflag {
		case tar.TypeDir:
			_ = os.MkdirAll(p, os.FileMode(h.Mode))
		case tar.TypeReg:
			_ = os.MkdirAll(filepath.Dir(p), 0755)
			out, err := os.OpenFile(p, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, os.FileMode(h.Mode))
			if err != nil {
				return err
			}
			if _, err := io.Copy(out, tr); err != nil {
				_ = out.Close()
				return err
			}
			_ = out.Close()
		}
	}
	return nil
}

// CheckInstallAndWorkDirs valida pasta de instalação (cria se preciso) e pasta --work (vazia = _work relativo).
func CheckInstallAndWorkDirs(runnerInstall, work string) wsjson.WorkdirResponse {
	runnerInstall = strings.TrimSpace(runnerInstall)
	work = strings.TrimSpace(work)
	if runnerInstall == "" {
		return wsjson.WorkdirResponse{Exists: false, Writable: false, Detail: "informe a pasta de instalação do runner (runnerInstallFolder)"}
	}
	if err := os.MkdirAll(runnerInstall, 0755); err != nil {
		return wsjson.WorkdirResponse{Exists: false, Writable: false, Detail: "instalação: " + err.Error()}
	}
	in := CheckDirExistsWritable(runnerInstall)
	if !in.Writable {
		return wsjson.WorkdirResponse{Exists: in.Exists, Writable: false, Detail: "instalação: " + in.Detail}
	}
	if work == "" {
		return wsjson.WorkdirResponse{
			Exists: true, Writable: true,
			Detail: "Instalação: OK. Trabalho dos jobs: _work (padrão, relativo à pasta de instalação).",
		}
	}
	var workAbs string
	if filepath.IsAbs(work) {
		workAbs = work
	} else {
		workAbs = filepath.Join(runnerInstall, work)
	}
	if err := os.MkdirAll(workAbs, 0755); err != nil {
		return wsjson.WorkdirResponse{
			Exists: false, Writable: false,
			Detail: "instalação: OK. trabalho: não foi possível criar " + workAbs + ": " + err.Error(),
		}
	}
	w := CheckDirExistsWritable(workAbs)
	if !w.Writable {
		return wsjson.WorkdirResponse{
			Exists: false, Writable: false,
			Detail: "instalação: OK. trabalho (" + workAbs + "): " + w.Detail,
		}
	}
	return wsjson.WorkdirResponse{
		Exists: true, Writable: true,
		Detail: "Instalação: OK. Trabalho dos jobs (" + workAbs + "): OK.",
	}
}

// CheckDirExistsWritable verifica pasta na máquina local.
func CheckDirExistsWritable(path string) wsjson.WorkdirResponse {
	path = strings.TrimSpace(path)
	if path == "" {
		return wsjson.WorkdirResponse{Exists: false, Writable: false, Detail: "caminho vazio"}
	}
	st, err := os.Stat(path)
	if err != nil {
		if os.IsNotExist(err) {
			return wsjson.WorkdirResponse{Exists: false, Writable: false, Detail: "pasta não existe"}
		}
		return wsjson.WorkdirResponse{Exists: false, Writable: false, Detail: err.Error()}
	}
	if !st.IsDir() {
		return wsjson.WorkdirResponse{Exists: false, Writable: false, Detail: "não é uma pasta"}
	}
	test := filepath.Join(path, ".evolui-write-test-"+fmt.Sprintf("%d", time.Now().UnixNano()))
	f, err := os.Create(test)
	if err != nil {
		return wsjson.WorkdirResponse{Exists: true, Writable: false, Detail: "sem permissão de escrita: " + err.Error()}
	}
	_ = f.Close()
	_ = os.Remove(test)
	return wsjson.WorkdirResponse{Exists: true, Writable: true, Detail: "ok"}
}
