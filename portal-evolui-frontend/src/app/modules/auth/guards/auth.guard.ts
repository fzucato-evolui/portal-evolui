import {Injectable} from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  CanActivateChild,
  CanLoad,
  Data,
  Route,
  Router,
  RouterStateSnapshot,
  UrlSegment,
  UrlTree
} from '@angular/router';
import {Observable, of} from 'rxjs';
import {switchMap} from 'rxjs/operators';
import {UtilFunctions} from "../../../shared/util/util-functions";
import {PerfilUsuarioEnum, UsuarioModel} from "../../../shared/models/usuario.model";
import {UserService} from "../../../shared/services/user/user.service";

@Injectable({
    providedIn: 'root'
})
export class AuthGuard implements CanActivate, CanActivateChild, CanLoad
{
    /**
     * Constructor
     */
    constructor(
        private _userService: UserService,
        private _router: Router
    )
    {
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Public methods
    // -----------------------------------------------------------------------------------------------------

    /**
     * Can activate
     *
     * @param route
     * @param state
     */
    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean
    {
        const redirectUrl = state.url === '/sign-out' ? '/' : state.url;
        return this._check(redirectUrl, route.data);
    }

    /**
     * Can activate child
     *
     * @param childRoute
     * @param state
     */
    canActivateChild(childRoute: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree
    {
        const redirectUrl = state.url === '/sign-out' ? '/' : state.url;
        return this._check(redirectUrl, childRoute.data);
    }

    /**
     * Can load
     *
     * @param route
     * @param segments
     */
    canLoad(route: Route, segments: UrlSegment[]): Observable<boolean> | Promise<boolean> | boolean
    {
        return this._check('/', route.data);
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Private methods
    // -----------------------------------------------------------------------------------------------------

    /**
     * Check the authenticated status
     *
     * @param redirectURL
     * @private
     */
    private _check(redirectURL: string, data: Data): Observable<boolean>
    {
        // Check the authentication status
        return this._userService.user$
                   .pipe(
                       switchMap((user) => {


                           // If the user is not authenticated...
                           if ( !user )
                           {
                               // Redirect to the sign-in page
                               this._router.navigate(['sign-in'], {queryParams: {redirectURL}});

                               // Prevent the access
                               return of(false);
                           } else {
                             if (data.hasOwnProperty("user")) {
                               (data["user"] as UsuarioModel).id = user.id;
                             }
                             if (UtilFunctions.isValidObject(data["roles"])) {
                               const roles: Array<PerfilUsuarioEnum> = data["roles"];
                               const has = this._userService.hasAnyAuthority(roles, user);
                               if (has === false) {
                                 this._router.navigate(['/']);
                               }
                               return of(has);
                             } else {

                               // Allow the access
                               return of(true);
                             }
                           }
                       })
                   );
    }
}
