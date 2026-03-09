import {ProjectModel} from './project.model';

export class ClientModel {
  public id: number;
  public identifier: string;
  public description: string;
  public keywords: Array<string> = new Array<string>();
  public produto: ProjectModel
}
