import {Component, OnInit, ViewEncapsulation} from '@angular/core';
import {ClassyLayoutComponent} from "../../../../layout/layouts/classy/classy.component";
import {MatTableDataSource} from "@angular/material/table";
import {UsuarioFilterModel, UsuarioModel} from "../../../../shared/models/usuario.model";
import {UtilFunctions} from "../../../../shared/util/util-functions";
import {UsuarioListService} from "./usuario-list.service";
import {tap} from "rxjs/operators";
import {MatDialog} from "@angular/material/dialog";
import {UsersEditModalComponent} from "../edit/modal/users-edit-modal.component";
import {MessageDialogService} from '../../../../shared/services/message/message-dialog-service';


@Component({
  selector     : 'users-list.component',
  templateUrl  : './users-list.component.html',
  styleUrls      : ['./users-list.component.scss'],
  encapsulation: ViewEncapsulation.None,

  standalone: false
})
export class UsersListComponent implements OnInit
{
  dataSource = new MatTableDataSource<UsuarioModel>();
  get drawerMode(): 'over' | 'side' {
    return this._parent.mobileQuery.matches ? 'over' : 'side';
  };
  get drawerOpened(): boolean {
    return this._parent.mobileQuery.matches === false;
  };

  constructor(
    private _parent: ClassyLayoutComponent,
    private _service: UsuarioListService,
    private _matDialog: MatDialog,
    private _messageService: MessageDialogService
  )
  {
  }

  // -----------------------------------------------------------------------------------------------------
  // @ Lifecycle hooks
  // -----------------------------------------------------------------------------------------------------

  /**
   * On init
   */
  ngOnInit(): void
  {



  }

  filtrar(filtro: UsuarioFilterModel) {
    this._service.filtrar(filtro)
      .then(value => {
        if (UtilFunctions.isValidStringOrArray(value) === true) {
          this.dataSource.data = value;
        } else {
          this.dataSource.data = [];
        }
        this.dataSource._updateChangeSubscription();
      }).catch(error => {
      console.error(error);
    });
  }

  delete(row: UsuarioModel) {
    this._messageService.open('Deseja realmente remover o usuário?', 'CONFIRMAÇÃO', 'confirm').subscribe((result) => {
      if (result === 'confirmed') {
        const index = this.dataSource.data.findIndex(r => r.id === row.id);
        if (index >= 0) {
          this._service.delete(row.id).then(value => {
            this.dataSource.data.splice(index,1);
            this.dataSource._updateChangeSubscription();
            this._messageService.open('Usuário removido com sucesso', 'SUCESSO', 'success');
          })
        }
      }
    });
  }
  edit(row: UsuarioModel) {
    let modal = this._matDialog.open(UsersEditModalComponent, { disableClose: true, panelClass: 'users-edit-modal-container' });
    modal.componentInstance.model = row;
    return modal.componentInstance.onSave.pipe(
      tap((resp) => {
        modal.close(resp);
      },err => {
        console.error("pipe error", err);
        throw err;
      })
    );
  }

  add() {
    let modal = this._matDialog.open(UsersEditModalComponent, { disableClose: true, panelClass: 'users-edit-modal-container' });
    modal.componentInstance.model = new UsuarioModel();
    return modal.componentInstance.onSave.pipe(
      tap((resp) => {
        modal.close(resp);
      },err => {
        console.error("pipe error", err);
        throw err;
      })
    );

  }

}
