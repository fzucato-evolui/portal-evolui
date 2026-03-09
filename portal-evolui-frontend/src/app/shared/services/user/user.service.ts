import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, of, ReplaySubject} from 'rxjs';
import {parseInt} from "lodash-es";
import {PerfilUsuarioEnum, UsuarioModel} from "../../models/usuario.model";
import {UtilFunctions} from "../../util/util-functions";
import {catchError, switchMap} from "rxjs/operators";
import {RootService} from "../root/root.service";
import {ThemeService} from "../theme/theme.service";

@Injectable({
  providedIn: 'root'
})
export class UserService
{
  private _user: ReplaySubject<UsuarioModel> = new ReplaySubject<UsuarioModel>(1);
  private model: UsuarioModel;
  /**
   * Constructor
   */
  constructor(private _httpClient: HttpClient, public rootService: RootService, private _themeService: ThemeService)
  {
    this.rootService.model$.subscribe(value => {
      this.user = value.user;
    })
  }

  set accessToken(token: string)
  {
    if (UtilFunctions.isValidStringOrArray(token) === true) {
      localStorage.setItem('accessToken', token);
    } else {
      localStorage.removeItem('accessToken');
    }
  }

  get accessToken(): string
  {
    return localStorage.getItem('accessToken') ?? '';
  }

  // -----------------------------------------------------------------------------------------------------
  // @ Accessors
  // -----------------------------------------------------------------------------------------------------

  /**
   * Setter & getter for user
   *
   * @param value
   */
  set user(value: UsuarioModel)
  {
    this.model = value;
    // Store the value
    this._user.next(value);
    // Apply theme from user config
    if (value && value.config) {
      this._themeService.apply(value.config);
    }
  }

  get user$(): Observable<UsuarioModel>
  {
    //console.log(this.model);
    return this._user.asObservable();
  }

  // -----------------------------------------------------------------------------------------------------
  // @ Public methods
  // -----------------------------------------------------------------------------------------------------

  hasAnyAuthority(authorities: any[], user: UsuarioModel): boolean {
    if(authorities.length === 0 && UtilFunctions.isValidObject(user)) {
      return true;
    }

    const keyPair = this.perfilToKeyPair();
    for (let i = 0; i < authorities.length; i++) {
      const index = keyPair.findIndex(x =>
        x.key === authorities[i] || x.value === authorities[i] || parseInt(x.value) === authorities[i]
      );
      if(index >= 0) {
        if (keyPair[index].key === user.profile) {
          return true;
        }
      }
    }
    return false;
  }

  perfilToKeyPair(): Array<{key: string, value: string}> {
    let keys = Object.keys(PerfilUsuarioEnum);
    //console.log(keys);
    //keys = keys.slice(0, keys.length / 2);

    const keyPair = [];
    for (let i = 0; i < keys.length; i++) {
      const enumMember = keys[i];
      keyPair.push({key: enumMember, value: PerfilUsuarioEnum[enumMember]});
    }
    return keyPair;
  }

  signIn(credentials: UsuarioModel): Observable<any>
  {

    return this._httpClient.post('/api/public/auth/login', credentials).pipe(
      switchMap((response: any) => {

        // Store the access token in the local storage
        this.accessToken = response.accessToken;

        // Store the user on the user service
        this.user = response.user;
        this.rootService.user = response.user;

        // Return a new observable with the response
        return of(response);
      })
    );
  }

  getLoggedUser(): Observable<UsuarioModel>
  {
    // Renew token
    return this._httpClient.get('/api/public/auth/user').pipe(
      catchError(() =>

        // Return false
        of(null)
      ),
      switchMap((response: any) => {

        // Store the user on the user service
        this.user = response;

        // Return true
        return of(response);
      })
    );
  }

  signOut(): Observable<any>
  {
    // Remove the access token from the local storage
    this.accessToken = null;
    this.rootService.user = null;
    this._themeService.clearStorage();
    this._themeService.apply({scheme: 'light', theme: 'default'});
    this.model = null;
    this._user.next(null);
    return of(true);
  }
}
