import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, of, ReplaySubject} from 'rxjs';
import {switchMap} from 'rxjs/operators';
import {HomeModel} from '../../../shared/models/home.model';

@Injectable({
  providedIn: 'root'
})
export class HomeService
{
  private baseRestUrl = 'api/admin/home';

  public static githubCommitDays = 30;
  public static beanDays = 15;
  public static cicdDays = 15;
  private _model: ReplaySubject<HomeModel> = new ReplaySubject<HomeModel>(1);
  private _currentModel: HomeModel;



   set model(value: HomeModel)
  {
    this._currentModel = value;
    // Store the value
    this._model.next(value);
  }

  get model$(): Observable<HomeModel>
  {
    //console.log(this.model);
    return this._model.asObservable();
  }

  /**
   * Constructor
   */
  constructor(private _httpClient: HttpClient) {
  }

  get(): Observable<HomeModel>  {
    return this._httpClient.get<HomeModel>(`${this.baseRestUrl}?githubCommitDays=${(HomeService.githubCommitDays)}&beanDays=${HomeService.beanDays}&&cicdDays=${HomeService.cicdDays}`).pipe(
      switchMap((response: HomeModel) => {

        this.model = response;
        // Return a new observable with the response
        return of(response);
      }));
  }

}
