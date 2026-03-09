import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, ReplaySubject} from 'rxjs';
import {tap} from 'rxjs/operators';
import {MatDialog} from "@angular/material/dialog";
import {UsuarioFilterModel, UsuarioModel} from "../../../../shared/models/usuario.model";
import {UsersListModalComponent} from "./modal/users-list-modal.component";

@Injectable({
    providedIn: 'root'
})
export class UsuarioListService
{
    private _model: ReplaySubject<UsuarioModel> = new ReplaySubject<UsuarioModel>(1);

    /**
     * Constructor
     */
    constructor(private _httpClient: HttpClient, private _matDialog: MatDialog)
    {

    }

    filtrar(model: UsuarioFilterModel): Promise<Array<UsuarioModel>>
    {
        return this._httpClient.post<Array<UsuarioModel>>('api/admin/users/filter', model).toPromise();
    }

    delete(id: number): Promise<any>
    {
        return this._httpClient.delete('api/admin/users/'+ id).toPromise();
    }

    public showDialog(filter: UsuarioFilterModel, defaulFilters: UsuarioFilterModel, showTableButtons: boolean, showTableFastFilter): Observable<UsuarioModel> {
        let modal = this._matDialog.open(UsersListModalComponent, { disableClose: true, panelClass: 'usuario-modal-container' });
        modal.componentInstance.preFilter = filter;
      modal.componentInstance.defaultFilters = defaulFilters;
        modal.componentInstance.showTableButtons = showTableButtons;
        modal.componentInstance.showTableFastFilter = showTableFastFilter;
        return modal.componentInstance.onDataSelected.pipe(
            tap((resp) => {
                modal.close(resp);
            },err => {
                console.error("pipe error", err);
                throw err;
            })
        );
    }

    public showMultipleDialog(filter: UsuarioFilterModel, defaulFilters: UsuarioFilterModel, showTableButtons: boolean, showTableFastFilter): Observable<Array<UsuarioModel>> {
        let modal = this._matDialog.open(UsersListModalComponent, { disableClose: true, panelClass: 'usuario-modal-container' });
        modal.componentInstance.preFilter = filter;
        modal.componentInstance.defaultFilters = defaulFilters;
        modal.componentInstance.showTableButtons = showTableButtons;
        modal.componentInstance.showTableFastFilter = showTableFastFilter;
        modal.componentInstance.multiple = true;

        return modal.componentInstance.onDataMultipleSelected.pipe(
            tap((resp) => {
                modal.close(resp);
            },err => {
                console.error("pipe error", err);
                throw err;
            })
        );

    }

}
