import {Component, EventEmitter, Output, ViewChild, ViewEncapsulation} from "@angular/core";
import {UsuarioModel} from "../../../../../shared/models/usuario.model";
import {UsersEditFormComponent} from "../form/users-edit-form.component";

@Component({
    selector       : 'users-edit-modal',
    styleUrls      : ['/users-edit-modal.component.scss'],
    templateUrl    : './users-edit-modal.component.html',
    encapsulation  : ViewEncapsulation.None,

    standalone: false
})
export class UsersEditModalComponent
{
  @ViewChild('form', {static: false}) formSave: UsersEditFormComponent;
  @Output()
  onSave: EventEmitter<UsuarioModel> = new EventEmitter<UsuarioModel>();

    public model: UsuarioModel;
    constructor()
    {
    }

  save(model: UsuarioModel) {
    this.onSave.emit(model);
  }

  doSaving() {
      this.formSave.salvar();
  }

  canSave(): boolean {
      if (this.formSave) {
        return this.formSave.canSave();
      }
      return false;
  }

}
