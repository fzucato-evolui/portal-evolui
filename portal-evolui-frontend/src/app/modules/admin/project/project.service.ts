import {Injectable} from '@angular/core';
import {Observable, of, ReplaySubject} from 'rxjs';
import {switchMap} from 'rxjs/operators';
import {UtilFunctions} from '../../../shared/util/util-functions';
import {ProjectModel} from 'app/shared/models/project.model';
import {HttpClient} from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class ProjectService
{
  private baseRestUrl = 'api/admin/project';
  private _model: ReplaySubject<Array<ProjectModel>> = new ReplaySubject<Array<ProjectModel>>(1);
  private _currentModel: Array<ProjectModel>;

  set model(value: Array<ProjectModel>)
  {
    this._currentModel = value;
    // Store the value
    this._model.next(value);
  }

  get model$(): Observable<Array<ProjectModel>>
  {
    //console.log(this.model);
    return this._model.asObservable();
  }

  /**
   * Constructor
   */
  constructor(private _httpProject: HttpClient) {
  }

  getAll(): Promise<Array<ProjectModel>>  {
    return this._httpProject.get<Array<ProjectModel>>(`${this.baseRestUrl}/all`).pipe(
      switchMap((response: Array<ProjectModel>) => {

        this.model = response;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  get(id: number): Promise<ProjectModel>  {
    return this._httpProject.get<ProjectModel>(`${this.baseRestUrl}/${id}`).toPromise();
  }

  save(model: ProjectModel): Promise<ProjectModel> {
    return this._httpProject.post<ProjectModel>(`${this.baseRestUrl}`, model).pipe(
      switchMap((response: ProjectModel) => {
        console.log(this._currentModel);
        if (UtilFunctions.isValidStringOrArray(this._currentModel) === false) {
          this._currentModel = new Array<ProjectModel>();
        }
        if (model.id && model.id > 0) {
          const index = this._currentModel.findIndex(x => x.id === model.id);
          if (index < 0) {
            this._currentModel.push(response);
          } else {
            this._currentModel[index] = response;
          }
        } else {
          this._currentModel.push(response);
        }
        this.model = this._currentModel;
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  delete(id: number): Promise<ProjectModel>  {
    return this._httpProject.delete<ProjectModel>(`${this.baseRestUrl}/${id}`).pipe(
      switchMap((response: ProjectModel) => {
        const index = this._currentModel.findIndex(x => x.id === id);
        if (index >= 0) {
          this._currentModel.splice(index, 1);
          this.model = this._currentModel;
        }
        // Return a new observable with the response
        return of(response);
      })).toPromise();
  }

  getRepoStructure(repository: string) {
    return this._httpProject.get<ProjectModel>(`${this.baseRestUrl}/structure/${repository}`).toPromise();
  }
}
