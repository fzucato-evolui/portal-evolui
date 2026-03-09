import {WhatsappPhoneModel} from "./whatsapp-phone.model";

export class FolderInfoModel {
  public identifier: string;
  public code: string;
  public allowEdit: boolean;
  public allowShare: boolean;
  public whatsAppShareInfo: WhatsAppShareInfoModel;
  public emailShareInfo: EmailShareInfoModel;
}

export class WhatsAppShareInfoModel {
  public allowEdit: boolean;
  public destinations: Array<WhatsappPhoneModel>;
  public departmentIdentifier: string;
  public message: string;
}

export class EmailShareInfoModel {
  public allowEdit: boolean;
  public destinations: Array<string>;
  public departmentIdentifier: string;
  public message: string;
  public subject: string;
}
