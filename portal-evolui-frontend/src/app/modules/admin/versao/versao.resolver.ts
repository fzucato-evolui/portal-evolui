import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve, RouterStateSnapshot} from '@angular/router';
import {Observable} from 'rxjs';
import {VersaoService} from './versao.service';
import {TargetType} from '../../../shared/models/enum.model';
import {ProjectService} from '../project/project.service';
import {mergeMap, take} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class VersaoResolver implements Resolve<any>
{
  /**
   * Constructor
   */
  constructor(
    private _service: VersaoService,
    private _produtosService: ProjectService
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
    const target = route.url[1].path as TargetType;
    return this._produtosService.model$.pipe(take(1),
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
