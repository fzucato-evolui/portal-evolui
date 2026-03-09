import {ClientModel} from './client.model';
import {ProjectModel} from './project.model';

export enum DatabaseTypeEnum {
  MSSQL = 'MSSQL',
  ORACLE = 'ORACLE',
  POSTGRES = 'POSTGRES'
}
export class MetadadosBranchModel {
  public id: number;
  public branch: string;
  public dbType: DatabaseTypeEnum;
  public host: string;
  public port: number;
  public debugId: number;
  public database: string;
  public dbUser: string;
  public dbPassword: string;
  public lthUser: string;
  public lthPassword: string;
  public licenseServer: string;
  public jvmOptions: string;
  public clients: Array<{id: number, client: ClientModel}> = new Array<{id: number, client: ClientModel}>();
  public produto: ProjectModel;
}
