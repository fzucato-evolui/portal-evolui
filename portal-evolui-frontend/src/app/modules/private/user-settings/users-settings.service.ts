import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {UserConfigModel, UsuarioModel} from "../../../shared/models/usuario.model";

@Injectable({
  providedIn: 'root'
})
export class UsersSettingsService
{
  /**
   * Constructor
   */
  constructor(private _httpClient: HttpClient)
  {

  }

  save(model: UsuarioModel): Promise<{user: UsuarioModel, accessToken: string}>
  {
    return this._httpClient.post<any>('/api/private/user', model).toPromise();
  }

  changePassword(model: UsuarioModel): Promise<UsuarioModel>
  {
    return this._httpClient.post<UsuarioModel>('/api/private/user/change-password', model).toPromise();
  }

  saveConfig(config: UserConfigModel): Promise<UsuarioModel>
  {
    return this._httpClient.post<UsuarioModel>('/api/private/user/config', config).toPromise();
  }
}
