import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve, RouterStateSnapshot} from '@angular/router';
import {from, Observable, of} from 'rxjs';
import {ProjectService} from './project.service';
import {UserService} from '../../../shared/services/user/user.service';
import {PerfilUsuarioEnum} from '../../../shared/models/usuario.model';
import {mergeMap, take} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ProjectResolver implements Resolve<any>
{
  /**
   * Constructor
   */
  constructor(
    private _service: ProjectService,
    private _userService: UserService,
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
    return this._userService.user$.pipe(take(1),
      mergeMap(response => {
        if (response.profile !== PerfilUsuarioEnum.ROLE_USER) {
          return from(this._service.getAll());
        } else {
          return of(null);
        }
      })
    );


  }
}
