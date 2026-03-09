import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {AtualizacaoVersaoFilterModel, AtualizacaoVersaoModel} from '../../../shared/models/atualizacao-versao.model';
import {Observable, of, ReplaySubject} from 'rxjs';
import {switchMap} from 'rxjs/operators';
import {VersaoModel} from 'app/shared/models/version.model';
import {UtilFunctions} from '../../../shared/util/util-functions';
import {AmbienteModel} from '../../../shared/models/ambiente.model';
import {ProjectModel} from 'app/shared/models/project.model';

@Injectable({
  providedIn: 'root'
})
export class AtualizacaoVersaoService
{
  private baseRestUrl = 'api/admin/atualizacao-versao';
  private _target: ReplaySubject<ProjectModel> = new ReplaySubject<ProjectModel>(1);
  private _currentTarget: ProjectModel;
  private _model: ReplaySubject<Array<AtualizacaoVersaoModel>> = new ReplaySubject<Array<AtualizacaoVersaoModel>>(1);
  private _currentModel: Array<AtualizacaoVersaoModel>;

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

  set model(value: Array<AtualizacaoVersaoModel>)
  {
    this._currentModel = value;
    // Store the value
    this._model.next(value);
  }

  get model$(): Observable<Array<AtualizacaoVersaoModel>>
  {
    //console.log(this.model);
    return this._model.asObservable();
  }

  /**
   * Constructor
   */
  constructor(private _httpClient: HttpClient) {
  }

  getAll(): Promise<Array<AtualizacaoVersaoModel>>  {
    return this._httpClient.get<Array<AtualizacaoVersaoModel>>(`${this.baseRestUrl}/${this._currentTarget.identifier}/all`).pipe(
      switchMap((response: Array<AtualizacaoVersaoModel>) => {

        this.model = response;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  get(id: number): Promise<AtualizacaoVersaoModel>  {
    return this._httpClient.get<AtualizacaoVersaoModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}/${id}`).toPromise();
  }

  filter(model: AtualizacaoVersaoFilterModel): Promise<Array<AtualizacaoVersaoModel>> {
    return this._httpClient.post<Array<AtualizacaoVersaoModel>>(`${this.baseRestUrl}/${this._currentTarget.identifier}/filter`, model).pipe(
      switchMap((response: Array<AtualizacaoVersaoModel>) => {

        this.model = response;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }


  save(model: AtualizacaoVersaoModel): Promise<AtualizacaoVersaoModel> {
    return this._httpClient.post<AtualizacaoVersaoModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}`, model).pipe(
      switchMap((response: AtualizacaoVersaoModel) => {
        if (UtilFunctions.isValidStringOrArray(this._currentModel) === false) {
          this._currentModel = new Array<AtualizacaoVersaoModel>();
        }
        this._currentModel.push(response);
        this.model = this._currentModel;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  cancel(id: number): Promise<AtualizacaoVersaoModel>  {
    return this._httpClient.get<AtualizacaoVersaoModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}/cancel/${id}`).pipe(
      switchMap((response: AtualizacaoVersaoModel) => {
        const index = this._currentModel.findIndex(x => x.id === id);
        if (index >= 0) {
          this._currentModel[index] = response;
          this.model = this._currentModel;
        }
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  rerunFailed(id: number): Promise<AtualizacaoVersaoModel>  {
    return this._httpClient.get<AtualizacaoVersaoModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}/rerun-failed/${id}`).pipe(
      switchMap((response: AtualizacaoVersaoModel) => {
        const index = this._currentModel.findIndex(x => x.id === id);
        if (index >= 0) {
          this._currentModel[index] = response;
          this.model = this._currentModel;
        }
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  getLogs(id: number): Promise<Array<AtualizacaoVersaoModel>>  {
    return this._httpClient.get<Array<AtualizacaoVersaoModel>>(`${this.baseRestUrl}/${this._currentTarget.identifier}/logs/${id}`).toPromise();
  }

  getLink(workflow: number): Promise<{resp: string}>  {
    return this._httpClient.get<{resp: string}>(`${this.baseRestUrl}/${this._currentTarget.identifier}/link/${workflow}`).toPromise();
  }

  getErrors(id: number): Promise<{resp: string}>  {
    return this._httpClient.get<{resp: string}>(`${this.baseRestUrl}/${this._currentTarget.identifier}/errors/${id}`).toPromise();
  }

  getInitialData(): Promise<{versions: Array<VersaoModel>, environments: Array<AmbienteModel>, history: Array<AtualizacaoVersaoModel>}>  {
    return this._httpClient.get<{versions: Array<VersaoModel>, environments: Array<AmbienteModel>, history: Array<AtualizacaoVersaoModel>}>(`${this.baseRestUrl}/${this._currentTarget.identifier}/initial-data`).toPromise();
  }

  delete(id: number): Promise<any>  {
    return this._httpClient.delete<AtualizacaoVersaoModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}/${id}`).pipe(
      switchMap((response: AtualizacaoVersaoModel) => {
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
