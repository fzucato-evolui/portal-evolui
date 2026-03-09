import {FolderModel} from "./folder.model";
import {WhatsappPhoneModel} from "./whatsapp-phone.model";
import {EmailDepartmentConfigModel, WhatsAppDepartmentConfigModel} from "./config.model";

export class ShareModel {
  public shareType: ShareModelEnum;
  public parser: WhatsAppShareModel | EmailShareModel;
}
export enum ShareModelEnum {
  WHATSAPP = "WHATSAPP",
  EMAIL = "EMAIL"
}

export class WhatsAppShareModel {
  public destinations: Array<WhatsappPhoneModel>;
  public folder: FolderModel;
  public message: string;
  public department: WhatsAppDepartmentConfigModel;
}

export class EmailShareModel {
  public destinations: Array<string>;
  public folder: FolderModel;
  public message: string;
  public subject: string;
  public department: EmailDepartmentConfigModel;
}
