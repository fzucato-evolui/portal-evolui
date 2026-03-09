import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve, RouterStateSnapshot} from '@angular/router';
import {Observable, of} from 'rxjs';
import {HealthCheckerService} from './health-checker.service';
import {UtilFunctions} from '../../../shared/util/util-functions';

@Injectable({
  providedIn: 'root'
})
export class HealthCheckerResolver implements Resolve<any>
{
  /**
   * Constructor
   */
  constructor(
    private _service: HealthCheckerService
  )
  {
  }

  // -----------------------------------------------------------------------------------------------------
  // @ Public methods
  // -----------------------------------------------------------------------------------------------------

  /**
   * Use this resolver to resolve initial mock-api for the application
   *
   * @param route
   * @param state
   */
  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<any>
  {
    if (route.paramMap && UtilFunctions.isValidStringOrArray(route.paramMap.get("id")) === true) {
      return this._service.getById(route.paramMap.get("id"));
    } else {
      return of(null);
    }



  }
}
