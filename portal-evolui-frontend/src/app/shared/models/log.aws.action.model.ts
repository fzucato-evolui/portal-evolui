import {UsuarioModel} from "./usuario.model";

export class LogAWSActionModel {
  public id: string;
  public logDate: Date;
  public user: UsuarioModel;
  public actionType: string;
  public instance: string;

}

export class LogAWSActionFilterModel {
  public id: number;
  public logDateFrom: Date;
  public logDateTo: Date;
  public userName: string;
  public userEmail: string;

  public actionType: LogAWSActionTypeEnum;

}

export enum LogAWSActionTypeEnum {
  START_EC2 = "START_EC2",
  STOP_EC2 = "STOP_EC2",
  REBOOT_EC2 = "REBOOT_EC2",
  START_RDS = "START_RDS",
  STOP_RDS = "STOP_RDS",
  START_WORKSPACE = "START_WORKSPACE",
  STOP_WORKSPACE = "STOP_WORKSPACE",
  REBOOT_WORKSPACE = "REBOOT_WORKSPACE",
}
