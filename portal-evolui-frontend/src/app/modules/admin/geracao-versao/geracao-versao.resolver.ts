import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve, RouterStateSnapshot} from '@angular/router';
import {forkJoin, Observable} from 'rxjs';
import {GeracaoVersaoService} from './geracao-versao.service';
import {ProjectService} from '../project/project.service';
import {mergeMap, take} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class GeracaoVersaoResolver implements Resolve<any>
{
  /**
   * Constructor
   */
  constructor(
    private _service: GeracaoVersaoService,
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
    const target = route.url[1].path;
    const obs1 = this._produtosService.model$.pipe(take(1),
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
    const obs2 = this._service.canGenerate(target);
    return forkJoin([obs1, obs2]);

  }
}
