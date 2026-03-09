import {UsuarioModel} from './usuario.model';
import {
  DiffCommitListModel,
  VersionConclusionEnum,
  VersionModel,
  VersionStatusEnum,
  VersionTypeEnum
} from './version.model';
import {ProjectModel, ProjectModuleModel} from './project.model';

export class GeracaoVersaoModuloModel extends VersionModel {
  public projectModule: ProjectModuleModel;
  public enabled: boolean;
}
export class GeracaoVersaoModel extends VersionModel {
  public workflow: number;
  public mondayId: string;
  public link: string;
  public mondayLink: string;
  public user: UsuarioModel;
  public requestDate: Date;
  public conclusionDate: Date;
  public status: VersionStatusEnum;
  public conclusion: VersionConclusionEnum;
  public compileType: VersionTypeEnum;
  public project: ProjectModel;
  public modules: Array<GeracaoVersaoModuloModel> = new Array<GeracaoVersaoModuloModel>();

}

export class GeracaoVersaoFilterModel {
  public userName: String;
  public userEmail: String;
  public requestDateFrom: Date;
  public requestDateTo: Date;
  public compileType: VersionTypeEnum;
  public status: VersionStatusEnum;
  public conclusion: VersionConclusionEnum;
  public version: string;

}

export class VersionGenerationModuleRequestModel {
  public id: number;
  public branch: string;
  public commit: string;
  public enabled: boolean = true;
}

export class VersionGenerationRequestModel {
  public id: number;
  public compileType: VersionTypeEnum;
  public tag: string;
  public modules: Array<VersionGenerationModuleRequestModel> = [];
}

export class GeracaoVersaoDiffModuleModel {
  public module: ProjectModel;
  public diffs: DiffCommitListModel;
}
export class GeracaoVersaoDiffModel {
  public from: GeracaoVersaoModel;
  public to: GeracaoVersaoModel;
  public modulesDiff: Array<GeracaoVersaoDiffModuleModel>;
}
