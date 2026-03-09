import {UtilFunctions} from "../util/util-functions";
import {UsuarioModel} from "./usuario.model";

export class ConfigModel {
  public id: number;
  public user: UsuarioModel;
  public configType: ConfigModelEnum;
  public repositoryType: RepositoryConfigModelEnum;
  public config: WhatsAppConfigModel | EmailConfigModel | AWSRepositoryConfigModel | GoogleRepositoryConfigModel | ServerFolderRepositoryConfigModel;

}

export enum ConfigModelEnum {
  WHATSAPP = "WHATSAPP",
  EMAIL = "EMAIL",
  REPOSITORY = "REPOSITORY"
}

export enum RepositoryConfigModelEnum {
  AWS = "AWS",
  GOOGLE = "GOOGLE",
  SERVERFOLDER = "SERVERFOLDER"
}

export class WhatsAppConfigModel {
  public token: string;
  public departments: Array<WhatsAppDepartmentConfigModel>;
}

export class WhatsAppDepartmentConfigModel {
  public identifier: string;
  public description: string;
  public phoneId: string;
  public senderName: string;
  public template: string;
  public principal: boolean;
}

export class EmailConfigModel {
  public server: string;
  public port: number;
  public ssl: boolean;
  public departments: Array<EmailDepartmentConfigModel>;
}

export class EmailDepartmentConfigModel {
  public identifier: string;
  public description: string;
  public user: string;
  public password: string;
  public senderName: string;
  public senderEmail: string;
  public template: string;
  public principal: boolean;
}

export class AWSRepositoryConfigModel {
  public accessKey: string;
  public secretKey: string;
  public regionName: string;
  public bucketName: string;
}

export class GoogleRepositoryConfigModel {
  public bucketName: string;
  public credentials: GoogleServiceAccountModel;
}

export class ServerFolderRepositoryConfigModel {
  public nothing = "nothing";
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
