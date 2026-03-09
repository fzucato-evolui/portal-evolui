export class IconModel {
  public fontSet: string;
  public fontIcon: string;
}
export class ProjectModel {
  public id: number;
  public identifier: string;
  public description: string;
  public title: string;
  public icon: IconModel;
  public framework: boolean;
  public repository: string;
  public luthierProject: boolean;
  public modules: Array<ProjectModuleModel>;
}

export class ProjectModuleModel {
  public id: number;
  public identifier: string;
  public description: string;
  public title: string;
  public icon: IconModel;
  public main: boolean;
  public framework: boolean;
  public repository: string;
  public relativePath: string;
  public childBonds: Array<ProjectModuleModel>;
  public bond: ProjectModuleModel;
}
