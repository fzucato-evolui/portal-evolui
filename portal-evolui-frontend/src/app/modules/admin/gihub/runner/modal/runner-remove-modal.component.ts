import {ChangeDetectorRef, Component, Inject, ViewEncapsulation} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {RunnerGithubModel} from '../../../../../shared/models/github.model';
import {RunnerService} from '../runner.service';
import {MessageDialogService} from '../../../../../shared/services/message/message-dialog-service';
import {SplashScreenService} from '../../../../../shared/services/splash/splash-screen.service';
import {GithubRegistrationTokenResponse} from '../../../../../shared/models/runner-installer.model';

export interface RunnerRemoveModalData {
  runner: RunnerGithubModel;
}

@Component({
  selector: 'runner-remove-modal',
  templateUrl: './runner-remove-modal.component.html',
  styleUrls: ['./runner-remove-modal.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: false
})
export class RunnerRemoveModalComponent {

  mode: 'org' | 'local' = 'org';
  removeToken: string | null = null;
  tokenExpiresAt: string | null = null;
  loadingToken = false;

  constructor(
    public dialogRef: MatDialogRef<RunnerRemoveModalComponent, 'refresh' | void>,
    @Inject(MAT_DIALOG_DATA) public data: RunnerRemoveModalData,
    private _runnerService: RunnerService,
    private _message: MessageDialogService,
    private _progress: SplashScreenService,
    private _cdr: ChangeDetectorRef
  ) {
  }

  get runner(): RunnerGithubModel {
    return this.data?.runner;
  }

  /** Script Linux: serviço systemd primeiro (evita «Uninstall service first»), depois remove. */
  get linuxRemoveScriptBlock(): string {
    if (!this.removeToken) {
      return '';
    }
    const q = this.removeToken.replace(/'/g, `'\\''`);
    return (
      `# Se instalou como serviço (./svc.sh install), pare e desinstale o serviço antes do config.sh remove:\n` +
      `# (Se não usou serviço, estas duas linhas podem falhar — pode ignorar.)\n` +
      `sudo ./svc.sh stop\n` +
      `sudo ./svc.sh uninstall\n` +
      `\n` +
      `# Registo no GitHub (não execute config.sh com sudo na sua shell; use sudo -u utilizador-dono se for root):\n` +
      `./config.sh remove --token '${q}'\n`
    );
  }

  /**
   * PowerShell: tenta parar e remover o serviço Windows a partir de `.service` (como o pacote oficial),
   * depois executa `config.cmd remove`. Cole na pasta do runner — sem linha `cd`.
   */
  get windowsRemoveScriptBlock(): string {
    if (!this.removeToken) {
      return '';
    }
    const dq = this.removeToken.replace(/"/g, '\\"');
    return (
      `# Na pasta do runner (PowerShell; administrador se precisar parar o serviço)\n` +
      `if (Test-Path .\\.service) {\n` +
      `  $svc = (Get-Content -Raw .\\.service).Trim()\n` +
      `  if ($svc) {\n` +
      `    Stop-Service -Name $svc -Force -ErrorAction SilentlyContinue\n` +
      `    sc.exe stop $svc | Out-Null\n` +
      `    sc.exe delete $svc | Out-Null\n` +
      `  }\n` +
      `}\n` +
      `.\\config.cmd remove --token "${dq}"\n`
    );
  }

  copyLinuxRemoveCommands(): void {
    const text = this.linuxRemoveScriptBlock;
    if (!text) {
      return;
    }
    this.copyTextToClipboard(text, 'Comandos copiados. Execute na pasta do runner (bash).');
  }

  copyWindowsRemoveCommands(): void {
    const text = this.windowsRemoveScriptBlock;
    if (!text) {
      return;
    }
    this.copyTextToClipboard(text, 'Comandos copiados. Execute na pasta do runner (PowerShell).');
  }

  private copyTextToClipboard(text: string, successMsg: string): void {
    const done = (): void => {
      this._message.open(successMsg, 'Sucesso', 'success');
      this._cdr.markForCheck();
    };
    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(text).then(done).catch(() => this.copyTextFallback(text, done));
    } else {
      this.copyTextFallback(text, done);
    }
  }

  private copyTextFallback(text: string, onDone: () => void): void {
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
        this._message.open('Selecione o texto e copie manualmente (Ctrl+C).', 'Aviso', 'warning');
      }
    } finally {
      document.body.removeChild(ta);
    }
  }

  removeFromOrganization(): void {
    const msg =
      'O runner será removido apenas da organização no GitHub, e não da máquina onde está instalado. ' +
      'Após a remoção, o agente na máquina não conseguirá mais se conectar a esta organização. ' +
      'Deseja continuar?';
    this._message.open(msg, 'Confirmar remoção', 'confirm').subscribe(result => {
      if (result !== 'confirmed') {
        return;
      }
      this._progress.show();
      this._runnerService.deleteRunner(this.runner.id)
        .then(() => {
          this._message.open('Runner removido da organização no GitHub.', 'Sucesso', 'success');
          this.dialogRef.close('refresh');
        })
        .catch(err => {
          console.error(err);
          const m = err?.error?.message || err?.message || 'Falha ao remover o runner na API do GitHub.';
          this._message.open(m, 'Erro', 'error');
          this._cdr.markForCheck();
        })
        .finally(() => this._progress.hide());
    });
  }

  fetchRemoveToken(): void {
    this.loadingToken = true;
    this.removeToken = null;
    this.tokenExpiresAt = null;
    this._cdr.markForCheck();
    this._runnerService.getRemoveToken()
      .then((res: GithubRegistrationTokenResponse) => {
        this.removeToken = res?.token || null;
        this.tokenExpiresAt = res?.expiresAt || res?.expires_at || null;
        if (!this.removeToken) {
          this._message.open('Resposta sem token.', 'Erro', 'error');
        }
      })
      .catch(err => {
        console.error(err);
        this._message.open(
          err?.error?.message || 'Não foi possível obter o token de remoção.',
          'Erro',
          'error'
        );
      })
      .finally(() => {
        this.loadingToken = false;
        this._cdr.markForCheck();
      });
  }

  copyToken(): void {
    if (!this.removeToken) {
      return;
    }
    const done = (): void => {
      this._message.open('Token copiado.', 'Sucesso', 'success');
    };
    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(this.removeToken).then(done).catch(() => this.copyFallback(done));
    } else {
      this.copyFallback(done);
    }
  }

  private copyFallback(onDone: () => void): void {
    const ta = document.createElement('textarea');
    ta.value = this.removeToken;
    ta.style.position = 'fixed';
    ta.style.left = '-9999px';
    document.body.appendChild(ta);
    ta.select();
    try {
      if (document.execCommand('copy')) {
        onDone();
      } else {
        this._message.open('Selecione o token e copie manualmente (Ctrl+C).', 'Aviso', 'warning');
      }
    } finally {
      document.body.removeChild(ta);
    }
  }
}
