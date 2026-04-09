import {ChangeDetectorRef, Component, OnDestroy, OnInit, ViewChild, ViewEncapsulation} from '@angular/core';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {MatDialogRef} from '@angular/material/dialog';
import {MatStepper} from '@angular/material/stepper';
import {RxStomp, RxStompConfig, RxStompState} from '@stomp/rx-stomp';
import {Subject, Subscription} from 'rxjs';
import {take, takeUntil, throttleTime} from 'rxjs/operators';
import {RunnerService} from '../runner.service';
import {UserService} from '../../../../../shared/services/user/user.service';
import {MessageDialogService} from '../../../../../shared/services/message/message-dialog-service';
import {SplashScreenService} from '../../../../../shared/services/splash/splash-screen.service';
import {ConfigSystemService} from '../../../config-system/config-system.service';
import {
  GithubConfigModel,
  SystemConfigModel,
  SystemConfigModelEnum
} from '../../../../../shared/models/system-config.model';
import {HealthCheckerMessageTopicConstants} from '../../../../../shared/models/health-checker.model';
import {
  ActionsRunnerLatestResponse,
  RunnerInstallConfigModel,
  RunnerInstallerBlockedModel,
  RunnerInstallerHelloModel,
  RunnerInstallerMessageTopicConstants,
  RunnerInstallMachineInfoResponseModel,
  RunnerInstallResultModel,
  RunnerInstallWorkdirCheckRequestModel
} from '../../../../../shared/models/runner-installer.model';
import {WebsocketMessageModel} from '../../../../../shared/models/websocket-message.model';
import {UtilFunctions} from '../../../../../shared/util/util-functions';

@Component({
  selector: 'runner-installer-modal',
  templateUrl: './runner-installer-modal.component.html',
  styleUrls: ['./runner-installer-modal.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: false
})
export class RunnerInstallerModalComponent implements OnInit, OnDestroy {

  @ViewChild('stepper') stepper: MatStepper;

  githubConfig: GithubConfigModel;
  sessionUuid: string;
  encryptedToken: string;
  endpoint: string;

  rxStomp: RxStomp;
  myRxStompConfig: RxStompConfig;
  private _destroy$ = new Subject<void>();
  private _subs: Subscription[] = [];

  connectionStatus = 'Desconectado';
  lastStompState: RxStompState = RxStompState.CLOSED;

  helloReceived = false;
  helloPayload: RunnerInstallerHelloModel;
  machineInfo: RunnerInstallMachineInfoResponseModel;
  actionsRunnerUrl: string;
  actionsRunnerVersion: string;

  installForm: FormGroup;
  nameAvailable: boolean | null = null;
  workDirOk: boolean | null = null;

  installResult: RunnerInstallResultModel;
  installing = false;

  /** assisted = instalador Go + STOMP; manual = pacote oficial e comandos como no GitHub. */
  installMode: 'assisted' | 'manual' = 'assisted';
  manualOs: 'windows' | 'linux' = 'windows';
  manualPackage: ActionsRunnerLatestResponse | null = null;
  manualPackageLoading = false;
  manualRegToken: string | null = null;
  manualRegTokenLoading = false;
  manualRunnerName = '';
  manualRunnerLabels = '';
  manualRunnerGroup = '';
  manualWorkFolder = '_work';
  /** Linux: svc.sh; Windows: --runasservice no config.cmd */
  manualInstallAsService = true;
  /** Modo manual: alinhado ao assistido quando há serviço. */
  manualServiceStartAtBoot = true;
  manualWindowsServiceUser = '';
  manualWindowsServicePassword = '';

  get wsConnected(): boolean {
    return this.lastStompState === RxStompState.OPEN;
  }

  constructor(
    public dialogRef: MatDialogRef<RunnerInstallerModalComponent>,
    private _runnerService: RunnerService,
    private _userService: UserService,
    private _configService: ConfigSystemService,
    private _fb: FormBuilder,
    private _cdr: ChangeDetectorRef,
    private _progress: SplashScreenService,
    private _message: MessageDialogService
  ) {
    this.myRxStompConfig = {
      brokerURL: '',
      connectHeaders: {},
      debug: (): void => undefined
    };
  }

  ngOnInit(): void {
    this.installForm = this._fb.group({
      runnerGroup: [''],
      runnerName: ['', Validators.required],
      runnerAlias: [''],
      runnerInstallFolder: ['', Validators.required],
      workFolder: [''],
      installAsService: [true],
      serviceStartAtBoot: [true],
      serviceAccountUser: [''],
      serviceAccountPassword: ['']
    });

    this._configService.get().pipe(take(1), takeUntil(this._destroy$)).subscribe({
      next: (configs: SystemConfigModel[]) => {
        const row = configs.find(c => c.configType === SystemConfigModelEnum.GITHUB);
        this.githubConfig = row?.config as GithubConfigModel;
        this._cdr.markForCheck();
      },
      error: () => undefined
    });

    this.dialogRef.beforeClosed()
      .pipe(takeUntil(this._destroy$))
      .subscribe(() => this.teardownWs());
  }

  ngOnDestroy(): void {
    this.teardownWs();
    this._destroy$.next();
    this._destroy$.complete();
  }

  get minVersionHint(): string {
    const v = this.githubConfig?.runnerInstallerMinVersion;
    return UtilFunctions.isValidStringOrArray(v) ? `>= ${v}` : '';
  }

  get publicInstallerUrl(): string {
    return this.githubConfig?.runnerInstallerDownloadUrl;
  }

  /** Nome do .zip (último segmento da URL ou padrão do portal). */
  get publicInstallerZipFileName(): string {
    const u = this.publicInstallerUrl?.trim();
    if (!u) {
      return 'runner-installer-client.zip';
    }
    try {
      const path = new URL(u).pathname;
      const seg = path.split('/').filter(Boolean).pop();
      if (seg && seg.includes('.')) {
        return seg;
      }
    } catch {
      /* URL inválida — usa padrão */
    }
    return 'runner-installer-client.zip';
  }

  private bashDoubleQuote(s: string): string {
    return s.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\$/g, '\\$').replace(/`/g, '\\`');
  }

  private psSingleQuoted(s: string): string {
    return s.replace(/'/g, "''");
  }

  /** Comandos bash: download, unzip, permissão e execução (binário amd64). */
  get publicInstallerCommandsLinux(): string {
    const url = this.publicInstallerUrl?.trim();
    if (!url) {
      return '';
    }
    const zip = this.publicInstallerZipFileName;
    const qUrl = this.bashDoubleQuote(url);
    const qZip = this.bashDoubleQuote(zip);
    return (
      `# Na pasta onde quer extrair o instalador\n` +
      `wget -O "${qZip}" "${qUrl}"\n` +
      `unzip -o "${qZip}"\n` +
      `chmod 777 portal-evolui-runner-installer-linux-amd64\n` +
      `./portal-evolui-runner-installer-linux-amd64`
    );
  }

  /** PowerShell: download, Expand-Archive e execução do .exe amd64. */
  get publicInstallerCommandsWindows(): string {
    const url = this.publicInstallerUrl?.trim();
    if (!url) {
      return '';
    }
    const zip = this.publicInstallerZipFileName;
    const u = this.psSingleQuoted(url);
    const z = this.psSingleQuoted(zip);
    return (
      `# Na pasta onde quer extrair o instalador (PowerShell)\n` +
      `Invoke-WebRequest -Uri '${u}' -OutFile '${z}'\n` +
      `Expand-Archive -LiteralPath '${z}' -DestinationPath '.' -Force\n` +
      `.\\portal-evolui-runner-installer-windows-amd64.exe`
    );
  }

  copyPublicInstallerCommands(os: 'linux' | 'windows'): void {
    const text = os === 'linux' ? this.publicInstallerCommandsLinux : this.publicInstallerCommandsWindows;
    if (!UtilFunctions.isValidStringOrArray(text)) {
      return;
    }
    this.copyManualScript(text);
  }

  get githubOrgUrl(): string {
    const owner = this.githubConfig?.owner?.trim();
    return owner ? `https://github.com/${owner}` : '';
  }

  get actionsRunnerReleasePage(): string {
    const v = this.manualPackage?.version?.trim();
    if (!v) {
      return 'https://github.com/actions/runner/releases/latest';
    }
    const tag = v.startsWith('v') || v.startsWith('V') ? v : `v${v}`;
    return `https://github.com/actions/runner/releases/tag/${tag}`;
  }

  onInstallModeChange(): void {
    if (this.installMode === 'manual' && this.githubOrgUrl && !this.manualPackage && !this.manualPackageLoading) {
      this.refreshManualPackage();
    }
  }

  onManualOsChange(): void {
    this.manualPackage = null;
    if (this.installMode === 'manual' && this.githubOrgUrl) {
      this.refreshManualPackage();
    }
  }

  refreshManualPackage(): void {
    if (!this.githubOrgUrl) {
      this._message.open('Configure o owner da organização GitHub nas definições do portal.', 'GitHub', 'warning');
      return;
    }
    this.manualPackageLoading = true;
    const osParam = this.manualOs === 'windows' ? 'windows' : 'linux';
    this._runnerService.getActionsRunnerLatest(osParam)
      .then(pkg => {
        this.manualPackage = pkg;
        this._cdr.markForCheck();
      })
      .catch(err => {
        console.error(err);
        this.manualPackage = null;
        this._message.open('Não foi possível obter o pacote oficial do actions-runner.', 'Erro', 'error');
        this._cdr.markForCheck();
      })
      .finally(() => {
        this.manualPackageLoading = false;
        this._cdr.markForCheck();
      });
  }

  fetchManualRegistrationToken(): void {
    this.manualRegTokenLoading = true;
    this._runnerService.getRegistrationToken()
      .then(r => {
        this.manualRegToken = (r?.token || '').trim() || null;
        if (!this.manualRegToken) {
          this._message.open('Resposta sem token. Tente novamente.', 'Token', 'warning');
        } else {
          this._message.open('Token obtido. Cole no script ou execute os comandos abaixo logo a seguir (expira em poucos minutos).', 'Token', 'success');
        }
        this._cdr.markForCheck();
      })
      .catch(err => {
        console.error(err);
        this.manualRegToken = null;
        this._message.open('Não foi possível obter o token de registo no GitHub.', 'Erro', 'error');
        this._cdr.markForCheck();
      })
      .finally(() => {
        this.manualRegTokenLoading = false;
        this._cdr.markForCheck();
      });
  }

  copyManualScript(text: string): void {
    const done = (): void => {
      this._message.open('Comandos copiados para a área de transferência.', 'Sucesso', 'success');
      this._cdr.markForCheck();
    };
    if (!UtilFunctions.isValidStringOrArray(text)) {
      return;
    }
    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(text).then(done).catch(() => this.copyManualScriptFallback(text, done));
    } else {
      this.copyManualScriptFallback(text, done);
    }
  }

  private copyManualScriptFallback(text: string, onDone: () => void): void {
    const ta = document.createElement('textarea');
    ta.value = text;
    ta.style.position = 'fixed';
    ta.style.left = '-9999px';
    document.body.appendChild(ta);
    ta.focus();
    ta.select();
    try {
      if (document.execCommand('copy')) {
        onDone();
      } else {
        this._message.open('Selecione o texto e use Ctrl+C.', 'Aviso', 'warning');
      }
    } catch (e) {
      this._message.open('Selecione o texto e use Ctrl+C.', 'Aviso', 'warning');
    } finally {
      document.body.removeChild(ta);
    }
  }

  /** Script completo exibido no painel manual (Windows / PowerShell). */
  get manualWindowsScript(): string {
    if (!this.manualPackage || !this.githubOrgUrl) {
      return '# Aguarde o carregamento do pacote e confirme a organização GitHub nas definições do portal.';
    }
    const url = this.manualPackage.downloadUrl || '';
    const file = this.manualPackage.assetName || 'actions-runner.zip';
    const org = this.githubOrgUrl;
    const name = (this.manualRunnerName || '').trim() || 'NOME-DO-RUNNER';
    const work = (this.manualWorkFolder || '').trim() || '_work';
    const tokenLine = this.manualRegToken
      ? `$regToken = '${this.manualRegToken.replace(/'/g, "''")}'`
      : `$regToken = 'COLE_AQUI_O_TOKEN_OBTIDO_NO_PORTAL'`;
    const labels = (this.manualRunnerLabels || '').trim();
    const group = (this.manualRunnerGroup || '').trim();
    let extra = '';
    if (group) {
      extra += ` \\\r\n  --runnergroup "${group.replace(/"/g, '\\"')}"`;
    }
    if (labels) {
      extra += ` \\\r\n  --labels "${labels.replace(/"/g, '\\"')}"`;
    }
    if (this.manualInstallAsService) {
      extra += ' \\\r\n  --runasservice';
      const u = (this.manualWindowsServiceUser || '').trim();
      if (u) {
        extra += ` \\\r\n  --windowslogonaccount "${u.replace(/"/g, '\\"')}"`;
        const p = this.manualWindowsServicePassword || '';
        if (p) {
          const psEsc = p.replace(/"/g, '\\"').replace(/\$/g, '\u0060$');
          extra += ` \\\r\n  --windowslogonpassword "${psEsc}"`;
        }
      }
    }
    const svcBootFix =
      this.manualInstallAsService && !this.manualServiceStartAtBoot
        ? `\r\n# Sem início automático no boot (o config cria o serviço em modo automático por omissão)\r\n` +
          `$svcName = (Get-Content -Raw .\\.service).Trim()\r\n` +
          `sc.exe config $svcName start= demand\r\n`
        : '';
    const runSection = this.manualInstallAsService
      ? '# O runner fica como serviço Windows após o config. Verifique em services.msc.\r\n# Para iniciar manualmente numa consola (sem serviço), não use --runasservice e execute .\\run.cmd\r\n'
      : '# 6) Executar o runner (consola aberta)\r\n.\\run.cmd\r\n';
    return (
      `# === GitHub Actions Runner — instalação manual (Windows / PowerShell) ===\r\n` +
      `# Organização: ${org}\r\n` +
      `# Pacote: ${file} (versão ${this.manualPackage.version || '?'})\r\n` +
      `# Valide o SHA-256 do ficheiro na página do release (opcional): ${this.actionsRunnerReleasePage}\r\n\r\n` +
      `# 1) Pasta (ajuste se quiser)\r\n` +
      `$installDir = Join-Path $env:USERPROFILE "actions-runner"\r\n` +
      `New-Item -ItemType Directory -Force -Path $installDir | Out-Null\r\n` +
      `Set-Location $installDir\r\n\r\n` +
      `# 2) Download\r\n` +
      `$packageUrl = "${url}"\r\n` +
      `$packageFile = "${file}"\r\n` +
      `Invoke-WebRequest -Uri $packageUrl -OutFile $packageFile\r\n\r\n` +
      `# 3) Extrair\r\n` +
      `Add-Type -AssemblyName System.IO.Compression.FileSystem\r\n` +
      `[System.IO.Compression.ZipFile]::ExtractToDirectory((Join-Path $PWD $packageFile), $PWD)\r\n\r\n` +
      `# 4) Token de registo (botão «Obter token de registro» neste assistente — uso único, expira em minutos)\r\n` +
      `${tokenLine}\r\n\r\n` +
      `# 5) Configurar\r\n` +
      `.\\config.cmd --url "${org}" --token $regToken --name "${name.replace(/"/g, '\\"')}" --work "${work.replace(/"/g, '\\"')}" --unattended${extra}\r\n\r\n` +
      runSection
    );
  }

  /** Script completo — Linux (bash). */
  get manualLinuxScript(): string {
    if (!this.manualPackage || !this.githubOrgUrl) {
      return '# Aguarde o carregamento do pacote e confirme a organização GitHub nas definições do portal.';
    }
    const url = this.manualPackage.downloadUrl || '';
    const file = this.manualPackage.assetName || 'actions-runner-linux-x64.tar.gz';
    const org = this.githubOrgUrl;
    const name = (this.manualRunnerName || '').trim() || 'NOME-DO-RUNNER';
    const work = (this.manualWorkFolder || '').trim() || '_work';
    const tokenLine = this.manualRegToken
      ? `REG_TOKEN='${this.manualRegToken.replace(/'/g, `'\"'\"'`)}'`
      : `REG_TOKEN='COLE_AQUI_O_TOKEN_OBTIDO_NO_PORTAL'`;
    const labels = (this.manualRunnerLabels || '').trim();
    const group = (this.manualRunnerGroup || '').trim();
    let extra = '';
    if (group) {
      extra += ` \\\n  --runnergroup "${group.replace(/"/g, '\\"')}"`;
    }
    if (labels) {
      extra += ` \\\n  --labels "${labels.replace(/"/g, '\\"')}"`;
    }
    const escBashDq = (s: string): string => s.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\$/g, '\\$');
    const urlLit = escBashDq(url);
    const fileLit = escBashDq(file);
    const isTarGz = file.toLowerCase().endsWith('.tar.gz');
    const extract = isTarGz
      ? `tar xzf "\$packageFile"`
      : `unzip -q -o "\$packageFile"`;
    const svcDisableLine =
      this.manualInstallAsService && !this.manualServiceStartAtBoot
        ? `# Sem início automático no boot (svc.sh install faz enable por omissão)\nsudo systemctl disable "$(cat .service)"\n`
        : '';
    const svcBlock = this.manualInstallAsService
      ? `\n# 7) Serviço systemd (execute como root se o serviço for em /etc/systemd)\n./svc.sh install\n${svcDisableLine}./svc.sh start\n`
      : `\n# 7) Executar em primeiro plano (deixe a sessão aberta)\n./run.sh\n`;
    return (
      `#!/usr/bin/env bash\n` +
      `set -euo pipefail\n` +
      `# === GitHub Actions Runner — instalação manual (Linux) ===\n` +
      `# Organização: ${org}\n` +
      `# Pacote: ${file} (versão ${this.manualPackage.version || '?'})\n` +
      `# Valide o SHA-256 na página do release (opcional): ${this.actionsRunnerReleasePage}\n\n` +
      `INSTALL_DIR="\$HOME/actions-runner"\n` +
      `mkdir -p "\$INSTALL_DIR"\n` +
      `cd "\$INSTALL_DIR"\n\n` +
      `packageUrl="${urlLit}"\n` +
      `packageFile="${fileLit}"\n` +
      `curl -fL "\$packageUrl" -o "\$packageFile"\n\n` +
      `${extract}\n\n` +
      `# Token (botão «Obter token de registro» — uso único)\n` +
      `${tokenLine}\n` +
      `# Se executar como root:\n` +
      `export RUNNER_ALLOW_RUNASROOT=1\n\n` +
      `./config.sh --url "${org}" --token "\$REG_TOKEN" --name "${name.replace(/"/g, '\\"')}" --work "${work.replace(/"/g, '\\"')}" --unattended${extra}\n` +
      svcBlock
    );
  }

  get manualActiveScript(): string {
    return this.manualOs === 'windows' ? this.manualWindowsScript : this.manualLinuxScript;
  }

  copyToken(): void {
    if (!this.encryptedToken) {
      return;
    }
    const done = (): void => {
      this._message.open('Token copiado para a área de transferência.', 'Sucesso', 'success');
      this._cdr.markForCheck();
    };
    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(this.encryptedToken).then(done).catch(() => this.copyTokenFallback(done));
    } else {
      this.copyTokenFallback(done);
    }
  }

  private copyTokenFallback(onDone: () => void): void {
    const ta = document.createElement('textarea');
    ta.value = this.encryptedToken;
    ta.style.position = 'fixed';
    ta.style.left = '-9999px';
    document.body.appendChild(ta);
    ta.focus();
    ta.select();
    try {
      if (document.execCommand('copy')) {
        onDone();
      } else {
        this._message.open('Não foi possível copiar automaticamente. Selecione o token e use Ctrl+C.', 'Aviso', 'warning');
      }
    } catch (e) {
      this._message.open('Não foi possível copiar automaticamente. Selecione o token e use Ctrl+C.', 'Aviso', 'warning');
    } finally {
      document.body.removeChild(ta);
    }
  }

  generateToken(): void {
    this.sessionUuid = typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
    this._progress.show();
    this._runnerService.generateInstallerSessionToken(this.sessionUuid)
      .then(res => {
        this.encryptedToken = res.token;
        this.endpoint = res.endpoint;
        this._cdr.markForCheck();
        /** Subscreve às filas antes do instalador Go enviar hello (mensagem não fica em buffer no broker). */
        this.connectWebSocket({advanceStepper: false});
      })
      .catch(err => {
        console.error(err);
        this._message.open(
          err?.error?.message || 'Não foi possível gerar o token. Verifique se seu perfil é SUPER ou HYPER.',
          'Erro',
          'error'
        );
      })
      .finally(() => this._progress.hide());
  }

  connectWebSocket(options?: { advanceStepper?: boolean }): void {
    if (!this.endpoint || !UtilFunctions.isValidStringOrArray(this._userService.accessToken)
        || !UtilFunctions.isValidStringOrArray(this.sessionUuid)) {
      return;
    }
    const advanceStepperOnOpen = options?.advanceStepper !== false;
    this.teardownWs();
    const url = new URL(this.endpoint.startsWith('http') ? this.endpoint : `https://${this.endpoint}`);
    const wsProto = url.protocol === 'https:' ? 'wss:' : 'ws:';
    this.myRxStompConfig.brokerURL = `${wsProto}//${url.host}/portalEvoluiWebSocket?Authorization=${this._userService.accessToken}`;
    /** Uma sessão por modal — evita misturar mensagens STOMP entre abas do mesmo usuário (JWT seria igual). */
    this.myRxStompConfig.connectHeaders = {Identifier: this.sessionUuid};

    this.rxStomp = new RxStomp();
    this.rxStomp.configure(this.myRxStompConfig);

    this._subs.push(
      this.rxStomp.connectionState$.pipe(takeUntil(this._destroy$)).subscribe((st: RxStompState) => {
        this.lastStompState = st;
        if (st === RxStompState.OPEN) {
          this.connectionStatus = 'Conectado — aguardando o instalador na máquina alvo…';
          this.addStompWatchers();
          this._cdr.markForCheck();
          if (advanceStepperOnOpen) {
            setTimeout(() => {
              if (this.stepper) {
                this.stepper.next();
              }
            }, 0);
          }
        }
        if (st === RxStompState.CLOSED) {
          this.connectionStatus = 'Desconectado';
          this._cdr.markForCheck();
        }
      })
    );

    this._subs.push(
      this.rxStomp.webSocketErrors$.pipe(
        /** RxStomp retenta ~a cada poucos segundos; sem throttle dispara dezenas de toasts iguais. */
        throttleTime(12000, undefined, {leading: true, trailing: false}),
        takeUntil(this._destroy$)
      ).subscribe(() => {
        this._message.open('Falha no WebSocket. Verifique o backend e a rede.', 'Erro', 'error');
        this._cdr.markForCheck();
      })
    );

    this.rxStomp.activate();
  }

  private stompWatchersAdded = false;

  private addStompWatchers(): void {
    if (this.stompWatchersAdded) {
      return;
    }
    this.stompWatchersAdded = true;
    const stompDest = this.sessionUuid;

    this._subs.push(
      this.rxStomp.watch({destination: `/topic/${HealthCheckerMessageTopicConstants.CLIENT_DISCONECTION}`})
        .pipe(takeUntil(this._destroy$))
        .subscribe(msg => {
          try {
            const m = JSON.parse(msg.body);
            if (m?.client === this.sessionUuid) {
              if (this.installing && !this.installResult) {
                this.onInstallerDisconnectedBeforeResult();
              } else if (!this.installResult) {
                this._message.open('O instalador desconectou.', 'Aviso', 'warning');
              }
              this._cdr.markForCheck();
            }
          } catch (e) {
            /* ignore */
          }
        })
    );

    this._subs.push(
      this.rxStomp.watch({destination: `/queue/${RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_BLOCKED}/${stompDest}`})
        .pipe(takeUntil(this._destroy$))
        .subscribe(msg => this.handleBlocked(msg.body))
    );

    this._subs.push(
      this.rxStomp.watch({destination: `/queue/${HealthCheckerMessageTopicConstants.ROUTING_FAILURE}/${stompDest}`})
        .pipe(takeUntil(this._destroy$))
        .subscribe(msg => {
          const m: WebsocketMessageModel = JSON.parse(msg.body);
          this._message.open(m?.error || 'Erro no roteamento STOMP', 'Erro', 'error');
        })
    );

    this._subs.push(
      this.rxStomp.watch({destination: `/queue/${RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_HELLO}/${stompDest}`})
        .pipe(takeUntil(this._destroy$))
        .subscribe(msg => this.handleHello(msg.body))
    );

    this._subs.push(
      this.rxStomp.watch({destination: `/queue/${RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_MACHINE_INFO_RESPONSE}/${stompDest}`})
        .pipe(takeUntil(this._destroy$))
        .subscribe(msg => this.handleMachineInfo(msg.body))
    );

    this._subs.push(
      this.rxStomp.watch({destination: `/queue/${RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_WORKDIR_CHECK_RESPONSE}/${stompDest}`})
        .pipe(takeUntil(this._destroy$))
        .subscribe(msg => this.handleWorkdirResponse(msg.body))
    );

    this._subs.push(
      this.rxStomp.watch({destination: `/queue/${RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_RESULT}/${stompDest}`})
        .pipe(takeUntil(this._destroy$))
        .subscribe(msg => this.handleInstallResult(msg.body))
    );
  }

  private handleBlocked(body: string): void {
    const m: WebsocketMessageModel = JSON.parse(body);
    const data = m.message as RunnerInstallerBlockedModel;
    const lines = [m.error || data?.reason || 'Instalador incompatível'];
    if (data?.minVersionRequired) {
      lines.push(`Versão mínima: >= ${data.minVersionRequired}`);
    }
    if (data?.clientVersion) {
      lines.push(`Versão do client: ${data.clientVersion}`);
    }
    if (UtilFunctions.isValidStringOrArray(data?.installerDownloadUrl)) {
      lines.push(`Download: ${data.installerDownloadUrl}`);
    }
    this._message.open(lines.join('\n'), 'Versão do instalador', 'error');
    this._cdr.markForCheck();
  }

  private handleHello(body: string): void {
    const m: WebsocketMessageModel = JSON.parse(body);
    if (UtilFunctions.isValidStringOrArray(m.error)) {
      this._message.open(m.error, 'Erro', 'error');
      return;
    }
    this.helloPayload = m.message as RunnerInstallerHelloModel;
    this.helloReceived = true;
    this.connectionStatus = 'Cliente conectado — solicitando informações da máquina…';
    this._cdr.markForCheck();
    this.sendStomp(RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_MACHINE_INFO_REQUEST, {});
  }

  private handleMachineInfo(body: string): void {
    const m: WebsocketMessageModel = JSON.parse(body);
    if (UtilFunctions.isValidStringOrArray(m.error)) {
      this._message.open(m.error, 'Erro', 'error');
      return;
    }
    this.machineInfo = m.message as RunnerInstallMachineInfoResponseModel;
    if (this.machineInfo.meetsMinimumRequirements === false) {
      this.connectionStatus = 'Esta máquina não atende os requisitos do runner oficial (ex.: glibc no Linux, versão do Windows). Veja o bloco «Máquina» abaixo.';
      this._message.open(
        this.machineInfo.requirementsDetail || 'Requisitos mínimos não atendidos na máquina alvo.',
        'Compatibilidade',
        'warning'
      );
    }
    const os = this.machineInfo?.osFamily || this.helloPayload?.osFamily || 'linux';
    this._runnerService.getActionsRunnerLatest(os)
      .then(ar => {
        this.actionsRunnerUrl = ar.downloadUrl;
        this.actionsRunnerVersion = ar.version;
        if (this.machineInfo?.meetsMinimumRequirements !== false) {
          this.connectionStatus = 'Pronto para configurar o runner.';
        }
        this._cdr.markForCheck();
      })
      .catch(err => {
        console.error(err);
        this._message.open('Não foi possível obter o pacote oficial do actions-runner.', 'Erro', 'error');
        this._cdr.markForCheck();
      });
  }

  private handleWorkdirResponse(body: string): void {
    const m: WebsocketMessageModel = JSON.parse(body);
    if (UtilFunctions.isValidStringOrArray(m.error)) {
      this.workDirOk = false;
      this._message.open(m.error, 'Pastas', 'error');
      this._cdr.markForCheck();
      return;
    }
    const r = m.message as { exists?: boolean; writable?: boolean; detail?: string };
    this.workDirOk = !!(r?.exists && r?.writable);
    if (!this.workDirOk) {
      this._message.open(r?.detail || 'Pastas inválidas ou sem permissão de escrita.', 'Pastas', 'warning');
    }
    this._cdr.markForCheck();
  }

  /**
   * O client Go fecha o WebSocket logo após o sucesso local; se o STOMP não chegar a tempo,
   * só vemos CLIENT_DISCONECTION — evita spinner infinito e explica o caso «runner já online».
   */
  private onInstallerDisconnectedBeforeResult(): void {
    if (!this.installing || this.installResult) {
      return;
    }
    this.installing = false;
    this._progress.hide();
    this.installResult = {
      success: false,
      inconclusive: true,
      message: 'O instalador encerrou a ligação antes do portal receber a confirmação final.',
      detail:
        'Se o runner já aparece como online na organização GitHub, a instalação concluiu na mesma. ' +
        'Caso contrário, abra uma nova sessão no assistente ou use o modo manual.'
    };
    setTimeout(() => this.selectResultStep(), 0);
  }

  private handleInstallResult(body: string): void {
    this.installing = false;
    const m: WebsocketMessageModel = JSON.parse(body);
    this.installResult = m.message as RunnerInstallResultModel;
    if (!this.installResult) {
      this.installResult = {success: false, message: m.error || 'Resposta inválida', detail: ''};
    }
    this._progress.hide();
    this._cdr.markForCheck();
    setTimeout(() => this.selectResultStep(), 0);
  }

  /** Garante o passo «Resultado» visível (evita corrida se a resposta STOMP for imediata). */
  private selectResultStep(): void {
    const n = this.stepper?.steps?.length;
    if (n > 0) {
      this.stepper.selectedIndex = n - 1;
    }
  }

  sendStomp(topic: string, payload: unknown): void {
    if (!this.rxStomp?.active || !this.sessionUuid) {
      return;
    }
    const model = new WebsocketMessageModel();
    model.from = this.sessionUuid;
    model.to = this.sessionUuid;
    model.message = payload;
    this.rxStomp.publish({
      destination: `/app/${topic}`,
      body: JSON.stringify(model)
    });
  }

  suggestAliasFromName(): void {
    const name = this.installForm.get('runnerName')?.value;
    const alias = this.installForm.get('runnerAlias')?.value;
    if (UtilFunctions.isValidStringOrArray(name) && !UtilFunctions.isValidStringOrArray(alias)) {
      this.installForm.patchValue({runnerAlias: name});
    }
  }

  checkRunnerName(): void {
    const name = this.installForm.get('runnerName')?.value?.trim();
    if (!name) {
      return;
    }
    this._progress.show();
    this._runnerService.isRunnerNameAvailable(name)
      .then(r => {
        this.nameAvailable = r.available;
        if (!r.available) {
          this._message.open('Já existe um runner com este nome na organização.', 'Nome', 'warning');
        }
        this._cdr.markForCheck();
      })
      .catch(err => {
        console.error(err);
        this.nameAvailable = null;
        this._message.open('Falha ao validar o nome.', 'Erro', 'error');
      })
      .finally(() => this._progress.hide());
  }

  /**
   * Só é possível verificar pasta no servidor remoto depois que o instalador Go está no ar (hello STOMP).
   */
  canCheckWorkdir(): boolean {
    return this.helloReceived && !!this.rxStomp?.active && UtilFunctions.isValidStringOrArray(this.sessionUuid);
  }

  get workdirCheckButtonTooltip(): string {
    if (!this.helloReceived) {
      return 'Aguarde o instalador Go na máquina alvo conectar (dados do cliente aparecem acima). Sem ele, a pasta não pode ser checada remotamente.';
    }
    return 'Confere na máquina alvo a pasta de instalação (e a de trabalho dos jobs, se informada).';
  }

  checkWorkdir(): void {
    const installPath = this.installForm.get('runnerInstallFolder')?.value?.trim();
    if (!installPath) {
      this._message.open('Informe a pasta de instalação do runner na máquina alvo (onde o pacote será extraído).', 'Pasta', 'warning');
      return;
    }
    const workPath = this.installForm.get('workFolder')?.value?.trim() || '';
    if (!this.rxStomp?.active || !UtilFunctions.isValidStringOrArray(this.sessionUuid)) {
      this._message.open('Conecte o WebSocket no passo anterior.', 'Pasta', 'warning');
      return;
    }
    if (!this.helloReceived) {
      this._message.open(
        'O instalador Go ainda não se conectou. A pasta só pode ser verificada na máquina alvo depois que ele estiver em execução e o handshake for concluído (veja o bloco «Cliente» acima).',
        'Aguardando instalador',
        'warning'
      );
      return;
    }
    const req: RunnerInstallWorkdirCheckRequestModel = {
      runnerInstallFolder: installPath,
      workFolder: workPath || undefined
    };
    this.sendStomp(RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_WORKDIR_CHECK_REQUEST, req);
  }

  /** URL passada ao actions/runner (--url); alinhada ao token de registro da API orgs/{owner}. */
  private buildGithubOrganizationUrl(): string {
    const owner = this.githubConfig?.owner?.trim();
    if (!owner) {
      return '';
    }
    return `https://github.com/${owner}`;
  }

  canInstallNow(): boolean {
    if (!this.installForm.valid || !this.helloReceived || !this.machineInfo) {
      return false;
    }
    if (this.machineInfo.meetsMinimumRequirements !== true) {
      return false;
    }
    if (this.nameAvailable !== true) {
      return false;
    }
    if (this.workDirOk !== true) {
      return false;
    }
    if (!UtilFunctions.isValidStringOrArray(this.actionsRunnerUrl)) {
      return false;
    }
    if (!UtilFunctions.isValidStringOrArray(this.githubConfig?.owner?.trim())) {
      return false;
    }
    return !this.installing;
  }

  installNow(): void {
    if (!this.canInstallNow()) {
      return;
    }
    this.installing = true;
    this._progress.show();
    this._runnerService.getRegistrationToken()
      .then(reg => {
        const cfg: RunnerInstallConfigModel = {
          runnerGroup: this.installForm.get('runnerGroup')?.value || '',
          runnerName: this.installForm.get('runnerName')?.value?.trim(),
          runnerAlias: (this.installForm.get('runnerAlias')?.value || this.installForm.get('runnerName')?.value)?.trim(),
          runnerInstallFolder: this.installForm.get('runnerInstallFolder')?.value?.trim(),
          workFolder: this.installForm.get('workFolder')?.value?.trim() || '',
          installAsService: !!this.installForm.get('installAsService')?.value,
          serviceStartAtBoot: !!this.installForm.get('serviceStartAtBoot')?.value,
          serviceAccountUser: this.installForm.get('serviceAccountUser')?.value || '',
          serviceAccountPassword: this.installForm.get('serviceAccountPassword')?.value || '',
          registrationToken: reg.token,
          githubOrganizationUrl: this.buildGithubOrganizationUrl(),
          actionsRunnerDownloadUrl: this.actionsRunnerUrl,
          actionsRunnerVersion: this.actionsRunnerVersion
        };
        this.sendStomp(RunnerInstallerMessageTopicConstants.RUNNER_INSTALL_CONFIG, cfg);
        this._cdr.markForCheck();
        setTimeout(() => this.selectResultStep(), 0);
      })
      .catch(err => {
        console.error(err);
        this.installing = false;
        this._progress.hide();
        this._message.open('Não foi possível obter o token de registro no GitHub.', 'Erro', 'error');
        this._cdr.markForCheck();
      });
  }

  private teardownWs(): void {
    this.stompWatchersAdded = false;
    for (const s of this._subs) {
      try {
        s.unsubscribe();
      } catch (e) {
        /* ignore */
      }
    }
    this._subs = [];
    if (this.rxStomp) {
      try {
        this.rxStomp.deactivate();
      } catch (e) {
        /* ignore */
      }
      this.rxStomp = null;
    }
  }
}
