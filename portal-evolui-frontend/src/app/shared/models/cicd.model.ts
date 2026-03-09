import {UsuarioModel} from './usuario.model';
import {VersionConclusionEnum, VersionModel, VersionStatusEnum} from './version.model';
import {ProjectModel} from './project.model';

export type CICDReportStatusType = 'SUCCESS' | 'SKIPPED' | 'FAILURE';

export class CICDSummary {
  public skipped: number;
  public success: number;
  public failure: number;
  public totalTime: number;
}
export class ModuleCICDModel extends VersionModel {
  public enabled: boolean;
  public includeTests: boolean;
  public projectModule: ProjectModel;
  public checkrun: number;
  public status: CICDReportStatusType;
  public fatalError: boolean;
  public buildSumary: CICDSummary;
  public testSumary:CICDSummary;

}
export class CICDModel extends VersionModel {
  public workflow: number;
  public link: string;
  public project: ProjectModel;
  public user: UsuarioModel;
  public requestDate: Date;
  public schedulerDate: Date;
  public conclusionDate: Date;
  public status: VersionStatusEnum;
  public conclusion: VersionConclusionEnum;
  public modules: Array<ModuleCICDModel> = new Array<ModuleCICDModel>()
  public error: string;

}

export class CICDFilterModel {
  public userName: String;
  public userEmail: String;
  public requestDateFrom: Date;
  public requestDateTo: Date;
  public status: VersionStatusEnum;
  public conclusion: VersionConclusionEnum;
  public version: string;

}

export class CICDSumaryMavenReportModel {
  public count: number = 0;
  public totalTime: number = 0;
}
export class CICDErrorDetailReportModel extends CICDSumaryMavenReportModel{
  public file: string;
  public stackTrace: string;
  public message: string;
  public title: string;
  public lineError: number;
}
export class CICDMethodTestReportModel extends CICDErrorDetailReportModel {
  public name: string;
  public fileName: string;
  public fileNameWithPackage: string;
  public status: CICDReportStatusType | string;
}

export class CICDClassReportModel {
  public path: string;
  public name: string;
  public package: string;
  public extension: string;
  public buildErrors: Array<CICDErrorDetailReportModel> = new Array<CICDErrorDetailReportModel>();
  public tests: Array<CICDMethodTestReportModel> = new Array<CICDMethodTestReportModel>();
  public testSumary: {[key: string]: CICDSumaryMavenReportModel} = {};
}
export class CICDProjectReportModel {
  public identifier: string;
  public name: string;
  public path: string;
  public artifactId: string;
  public groupId: string;
  public packaging: string;
  public buildTotalTime: number;
  public buildStatus : CICDReportStatusType | string;
  public testSumary: {[key: string]: CICDSumaryMavenReportModel} = {};
  public classes: Array<CICDClassReportModel> = new Array<CICDClassReportModel>();
}

export class CICDReportModel {
  public projects: Array<CICDProjectReportModel> = new Array<CICDProjectReportModel>();
  public buildSumary: {[key: string]: CICDSumaryMavenReportModel} = {};
  public testSumary: {[key: string]: CICDSumaryMavenReportModel} = {};
  public multiModule: boolean;
  public fatalError: any;
}


