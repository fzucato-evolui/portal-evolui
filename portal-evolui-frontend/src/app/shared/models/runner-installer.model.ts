/** Espelho de {@code RunnerInstallerMessageTopicConstants} no backend. */
export class RunnerInstallerMessageTopicConstants {
  static readonly RUNNER_INSTALL_BLOCKED = 'runner-install-blocked';
  static readonly RUNNER_INSTALL_HELLO = 'runner-install-hello';
  static readonly RUNNER_INSTALL_MACHINE_INFO_REQUEST = 'runner-install-machine-info-request';
  static readonly RUNNER_INSTALL_MACHINE_INFO_RESPONSE = 'runner-install-machine-info-response';
  static readonly RUNNER_INSTALL_WORKDIR_CHECK_REQUEST = 'runner-install-workdir-check-request';
  static readonly RUNNER_INSTALL_WORKDIR_CHECK_RESPONSE = 'runner-install-workdir-check-response';
  static readonly RUNNER_INSTALL_CONFIG = 'runner-install-config';
  static readonly RUNNER_INSTALL_RESULT = 'runner-install-result';
}

export class RunnerInstallerHelloModel {
  clientVersion: string;
  osFamily: string;
  arch: string;
  hostname: string;
}

export class RunnerInstallerBlockedModel {
  reason: string;
  minVersionRequired: string;
  clientVersion: string;
  installerDownloadUrl: string;
}

export class RunnerInstallMachineInfoResponseModel {
  osFamily: string;
  arch: string;
  hostname: string;
  meetsMinimumRequirements: boolean;
  requirementsDetail: string;
}

export class RunnerInstallWorkdirCheckRequestModel {
  runnerInstallFolder: string;
  /** Opcional; vazio = _work relativo à instalação. */
  workFolder?: string;
}

export class RunnerInstallWorkdirCheckResponseModel {
  exists: boolean;
  writable: boolean;
  detail: string;
}

export class RunnerInstallConfigModel {
  runnerGroup: string;
  runnerName: string;
  runnerAlias: string;
  /** Diretório na máquina alvo: extração do pacote + config.sh/cmd. */
  runnerInstallFolder: string;
  /** --work; vazio = _work (relativo à instalação). */
  workFolder: string;
  installAsService: boolean;
  /** Início automático no boot do SO quando instalado como serviço (omitir = true). */
  serviceStartAtBoot?: boolean;
  serviceAccountUser: string;
  serviceAccountPassword: string;
  registrationToken: string;
  /** URL da org no GitHub para o config do runner (ex.: https://github.com/minha-org); vem do owner da config GitHub do portal. */
  githubOrganizationUrl: string;
  actionsRunnerDownloadUrl: string;
  actionsRunnerVersion: string;
}

export class RunnerInstallResultModel {
  success: boolean;
  message: string;
  detail: string;
  /** Definido só no browser quando o instalador desliga antes de chegar o STOMP de resultado. */
  inconclusive?: boolean;
}

export interface GithubRegistrationTokenResponse {
  token: string;
  expires_at?: string;
  expiresAt?: string;
}

export interface ActionsRunnerLatestResponse {
  version: string;
  downloadUrl: string;
  assetName: string;
}
