import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, of, ReplaySubject} from 'rxjs';
import {switchMap} from 'rxjs/operators';
import {AmbienteModel} from 'app/shared/models/ambiente.model';
import {ClientModel} from '../../../shared/models/client.model';
import {RunnerGithubModel} from '../../../shared/models/github.model';
import {UtilFunctions} from '../../../shared/util/util-functions';
import {ProjectModel} from '../../../shared/models/project.model';
import {VersaoModel} from 'app/shared/models/version.model';
import {AtualizacaoVersaoModel} from '../../../shared/models/atualizacao-versao.model';

@Injectable({
  providedIn: 'root'
})
export class AmbienteService
{
  private baseRestUrl = 'api/admin/ambiente';
  private _target: ReplaySubject<ProjectModel> = new ReplaySubject<ProjectModel>(1);
  private _currentTarget: ProjectModel;
  private _model: ReplaySubject<Array<AmbienteModel>> = new ReplaySubject<Array<AmbienteModel>>(1);
  private _currentModel: Array<AmbienteModel>;

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

  set model(value: Array<AmbienteModel>)
  {
    this._currentModel = value;
    // Store the value
    this._model.next(value);
  }

  get model$(): Observable<Array<AmbienteModel>>
  {
    //console.log(this.model);
    return this._model.asObservable();
  }
  /**
   * Constructor
   */
  constructor(private _httpClient: HttpClient) {
  }

  getAll(): Promise<Array<AmbienteModel>>  {
    return this._httpClient.get<Array<AmbienteModel>>(`${this.baseRestUrl}/${this._currentTarget.identifier}/all`).pipe(
      switchMap((response: Array<AmbienteModel>) => {

        this.model = response;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  get(id: number): Promise<AmbienteModel>  {
    return this._httpClient.get<AmbienteModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}/${id}`).toPromise();
  }


  save(model: AmbienteModel): Promise<AmbienteModel> {
    return this._httpClient.post<AmbienteModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}`, model).pipe(
      switchMap((response: AmbienteModel) => {
        if (UtilFunctions.isValidStringOrArray(this._currentModel) === false) {
          this._currentModel = new Array<AmbienteModel>();
        }
        const index = model.id && model.id > 0 ? this._currentModel.findIndex(x => x.id === model.id) : -1;
        if (index < 0) {
          this._currentModel.push(response);
        } else {
          this._currentModel[index] = response;
        }
        this.model = this._currentModel;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  delete(id: number): Promise<AmbienteModel>  {
    return this._httpClient.delete<AmbienteModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}/${id}`).pipe(
      switchMap((response: AmbienteModel) => {
        const index = this._currentModel.findIndex(x => x.id === id);
        if (index >= 0) {
          this._currentModel.splice(index, 1);
          this.model = this._currentModel;
        }
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  getInitialData(id: number): Promise<{runners: Array<RunnerGithubModel>, clients: Array<ClientModel>, versions: Array<VersaoModel>, history: Array<AtualizacaoVersaoModel>}>  {
    return this._httpClient.get<{runners: Array<RunnerGithubModel>, clients: Array<ClientModel>, versions: Array<VersaoModel>, history: Array<AtualizacaoVersaoModel>}>(`${this.baseRestUrl}/${this._currentTarget.identifier}/initial-data/${id}`).toPromise();
  }
}
