import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {
  GeracaoVersaoDiffModel,
  GeracaoVersaoFilterModel,
  GeracaoVersaoModel,
  VersionGenerationRequestModel
} from '../../../shared/models/geracao-versao.model';
import {Observable, of, ReplaySubject} from 'rxjs';
import {switchMap} from 'rxjs/operators';
import {AvailableVersions, VersaoModel} from 'app/shared/models/version.model';
import {ProjectModel} from '../../../shared/models/project.model';
import {BranchesAndTagsDetailModel} from '../../../shared/models/github.model';

@Injectable({
  providedIn: 'root'
})
export class GeracaoVersaoService
{
  private baseRestUrl = 'api/admin/geracao-versao';
  private _target: ReplaySubject<ProjectModel> = new ReplaySubject<ProjectModel>(1);
  private _currentTarget: ProjectModel;
  private _model: ReplaySubject<Array<GeracaoVersaoModel>> = new ReplaySubject<Array<GeracaoVersaoModel>>(1);
  private _currentModel: Array<GeracaoVersaoModel>;
  private _canGenerateVersion: ReplaySubject<boolean> = new ReplaySubject<boolean>(1);
  private _branchesCache: Map<string, BranchesAndTagsDetailModel> = new Map();

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

  set model(value: Array<GeracaoVersaoModel>)
  {
    this._currentModel = value;
    // Store the value
    this._model.next(value);
  }

  get model$(): Observable<Array<GeracaoVersaoModel>>
  {
    //console.log(this.model);
    return this._model.asObservable();
  }
  set canGenerateVersion(value: boolean)
  {
    // Store the value
    this._canGenerateVersion.next(value);
  }

  get canGenerateVersion$(): Observable<boolean>
  {
    //console.log(this.model);
    return this._canGenerateVersion.asObservable();
  }
  /**
   * Constructor
   */
  constructor(private _httpClient: HttpClient) {
  }

  getAll(): Promise<Array<GeracaoVersaoModel>>  {
    return this._httpClient.get<Array<GeracaoVersaoModel>>(`${this.baseRestUrl}/${this._currentTarget.identifier}/all`).pipe(
      switchMap((response: Array<GeracaoVersaoModel>) => {

        this.model = response;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  get(id: number): Promise<GeracaoVersaoModel>  {
    return this._httpClient.get<GeracaoVersaoModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}/${id}`).toPromise();
  }

  filter(model: GeracaoVersaoFilterModel): Promise<{canGenerate: boolean, rows: Array<GeracaoVersaoModel>}> {
    return this._httpClient.post<{canGenerate: boolean, rows: Array<GeracaoVersaoModel>}>(`${this.baseRestUrl}/${this._currentTarget.identifier}/filter`, model).pipe(
      switchMap((response: {canGenerate: boolean, rows: Array<GeracaoVersaoModel>}) => {

        this.model = response.rows;
        this.canGenerateVersion = response.canGenerate;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  canGenerate(target: string): Observable<boolean> {
    return this._httpClient.get<boolean>(`${this.baseRestUrl}/${target}/can-generate`).pipe(
      switchMap((response: boolean) => {

        this.model = [];
        this.canGenerateVersion = response;

        // Return a new observable with the response
        return of(response);
      }));
  }

  save(model: VersionGenerationRequestModel): Promise<GeracaoVersaoModel> {
    return this._httpClient.post<GeracaoVersaoModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}`, model).pipe(
      switchMap((response: GeracaoVersaoModel) => {

        this._currentModel.push(response);
        this.model = this._currentModel;
        this.canGenerateVersion = false;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  cancel(id: number): Promise<GeracaoVersaoModel>  {
    return this._httpClient.get<GeracaoVersaoModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}/cancel/${id}`).pipe(
      switchMap((response: GeracaoVersaoModel) => {
        const index = this._currentModel.findIndex(x => x.id === id);
        if (index >= 0) {
          this._currentModel[index] = response;
          this.model = this._currentModel;
          this.canGenerateVersion = false;
        }
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  rerunFailed(id: number): Promise<GeracaoVersaoModel>  {
    return this._httpClient.get<GeracaoVersaoModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}/rerun-failed/${id}`).pipe(
      switchMap((response: GeracaoVersaoModel) => {
        const index = this._currentModel.findIndex(x => x.id === id);
        if (index >= 0) {
          this._currentModel[index] = response;
          this.model = this._currentModel;
          this.canGenerateVersion = false;
        }
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  delete(id: number): Promise<any>  {
    return this._httpClient.delete<GeracaoVersaoModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}/${id}`).pipe(
      switchMap((response: GeracaoVersaoModel) => {
        const index = this._currentModel.findIndex(x => x.id === id);
        if (index >= 0) {
          this._currentModel.splice(index, 1);
          this.model = this._currentModel;
        }
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  getLogs(id: number): Promise<Array<GeracaoVersaoModel>>  {
    return this._httpClient.get<Array<GeracaoVersaoModel>>(`${this.baseRestUrl}/${this._currentTarget.identifier}/logs/${id}`).toPromise();
  }

  getLink(workflow: number): Promise<{resp: string}>  {
    return this._httpClient.get<{resp: string}>(`${this.baseRestUrl}/${this._currentTarget.identifier}/link/${workflow}`).toPromise();
  }

  getMondayLink(id: string): Promise<{resp: string}>  {
    return this._httpClient.get<{resp: string}>(`${this.baseRestUrl}/${this._currentTarget.identifier}/monday-link/${id}`).toPromise();
  }

  getBranches(): Promise<AvailableVersions>  {
    return this._httpClient.get<AvailableVersions>(`${this.baseRestUrl}/${this._currentTarget.identifier}/branches`).toPromise();
  }

  getBranchesAndTags(moduleId: number): Promise<BranchesAndTagsDetailModel>  {
    return this._httpClient.get<BranchesAndTagsDetailModel>(`${this.baseRestUrl}/branches/${moduleId}`).toPromise();
  }

  getBranchesAndTagsCached(moduleId: number, repository: string): Promise<BranchesAndTagsDetailModel> {
    if (this._branchesCache.has(repository)) {
      return Promise.resolve(this._branchesCache.get(repository));
    }
    return this.getBranchesAndTags(moduleId).then(result => {
      this._branchesCache.set(repository, result);
      return result;
    });
  }

  clearBranchesCache(): void {
    this._branchesCache.clear();
  }

  getAvailableBranches(): Promise<Array<VersaoModel>>  {
    return this._httpClient.get<Array<VersaoModel>>(`${this.baseRestUrl}/${this._currentTarget.identifier}/available-branches`).toPromise();
  }

  getDiff(idFrom: number, idTo: number): Promise<GeracaoVersaoDiffModel>  {
    return this._httpClient.get<GeracaoVersaoDiffModel>(`${this.baseRestUrl}/${this._currentTarget.identifier}/diff/${idFrom}/${idTo}`).toPromise();
  }
}
