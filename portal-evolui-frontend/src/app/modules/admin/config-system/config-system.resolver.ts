import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve, RouterStateSnapshot} from '@angular/router';
import {Observable} from 'rxjs';
import {ConfigSystemService} from "./config-system.service";

@Injectable({
    providedIn: 'root'
})
export class ConfigSystemResolver implements Resolve<any>
{
    /**
     * Constructor
     */
    constructor(
        private _service: ConfigSystemService
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
        return this._service.getInitialData();

    }
}
