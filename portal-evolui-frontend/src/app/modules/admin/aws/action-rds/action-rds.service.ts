import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {RDSModel} from '../../../../shared/models/rds.model';
import {BucketModel} from '../../../../shared/models/bucket.model';
import {switchMap} from 'rxjs/operators';
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {Observable, of, ReplaySubject} from 'rxjs';
import {ActionRdsFilterModel, ActionRdsModel} from '../../../../shared/models/action-rds.model';
import {VersionConclusionEnum} from '../../../../shared/models/version.model';

@Injectable({
  providedIn: 'root'
})
export class ActionRdsService
{

  private _model: ReplaySubject<Array<ActionRdsModel>> = new ReplaySubject<Array<ActionRdsModel>>(1);
  private _currentModel: Array<ActionRdsModel>;

  set model(value: Array<ActionRdsModel>)
  {
    this._currentModel = value;
    // Store the value
    this._model.next(value);
  }

  get model$(): Observable<Array<ActionRdsModel>>
  {
    //console.log(this.model);
    return this._model.asObservable();
  }
  /**
   * Constructor
   */
  constructor(private _httpClient: HttpClient)
  {

  }

  filtrar(model: ActionRdsFilterModel): Promise<Array<ActionRdsModel>>
  {
    return this._httpClient.post<Array<ActionRdsModel>>('api/admin/aws/action-rds/filter', model).pipe(
      switchMap((response: Array<ActionRdsModel>) => {

        this.model = response;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  getAllRDSs(): Promise<{[key: string]: Array<RDSModel>}>
  {
    return this._httpClient.get<{[key: string]: Array<RDSModel>}>('api/admin/aws/action-rds/databases').toPromise();
  }

  retrieveSchemas(model: RDSModel): Promise<Array<string>> {
    return this._httpClient.post<Array<string>>('api/admin/aws/action-rds/retrieve-schemas', model).toPromise();
  }

  retrieveTableSpaces(model: RDSModel): Promise<Array<string>> {
    return this._httpClient.post<Array<string>>('api/admin/aws/action-rds/retrieve-tablespaces', model).toPromise();
  }

  retrieveBuckets(model: BucketModel): Promise<Array<BucketModel>> {
    return this._httpClient.post<Array<BucketModel>>('api/admin/aws/action-rds/retrieve-buckets', model).toPromise();
  }

  save(model: ActionRdsModel): Promise<ActionRdsModel> {
    return this._httpClient.post<ActionRdsModel>('api/admin/aws/action-rds', model).pipe(
      switchMap((response: ActionRdsModel) => {
        if (UtilFunctions.isValidStringOrArray(this._currentModel) === false) {
          this._currentModel = new Array<ActionRdsModel>();
        }
        this._currentModel.push(response);
        this.model = this._currentModel;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  getErrors(id: number): Promise<{resp: string}>  {
    return this._httpClient.get<{resp: string}>(`api/admin/aws/action-rds/errors/${id}`).toPromise();
  }

  delete(id: number): Promise<any>  {
    return this._httpClient.delete<ActionRdsModel>(`api/admin/aws/action-rds/${id}`).pipe(
      switchMap((response: ActionRdsModel) => {
        const index = this._currentModel.findIndex(x => x.id === id);
        if (index >= 0) {
          this._currentModel.splice(index, 1);
          this.model = this._currentModel;
        }
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  get(id: number): Promise<ActionRdsModel>  {
    return this._httpClient.get<ActionRdsModel>(`api/admin/aws/action-rds/${id}`).toPromise();
  }

  rerun(id: number): Promise<ActionRdsModel>  {
    return this._httpClient.put<ActionRdsModel>(`api/admin/aws/action-rds/rerun-failed/${id}`, null).pipe(
      switchMap((response: ActionRdsModel) => {
        const index = this._currentModel.findIndex(x => x.id === id);
        if (index >= 0) {
          this._currentModel[index] = response;
          this.model = this._currentModel;
        }
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  cancel(id: number): Promise<any>  {
    return this._httpClient.delete<any>(`api/admin/aws/action-rds/cancel/${id}`).pipe(
      switchMap((response) => {
        const index = this._currentModel.findIndex(x => x.id === id);
        if (index >= 0) {
          this._currentModel[index].conclusion = VersionConclusionEnum.cancelling;
          this.model = this._currentModel;
        }
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  status(id: number): EventSource {
    return new EventSource(`api/admin/aws/action-rds/status/${id}`);
  }

  statusSimulate(id: number): EventSource {
    return new EventSource(`api/admin/test/status/simulate/${id}`);
  }
}
