import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, of, ReplaySubject} from 'rxjs';
import {switchMap} from 'rxjs/operators';
import {UtilFunctions} from '../../../shared/util/util-functions';
import {MetadadosBranchModel} from 'app/shared/models/metadados-branch.model';
import {ClientModel} from '../../../shared/models/client.model';
import {AvailableVersions} from 'app/shared/models/version.model';
import {ProjectModel} from '../../../shared/models/project.model';

@Injectable({
  providedIn: 'root'
})
export class MetadadosService
{
  private baseRestUrl = 'api/admin/metadados';
  private _target: ReplaySubject<ProjectModel> = new ReplaySubject<ProjectModel>(1);
  private _currentTarget: ProjectModel;
  private _model: ReplaySubject<Array<MetadadosBranchModel>> = new ReplaySubject<Array<MetadadosBranchModel>>(1);
  private _currentModel: Array<MetadadosBranchModel>;

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

  set model(value: Array<MetadadosBranchModel>)
  {
    this._currentModel = value;
    // Store the value
    this._model.next(value);
  }

  get model$(): Observable<Array<MetadadosBranchModel>>
  {
    //console.log(this.model);
    return this._model.asObservable();
  }

  /**
   * Constructor
   */
  constructor(private _httpClient: HttpClient) {
  }

  getAll(): Promise<Array<MetadadosBranchModel>>  {
    return this._httpClient.get<Array<MetadadosBranchModel>>(`${this.baseRestUrl}/${this._currentTarget.identifier}/all`).pipe(
      switchMap((response: Array<MetadadosBranchModel>) => {

        this.model = response;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  get(id: number): Promise<MetadadosBranchModel>  {
    return this._httpClient.get<MetadadosBranchModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}/${id}`).toPromise();
  }

  save(model: MetadadosBranchModel): Promise<MetadadosBranchModel> {
    return this._httpClient.post<MetadadosBranchModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}`, model).pipe(
      switchMap((response: MetadadosBranchModel) => {

        if (UtilFunctions.isValidStringOrArray(this._currentModel) === false) {
          this._currentModel = new Array<MetadadosBranchModel>();
        }
        if (model.id && model.id > 0) {
          const index = this._currentModel.findIndex(x => x.id === model.id);
          this._currentModel[index] = response;
        } else {
          this._currentModel.push(response);
        }
        this.model = this._currentModel;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  delete(id: number): Promise<MetadadosBranchModel>  {
    return this._httpClient.delete<MetadadosBranchModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}/${id}`).pipe(
      switchMap((response: MetadadosBranchModel) => {
        const index = this._currentModel.findIndex(x => x.id === id);
        if (index >= 0) {
          this._currentModel.splice(index, 1);
          this.model = this._currentModel;
        }
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  getInitialData(): Promise<{branches: AvailableVersions, clients: Array<ClientModel>}>  {
    return this._httpClient.get<{branches: AvailableVersions, clients: Array<ClientModel>}>(`${this.baseRestUrl}/${this._currentTarget.identifier}/initial-data`).toPromise();
  }

}
