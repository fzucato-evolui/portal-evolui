import {UtilFunctions} from '../util/util-functions';

export class SystemConfigModel {
    public id: number;
    public configType: SystemConfigModelEnum;
    public config: AXConfigModel | PortalLuthierConfigModel | GoogleConfigModel | GithubConfigModel | AWSConfigModel | SMTPConfigModel | NotificationConfigModel | MondayConfigModel | CICDConfigModel;
}

export enum SystemConfigModelEnum {
  GOOGLE = 'GOOGLE',
  AWS = 'AWS',
  GITHUB = 'GITHUB',
  SMTP = 'SMTP',

  MONDAY = 'MONDAY',
  NOTIFICATION = 'NOTIFICATION',

  CICD = 'CICD',
  AX = 'AX',
  PORTAL_LUTHIER = 'PORTAL_LUTHIER'
}

export class GoogleServiceAccountModel {
  public type: string;
  public project_id: string;
  public private_key_id: string;
  public private_key: string;
  public client_email: string;
  public client_id: string;
  public auth_uri: string;
  public token_uri: string;
  public auth_provider_x509_cert_url: string;
  public client_x509_cert_url: string;
  public universe_domain: string;

  public static validate(model: GoogleServiceAccountModel): boolean {
    return model.type === "service_account" &&
      UtilFunctions.isValidStringOrArray(model.project_id) &&
      UtilFunctions.isValidStringOrArray(model.private_key_id) &&
      UtilFunctions.isValidStringOrArray(model.private_key) &&
      UtilFunctions.isValidEmail(model.client_email) &&
      UtilFunctions.isValidStringOrArray(model.client_id) &&
      UtilFunctions.isValidURL(model.auth_uri) &&
      UtilFunctions.isValidURL(model.token_uri) &&
      UtilFunctions.isValidURL(model.auth_provider_x509_cert_url) &&
      UtilFunctions.isValidURL(model.client_x509_cert_url);
  }
}

export class GoogleConfigModel {
  public apiKey: string;
  public clientID: string;
  public secretKey: string;
  public serviceAccount: GoogleServiceAccountModel;
}

export class GoogleSpaceModel {
  public name: string;
  public displayName: string;
}

export class MondayBoardModel {
  public id: string;
  public name: string;
}

export class MondayGroupModel {
  public id: string;
  public title: string;
}

export class MondayColumnModel {
  public id: string;
  public text: string;
  public type: string;
  public  title: string;
  public possibleValues:  { [key: string]: string};
}

export class GithubConfigModel {
  public user: string;
  public token: string;
  public owner: string;
  public daysForKeep: number;
  /** URL pública do zip do runner-installer (bucket estático). */
  public runnerInstallerDownloadUrl: string;
  /** Versão mínima do client (semver, ex.: 1.1.0); UI pode exibir como ">= 1.1.0". */
  public runnerInstallerMinVersion: string;
}

export enum AWSInstanceRunnerTypeEnum {
  EC2 = 'EC2',
  WORKSPACE = 'WORKSPACE'
}

export  class AWSRunnerConfigModel {
  public id: string;
  public instanceType: AWSInstanceRunnerTypeEnum;
}

export class AWSAccountConfigModel {
  public enabled: boolean;
  public main: boolean;
  public region: string;
  public accessKey: string;
  public secretKey: string;
  public bucketVersions: string;
  public bucketTempDump: string;
  public bucketLocalMountPath: string;
  public runnerVersions: AWSRunnerConfigModel
  public runnerTests: AWSRunnerConfigModel;
}

export class AWSConfigModel {
  public daysForKeep: number;
  public accountConfigs: Array<{ [key: string]: AWSAccountConfigModel}>;
}

export class NotificationBasicConfigModel {
  public enabled: boolean;
  public destinations: Array<string> = [];
}

export enum NotificationConfigTypeEnum {
  WHATSAPP = 'WHATSAPP',
  EMAIL = 'EMAIL',
  GOOGLE_CHAT = "GOOGLE_CHAT",
  MONDAY_BOARD ="MONDAY_BOARD"
}

export enum NotificationTriggerEnum {
  VERSION_CREATION = "VERSION_CREATION",
  VERSION_UPDATE = "VERSION_UPDATE",
  CI_CD = "CI_CD",
  HEALTH_CHECKER = "HEALTH_CHECKER",
  BACKUP_RESTORE = "BACKUP_RESTORE"
}

export class NotificationTriggerConfigModel {
  public triggerType: NotificationTriggerEnum;
  public references: Array<string>;
  public configs: { [key: string]: NotificationBasicConfigModel};
}
export class NotificationConfigModel {
  public configs: Array<NotificationTriggerConfigModel>;
}

export class SMTPConfigModel {
  public server: string;
  public port: number;
  public ssl: boolean;
  public user: string;
  public password: string;
  public senderName: string;
  public senderEmail: string;
}

export class MondayVersionGenerationConfigModel {
  public enabled: boolean;
  public taskBoardId: string;
  public boardId: string;
  public groupId: string;
  public columnProduto: MondayColumnModel;
  public columnMajorMinor: MondayColumnModel;
  public columnPatch: MondayColumnModel;
  public columnBuild: MondayColumnModel;
  public columnStatus: MondayColumnModel;
  public columnResponsable: MondayColumnModel;
  public columnItemsIncluded: MondayColumnModel;
  public columnVersionType: MondayColumnModel;
  public columnItemsStatus: MondayColumnModel;
  public columnGenerationDate: MondayColumnModel;
  public columnGenerationHour: MondayColumnModel;
  public allowedStatusValues: { [key: string]: string};
  public allowedItemStatusValues: { [key: string]: string};
  public mappedStatusValues: { [key: string]: string};
  public mappedItemStatusValues: { [key: string]: string};

}
export class MondayConfigModel {
  public endpoint: string;
  public token: string;
  public page: string;
  public versionGenerationConfig: MondayVersionGenerationConfigModel;

}

export class CICDProductModuleConfigModel {
  public productId: number;
  public branch: string;
  public enabled: boolean;
  public includeTests: boolean;
  public ignoreHashCommit: boolean;
}

export class CICDProductConfigModel {
  public productId: number;
  public compileType: string;
  public branch: string;
  public enabled: boolean;
  public modules: Array<CICDProductModuleConfigModel>;

}
export class CICDConfigModel {
  public cronExpression: string;
  public enabled: boolean;
  public products: Array<CICDProductConfigModel>;

}

export class AXConfigModel {
  public enabled: boolean;
  public server: string;
  public user: string;
  public token: string;
}

export class PortalLuthierConfigModel {
  public enabled: boolean;
  public server: string;
  public user: string;
  public password: string;
  public luthierUser: string;
  public luthierPassword: string;
}

export class AnotherConfigModel {
}

export interface IConfig {

}
