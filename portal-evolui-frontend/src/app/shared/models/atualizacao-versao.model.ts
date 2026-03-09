import {UsuarioModel} from './usuario.model';
import {VersionConclusionEnum, VersionModel, VersionStatusEnum} from './version.model';
import {AmbienteModel, AmbienteModuloModel} from './ambiente.model';

export class ModuleAtualizacaoVersaoModel extends VersionModel {
  public enabled: boolean;
  public executeUpdateCommands: boolean;
  public environmentModule: AmbienteModuloModel;
}
export class AtualizacaoVersaoModel extends VersionModel {
  public workflow: number;
  public link: string;
  public user: UsuarioModel;
  public requestDate: Date;
  public schedulerDate: Date;
  public conclusionDate: Date;
  public status: VersionStatusEnum;
  public conclusion: VersionConclusionEnum;
  public tags: Array<string>;
  public modules: Array<ModuleAtualizacaoVersaoModel> = new Array<ModuleAtualizacaoVersaoModel>()
  public environment: AmbienteModel;
  public error: string;

}

export class AtualizacaoVersaoFilterModel {
  public userName: String;
  public userEmail: String;
  public requestDateFrom: Date;
  public requestDateTo: Date;
  public status: VersionStatusEnum;
  public conclusion: VersionConclusionEnum;
  public version: string;

}

