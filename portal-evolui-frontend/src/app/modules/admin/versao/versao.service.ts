import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {VersaoModel} from '../../../shared/models/version.model';
import {Observable, of, ReplaySubject} from 'rxjs';
import {switchMap} from 'rxjs/operators';
import {UtilFunctions} from '../../../shared/util/util-functions';
import {ProjectModel} from '../../../shared/models/project.model';

@Injectable({
  providedIn: 'root'
})
export class VersaoService
{
  private baseRestUrl = 'api/admin/versao';
  private _target: ReplaySubject<ProjectModel> = new ReplaySubject<ProjectModel>(1);
  private _currentTarget: ProjectModel;
  private _model: ReplaySubject<Array<VersaoModel>> = new ReplaySubject<Array<VersaoModel>>(1);
  private _currentModel: Array<VersaoModel>;

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

  set model(value: Array<VersaoModel>)
  {
    this._currentModel = value;
    // Store the value
    this._model.next(value);
  }

  get model$(): Observable<Array<VersaoModel>>
  {
    //console.log(this.model);
    return this._model.asObservable();
  }

  /**
   * Constructor
   */
  constructor(private _httpVersao: HttpClient) {
  }

  getAll(): Promise<Array<VersaoModel>>  {
    return this._httpVersao.get<Array<VersaoModel>>(`${this.baseRestUrl}/${this._currentTarget.identifier}/all`).pipe(
      switchMap((response: Array<VersaoModel>) => {

        this.model = response;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  get(id: number): Promise<VersaoModel>  {
    return this._httpVersao.get<VersaoModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}/${id}`).toPromise();
  }

  save(model: VersaoModel): Promise<VersaoModel> {
    return this._httpVersao.post<VersaoModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}`, model).pipe(
      switchMap((response: VersaoModel) => {

        if (UtilFunctions.isValidStringOrArray(this._currentModel) === false) {
          this._currentModel = new Array<VersaoModel>();
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

  delete(id: number): Promise<VersaoModel>  {
    return this._httpVersao.delete<VersaoModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}/${id}`).pipe(
      switchMap((response: VersaoModel) => {
        const index = this._currentModel.findIndex(x => x.id === id);
        if (index >= 0) {
          this._currentModel.splice(index, 1);
          this.model = this._currentModel;
        }
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  deleteBranch(branch: string): Promise<{alerts: Array<string>}>  {
    return this._httpVersao.delete<{alerts: Array<string>}>(`${this.baseRestUrl}/${this._currentTarget.identifier}/branch/${branch}`).pipe(
      switchMap((response: {alerts: Array<string>}) => {
        const versions = this._currentModel.filter(x => x.branch === branch);
        if (versions && versions.length > 0) {
          versions.forEach(x => {
            const index = this._currentModel.findIndex(y => y.id == x.id);
            this._currentModel.splice(index, 1);
          })

          this.model = this._currentModel;
        }
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

}
