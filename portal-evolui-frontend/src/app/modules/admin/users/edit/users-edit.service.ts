import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, of, ReplaySubject} from 'rxjs';
import {tap} from 'rxjs/operators';
import {UsuarioModel} from "../../../../shared/models/usuario.model";

@Injectable({
    providedIn: 'root'
})
export class UsersEditService
{
    private _model: ReplaySubject<UsuarioModel> = new ReplaySubject<UsuarioModel>(1);

    /**
     * Constructor
     */
    constructor(private _httpClient: HttpClient)
    {

    }

    // -----------------------------------------------------------------------------------------------------
    // @ Accessors
    // -----------------------------------------------------------------------------------------------------

    /**
     * Getter for navigation
     */
    get model$(): Observable<UsuarioModel>
    {
        return this._model.asObservable();
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Public methods
    // -----------------------------------------------------------------------------------------------------

    /**
     * Get all navigation data
     */
    get(id: string): Observable<UsuarioModel>
    {
        if (id === '-1') {
            const model = new UsuarioModel();
            this._model.next(model);
            return of(model);
        } else {
            return this._httpClient.get<UsuarioModel>('api/angular/admin/cadastros/usuario/get/' + id).pipe(
                tap((model) => {

                    this._model.next(model);

                })
            );
        }
    }

    save(model: UsuarioModel): Promise<UsuarioModel>
    {
        return this._httpClient.post<UsuarioModel>('api/admin/users', model).toPromise();
    }
}
