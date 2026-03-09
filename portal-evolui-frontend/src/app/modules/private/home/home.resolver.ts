import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve, RouterStateSnapshot} from '@angular/router';
import {Observable} from 'rxjs';
import {HomeService} from './home.service';

@Injectable({
  providedIn: 'root'
})
export class HomeResolver implements Resolve<any>
{
  /**
   * Constructor
   */
  constructor(
    private _service: HomeService
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
    return this._service.get();
  }
}
