import {LineChartModel, PieChartModel, StackedBarChartModel} from './chart.model';

export class HomeGithubModel {
  public runners: PieChartModel;
  public repositories: PieChartModel;
  public members: PieChartModel;
  public productCommits:LineChartModel;
  public mainCommiters: {[key: string]: PieChartModel} = {};
}

export class HomeCICDModel {
  public produtoId: number;
  public branch: string;
  public chart: StackedBarChartModel;
}
export class HomeModel {
  public gitHub: HomeGithubModel;
  public beansCountAtualizazaoVersao: {[key: string]: PieChartModel} = {};
  public beansCountAmbiente: {[key: string]: PieChartModel} = {};
  public beansCountGeracaoVersao: {[key: string]: PieChartModel} = {};
  public beansCountBranch: {[key: string]: PieChartModel} = {};
  public cicds: Array<HomeCICDModel>
}
