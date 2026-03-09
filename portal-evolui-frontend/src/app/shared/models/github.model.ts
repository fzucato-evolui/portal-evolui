export type RunnerGithubLabelType = 'read-only' | 'custom';
export class MemberGithubModel {
  public login: string;
  public id: number;
  public avatar_url: string;
  public url: string;
  public name: string;
  public company: string;
  public type: string;
  public site_admin: boolean;
}

export class DetailedRepositoryGithubModel {
  public id: number;
  public node_id: string;
  public name: string;
  public full_name: string;
  public html_url: string;
  public description: string;
  public url: string;
  public created_at: Date;
  public updated_at: Date;
  public pushed_at: Date;
  public language: string;
  public archived: boolean;
  public disabled: boolean;

  public default_branch: string;
  public topics: Array<string>;
}


export class GithubContentModel {
  public name: string;
  public path: string;
  public sha: string;
  public size: number;
  public type: string;
  public html_url: string;
  public download_url: string;

  isDirectory(): boolean {
    return this.type === 'dir';
  }
}

export class GitRefDetailModel {
  public name: string;
  public sha: string;
  public author: string;
  public message: string;
  public date: Date;
}

export class BranchesAndTagsDetailModel {
  public branches: Array<GitRefDetailModel>;
  public tags: Array<GitRefDetailModel>;
}

export class RunnerGithubLabelModel {
  public id: number;
  public name: string;
  public type: RunnerGithubLabelType;
}
export class RunnerGithubModel {
  public id: number;
  public name: string;
  public os: string;
  public status: string;
  public busy: boolean;
  public labels: Array<RunnerGithubLabelModel>;
}
