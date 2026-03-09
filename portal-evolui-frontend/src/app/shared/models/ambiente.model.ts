import {VersionModel} from './version.model';
import {ClientModel} from './client.model';
import {ProjectModel, ProjectModuleModel} from './project.model';

export class FileMapConfigAmbienteModel {
  public source: string;
  public destination: string;
}

export class DestinationServerConfigAmbienteModel {
  public host: string;
  public user: string;
  public password?: string;
  public port?: number;
  public privateKey?: string;
  public workDirectory?: string;
}

export class ModuleConfigAmbienteModel {
  public enabled: boolean = true;
  public destinationPath: string;
  public beforeUpdateModuleCommand: string;
  public afterUpdateModuleCommand: string;
  public runnerId: string;
  public runnerIdentifier: string;
  public contextURL: string;
  public contextName: string;
  public lthUser: string;
  public lthPassword: string;
  public jvmOptionsCommand: string;
  public jvmOptionsLiquibase: string;
  public filesMap: Array<FileMapConfigAmbienteModel>;
  public destinationServer: DestinationServerConfigAmbienteModel;

}

export class AmbienteModuloModel extends VersionModel {
  public projectModule: ProjectModuleModel;
  public config: ModuleConfigAmbienteModel;
}

export class AmbienteModel extends VersionModel{
  public identifier: string;
  public description: string;
  public client: ClientModel;
  public project: ProjectModel;
  public busy: boolean;
  public modules: Array<AmbienteModuloModel> = new Array<AmbienteModuloModel>();

}

