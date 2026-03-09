import {Component, EventEmitter, Input, Output, ViewEncapsulation} from '@angular/core';
import {PerfilUsuarioEnum, UsuarioFilterModel} from "../../../../../shared/models/usuario.model";
import {UtilFunctions} from "../../../../../shared/util/util-functions";

@Component({
    selector       : 'users-list-filter',
    templateUrl    : './users-list-filter.component.html',
    styleUrls      : ['./users-list-filter.component.scss'],
    encapsulation  : ViewEncapsulation.None,

    standalone: false
})
export class UsersListFilterComponent
{
    @Output()
    onFilter: EventEmitter<UsuarioFilterModel> = new EventEmitter<UsuarioFilterModel>();

    private _model: UsuarioFilterModel = new UsuarioFilterModel();

    @Input()
    get model(): UsuarioFilterModel | null {
        return this._model;
    }
    set model(val: UsuarioFilterModel | null) {
        if (UtilFunctions.isValidObject(val) === true) {
            this._model = val;
        }
    }

    @Input()
    defaultFilters: UsuarioFilterModel = null;

    PerfilUsuarioEnum = PerfilUsuarioEnum;
    /**
     * Constructor
     */
    constructor()
    {
    }

    filtrar() {
      let model = this._model ? this._model : new UsuarioFilterModel();
      if (UtilFunctions.isValidStringOrArray(this.defaultFilters) === true) {
        model = Object.assign({}, this._model, this.defaultFilters);
      }
      this.onFilter.emit(model);
    }

    limpar() {
        this.model = new UsuarioFilterModel();
    }

}
