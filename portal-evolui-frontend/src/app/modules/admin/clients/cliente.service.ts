import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {ClientModel} from '../../../shared/models/client.model';
import {Observable, of, ReplaySubject} from 'rxjs';
import {switchMap} from 'rxjs/operators';
import {UtilFunctions} from '../../../shared/util/util-functions';
import {ProjectModel} from '../../../shared/models/project.model';

@Injectable({
  providedIn: 'root'
})
export class ClienteService
{
  private baseRestUrl = 'api/admin/cliente';
  private _target: ReplaySubject<ProjectModel> = new ReplaySubject<ProjectModel>(1);
  private _currentTarget: ProjectModel;
  private _model: ReplaySubject<Array<ClientModel>> = new ReplaySubject<Array<ClientModel>>(1);
  private _currentModel: Array<ClientModel>;

  private _canGenerateVersion: ReplaySubject<boolean> = new ReplaySubject<boolean>(1);

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

  set model(value: Array<ClientModel>)
  {
    this._currentModel = value;
    // Store the value
    this._model.next(value);
  }

  get model$(): Observable<Array<ClientModel>>
  {
    //console.log(this.model);
    return this._model.asObservable();
  }

  /**
   * Constructor
   */
  constructor(private _httpClient: HttpClient) {
  }

  getAll(): Promise<Array<ClientModel>>  {
    return this._httpClient.get<Array<ClientModel>>(`${this.baseRestUrl}/${this._currentTarget.identifier}/all`).pipe(
      switchMap((response: Array<ClientModel>) => {

        this.model = response;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  get(id: number): Promise<ClientModel>  {
    return this._httpClient.get<ClientModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}/${id}`).toPromise();
  }

  save(model: ClientModel): Promise<ClientModel> {
    return this._httpClient.post<ClientModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}`, model).pipe(
      switchMap((response: ClientModel) => {

        if (UtilFunctions.isValidStringOrArray(this._currentModel) === false) {
          this._currentModel = new Array<ClientModel>();
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

  delete(id: number): Promise<ClientModel>  {
    return this._httpClient.delete<ClientModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}/${id}`).pipe(
      switchMap((response: ClientModel) => {
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
