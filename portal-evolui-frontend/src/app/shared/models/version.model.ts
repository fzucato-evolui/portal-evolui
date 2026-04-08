import {ProjectModel, ProjectModuleModel} from './project.model';

export class VersionModel {
  public id: number;
  public major: number;
  public minor: number;
  public patch: number;
  public build: string;
  public qualifier: string;
  public tag: string;
  public branch: string;
  public commit: string;
  public repository: string;
  public relativePath: string;
  public versionType: VersionTypeEnum
}

export enum VersionTypeEnum {
  patch = 'patch',
  stable = 'stable',
  beta = 'beta',
  alpha = 'alpha',
  rc = 'rc'
};

export enum VersionStatusEnum {
  queued = "queued",
  in_progress = "in_progress",
  completed = "completed",
  scheduled = "scheduled"
};

export enum VersionConclusionEnum {
  success = "success",
  failure = "failure",
  neutral = "neutral",
  cancelled = "cancelled",
  skipped = "skipped",
  timed_out = "timed_out",
  action_required = "action_required",
  cancelling = "cancelling",
  scheduler_error = "scheduler_error",
  warning = "warning"
};

export class AuthorCommitModel {
  public name: string;
  public date: Date;
}

export class CommitModel {
  public author: AuthorCommitModel;
  public message: string;
}

export class AuthorDiffCommitModel {
  public login: string;
  public id: number;
  public avatar_url: string;
}
export class DiffCommitModel {
  public sha: string
  public commit: CommitModel;
  public author: AuthorDiffCommitModel;
  public html_url: string;
}

export class DiffCommitListModel {
  public total_commits: number;
  public ahead_by: number;
  public behind_by: number;
  public commits: Array<DiffCommitModel>;
}

export class AvailableVersions {
  public branches: Array<{version: string, lastTag: string, tags: Array<string>, abnormalBranch: boolean}>;
}

export class VersaoModuloModel extends VersionModel {
  public projectModule: ProjectModuleModel;
}
export class VersaoModel extends VersionModel{
  public project: ProjectModel;
  public modules: Array<VersaoModuloModel> = new Array<VersaoModuloModel>();
  public beta: boolean;
}
