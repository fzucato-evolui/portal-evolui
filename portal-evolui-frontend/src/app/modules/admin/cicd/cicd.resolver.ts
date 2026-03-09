import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve, RouterStateSnapshot} from '@angular/router';
import {Observable} from 'rxjs';
import {CicdService} from './cicd.service';
import {ProjectService} from '../project/project.service';
import {mergeMap, take} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class CICDResolver implements Resolve<any>
{
  /**
   * Constructor
   */
  constructor(
    private _service: CicdService,
    private _projectsService: ProjectService
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
    const target = route.url[1].path;
    return this._projectsService.model$.pipe(take(1),
      mergeMap(response => {
        const index = response.findIndex(x=> x.identifier === target);
        if (index < 0) {
          throw new Error("Not Found");
        } else {
          this._service.target = response[index];
          return response;
        }
      })
    );

  }
}
