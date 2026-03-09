export class PieChartModel {
  public series: {[key: string]: any} = {};
}

export class CoordinateChartModel {
  public x: any;
  public y: any;
}

export class LineChartModel {
  public series: {[key: string]: Array<CoordinateChartModel>} = {};
}

export class StackedBarChartValueModel {
  public x: any;
  public y: {[key: string]: any} = {};
}
export class StackedBarChartModel {
  public labels: Array<string>;
  public series: Array<StackedBarChartValueModel>;
}
