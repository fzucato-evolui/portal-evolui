import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, of, ReplaySubject} from 'rxjs';
import {switchMap, tap} from 'rxjs/operators';
import {
  GoogleSpaceModel,
  MondayBoardModel,
  MondayColumnModel,
  MondayGroupModel,
  SystemConfigModel
} from "../../../shared/models/system-config.model";
import {EC2Model} from '../../../shared/models/ec2.model';
import {WorkspaceModel} from '../../../shared/models/workspace.model';
import {RunnerGithubModel} from '../../../shared/models/github.model';
import {AmbienteModel} from '../../../shared/models/ambiente.model';
import {HealthCheckerModel} from '../../../shared/models/health-checker.model';
import {RDSModel} from '../../../shared/models/rds.model';

export type ConfigInitialDataType = {
  configs: Array<SystemConfigModel>,
  // ec2<conta>/wks<conta>: listas de EC2/Workspace por conta AWS configurada, chave dinâmica
  // (montada no backend como "ec2" + nome da conta / "wks" + nome da conta) — acessadas via
  // initialData['ec2'+account] no template, por isso não têm um nome de propriedade fixo aqui.
  [key: `ec2${string}`]: Array<EC2Model>,
  [key: `wks${string}`]: Array<WorkspaceModel>,
  rds: Array<RDSModel>,
  runners: Array<RunnerGithubModel>,
  environments: Array<AmbienteModel>,
  healthcheckers: Array<HealthCheckerModel>,
  spaces: Array<GoogleSpaceModel>,
  boards: Array<MondayBoardModel>,
  boardBuildGroups: Array<MondayGroupModel>,
  boardBuildColumns: Array<MondayColumnModel>
  boardTaskColumns: Array<MondayColumnModel>
  productBranches: {[key: string]: Array<string>};
}
@Injectable({
  providedIn: 'root'
})

export class ConfigSystemService
{
  private _model: ReplaySubject<Array<SystemConfigModel>> = new ReplaySubject<Array<SystemConfigModel>>(1);
  private _initialData: ReplaySubject<ConfigInitialDataType> = new ReplaySubject<ConfigInitialDataType>(1);

  private _currentModel: ConfigInitialDataType;
  /**
   * Constructor
   */
  constructor(private _httpClient: HttpClient)
  {

  }
  /*
 get model$(): Observable<Array<SystemConfigModel>>
 {
   return this._model.asObservable();
 }
 */
  set model(value: ConfigInitialDataType)
  {
    this._currentModel = value;
    this._initialData.next(value);
  }

  get model$(): Observable<ConfigInitialDataType>
  {
    return this._initialData.asObservable();
  }


  get(): Observable<Array<SystemConfigModel>>
  {
    return this._httpClient.get<Array<SystemConfigModel>>('/api/admin/sysconfig').pipe(
      tap((resp) => {
        this._model.next(resp);

      },err => {
        console.error("pipe error", err);
        throw err;
      })
    );

    //return of(new RootModel())
  }

  getInitialData(): Observable<ConfigInitialDataType>
  {
    return this._httpClient.get<ConfigInitialDataType>('/api/admin/sysconfig/initial-data').pipe(
      tap((resp) => {
        this.model = resp;
      },err => {
        console.error("pipe error", err);
        throw err;
      })
    );

    //return of(new RootModel())
  }

  save(model: SystemConfigModel): Promise<SystemConfigModel>
  {
    return this._httpClient.post<SystemConfigModel>('/api/admin/sysconfig', model).toPromise().then(resp => {
      if (this._currentModel?.configs) {
        const idx = this._currentModel.configs.findIndex(c => c.configType === resp.configType);
        if (idx >= 0) {
          this._currentModel.configs[idx] = resp;
        } else {
          this._currentModel.configs.push(resp);
        }
        this.model = this._currentModel;
      }
      return resp;
    });
  }

  sendEmailTest(destination: string): Promise<any> {
    return this._httpClient.get('/api/admin/sysconfig/email-test/' + destination).toPromise();
  }

  getMondayBuildGroupsColumns(boardId: string): Promise<ConfigInitialDataType>  {
    return this._httpClient.get<ConfigInitialDataType>(`/api/admin/sysconfig/monday-build-groups-columns/${boardId}`).pipe(
      switchMap((response: ConfigInitialDataType) => {

        this._currentModel.boardBuildGroups = response.boardBuildGroups;
        this._currentModel.boardBuildColumns = response.boardBuildColumns;
        this.model = this._currentModel;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  getMondayTaskColumns(boardId: string): Promise<ConfigInitialDataType>  {
    return this._httpClient.get<ConfigInitialDataType>(`/api/admin/sysconfig/monday-task-columns/${boardId}`).pipe(
      switchMap((response: ConfigInitialDataType) => {

        this._currentModel.boardTaskColumns = response.boardTaskColumns;
        this.model = this._currentModel;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  getMondayColumn(boardId: string, columnId: string): Promise<MondayColumnModel> {
    return this._httpClient.get<MondayColumnModel>(`/api/admin/sysconfig/monday-column/${boardId}/${columnId}`).toPromise();
  }

}
