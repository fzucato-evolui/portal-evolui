import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, of, ReplaySubject} from 'rxjs';
import {switchMap} from 'rxjs/operators';
import {ProjectModel} from 'app/shared/models/project.model';
import {CICDFilterModel, CICDModel, CICDReportModel} from 'app/shared/models/cicd.model';
import {CICDProductConfigModel} from '../../../shared/models/system-config.model';
import {UtilFunctions} from '../../../shared/util/util-functions';
import {VersaoModel} from 'app/shared/models/version.model';

@Injectable({
  providedIn: 'root'
})
export class CicdService
{
  private baseRestUrl = 'api/admin/cicd';
  private _target: ReplaySubject<ProjectModel> = new ReplaySubject<ProjectModel>(1);
  private _currentTarget: ProjectModel;
  private _model: ReplaySubject<Array<CICDModel>> = new ReplaySubject<Array<CICDModel>>(1);
  private _currentModel: Array<CICDModel>;

  set target(value: ProjectModel)
  {
    this._currentTarget = value;
    // Store the value
    this._target.next(value);
  }

  get target$(): Observable<ProjectModel>
  {
    //console.log(this.model);
    return this._target.asObservable();
  }

  set model(value: Array<CICDModel>)
  {
    this._currentModel = value;
    // Store the value
    this._model.next(value);
  }

  get model$(): Observable<Array<CICDModel>>
  {
    //console.log(this.model);
    return this._model.asObservable();
  }

  /**
   * Constructor
   */
  constructor(private _httpClient: HttpClient) {
  }

  getAll(): Promise<Array<CICDModel>>  {
    return this._httpClient.get<Array<CICDModel>>(`${this.baseRestUrl}/${this._currentTarget.identifier}/all`).pipe(
      switchMap((response: Array<CICDModel>) => {

        this.model = response;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  get(id: number): Promise<CICDModel>  {
    return this._httpClient.get<CICDModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}/${id}`).toPromise();
  }

  filter(model: CICDFilterModel): Promise<Array<CICDModel>> {
    return this._httpClient.post<Array<CICDModel>>(`${this.baseRestUrl}/${this._currentTarget.identifier}/filter`, model).pipe(
      switchMap((response: Array<CICDModel>) => {

        this.model = response;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }



  getLogs(id: number): Promise<Array<CICDModel>>  {
    return this._httpClient.get<Array<CICDModel>>(`${this.baseRestUrl}/${this._currentTarget.identifier}/logs/${id}`).toPromise();
  }

  getLink(workflow: number): Promise<{resp: string}>  {
    return this._httpClient.get<{resp: string}>(`${this.baseRestUrl}/${this._currentTarget.identifier}/link/${workflow}`).toPromise();
  }

  getDetailedReport(workflow: number):  Promise<{[key: string]: CICDReportModel}>  {
    return this._httpClient.get<{[key: string]: CICDReportModel}>(`${this.baseRestUrl}/${this._currentTarget.identifier}/detailed-report/${workflow}`).toPromise();
  }

  getCheckrunLink(productIdentifier: string, checkRunId: number): Promise<{resp: string}>  {
    return this._httpClient.get<{resp: string}>(`${this.baseRestUrl}/${productIdentifier}/checkrun-link/${checkRunId}`).toPromise();
  }

  getErrors(id: number): Promise<{resp: string}>  {
    return this._httpClient.get<{resp: string}>(`${this.baseRestUrl}/${this._currentTarget.identifier}/errors/${id}`).toPromise();
  }


  getBranches(): Promise<Array<string>>  {
    return this._httpClient.get<Array<string>>(`${this.baseRestUrl}/${this._currentTarget.identifier}/branches`).toPromise();
  }

  getVersions(): Promise<Array<VersaoModel>>  {
    return this._httpClient.get<Array<VersaoModel>>(`${this.baseRestUrl}/${this._currentTarget.identifier}/versions`).toPromise();
  }

  getModuleBranches(moduleId: number): Promise<Array<string>>  {
    return this._httpClient.get<Array<string>>(`${this.baseRestUrl}/module-branches/${moduleId}`).toPromise();
  }

  save(model: CICDProductConfigModel): Promise<CICDModel> {
    return this._httpClient.post<CICDModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}/run`, model).pipe(
      switchMap((response: CICDModel) => {

        if (UtilFunctions.isValidStringOrArray(this._currentModel) === false) {
          this._currentModel = [];
        }
        this._currentModel.push(response);
        this.model = this._currentModel;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  delete(id: number): Promise<{alerts: Array<string>}>  {
    return this._httpClient.delete<{alerts: Array<string>}>(`${this.baseRestUrl}/${this._currentTarget.identifier}/${id}`).pipe(
      switchMap((response: {alerts: Array<string>}) => {
        const index = this._currentModel.findIndex(x => x.id === id);
        if (index >= 0) {
          this._currentModel.splice(index, 1);
          this.model = this._currentModel;
        }
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }
}
