import {Injectable} from '@angular/core';
import {Observable, of, ReplaySubject} from 'rxjs';
import {switchMap, tap} from 'rxjs/operators';
import {HttpClient} from '@angular/common/http';
import {UsuarioModel} from '../../../shared/models/usuario.model';
import {CertificateModel} from '../../../shared/models/certificate.model';
import {
  HealthCheckerAlertModel,
  HealthCheckerConfigModel,
  HealthCheckerModel
} from '../../../shared/models/health-checker.model';

@Injectable({
  providedIn: 'root'
})
export class HealthCheckerService
{
  private baseRestUrl = 'api/admin/health-checker';
  private _model: ReplaySubject<Array<HealthCheckerModel>> = new ReplaySubject<Array<HealthCheckerModel>>(1);
  private _currentModel: Array<HealthCheckerModel>;

  set model(value: Array<HealthCheckerModel>)
  {
    this._currentModel = value;
    // Store the value
    this._model.next(value);
  }

  get model$(): Observable<Array<HealthCheckerModel>>
  {
    return this._model.asObservable();
  }

  /**
   * Constructor
   */
  constructor(private _httpClient: HttpClient) {
  }

  getAll(): Promise<Array<HealthCheckerModel>>  {
    return this._httpClient.get<Array<HealthCheckerModel>>(`${this.baseRestUrl}/all`).pipe(
      switchMap((response: Array<HealthCheckerModel>) => {

        this.model = response;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  getById(id: any): Observable<HealthCheckerModel>  {
    return this._httpClient.get<HealthCheckerModel>(`${this.baseRestUrl}/${id}`).pipe(
      tap((resp) => {
        const value = [];
        value.push(resp);
        // Store the value
        this.model = value;
      },err => {
        console.error("pipe error", err);
        throw err;
      })
    );

  }

  get(id: number): Promise<HealthCheckerModel>  {
    return this._httpClient.get<HealthCheckerModel>(`${this.baseRestUrl}/${id}`).toPromise();
  }

  getPossibleUsers(): Promise<Array<UsuarioModel>>  {
    return this._httpClient.get<Array<UsuarioModel>>(`${this.baseRestUrl}/possible-users`).toPromise();
  }

  save(model: HealthCheckerConfigModel): Promise<HealthCheckerConfigModel> {
    return this._httpClient.post<HealthCheckerConfigModel>(`${this.baseRestUrl}`, model).toPromise();
  }

  delete(id: number): Promise<HealthCheckerModel>  {
    return this._httpClient.delete<HealthCheckerModel>(`${this.baseRestUrl}/${id}`).pipe(
      switchMap((response: HealthCheckerModel) => {
        const index = this._currentModel.findIndex(x => x.id === id);
        if (index >= 0) {
          this._currentModel.splice(index, 1);
          this.model = this._currentModel;
        }
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  generateToken(model: UsuarioModel): Promise<{ token: string, endpoint: string, online: boolean }>  {
    return this._httpClient.post<{ token: string, endpoint: string, online: boolean }>(`${this.baseRestUrl}/generate-token`, model).toPromise();
  }

  testCertificate(model: CertificateModel): Promise<any>  {
    return this._httpClient.post<{ token: string, endpoint: string }>(`${this.baseRestUrl}/test-certificate`, model).toPromise();
  }

  getAlerts(id: number, module: boolean): Promise<Array<HealthCheckerAlertModel>>  {
    const path = module === false ? 'alerts' : 'alerts-module';
    return this._httpClient.get<Array<HealthCheckerAlertModel>>(`${this.baseRestUrl}/${path}/${id}`).toPromise();
  }
}
