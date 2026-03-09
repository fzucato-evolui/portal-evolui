import {ChangeDetectorRef, Component, OnInit, ViewEncapsulation} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {WorkspaceService} from "./workspace.service";
import {ClassyLayoutComponent} from 'app/layout/layouts/classy/classy.component';
import {UtilFunctions} from "../../../../shared/util/util-functions";
import {WorkspaceTableComponent} from "./table/workspace-table.component";
import {MessageDialogService} from "../../../../shared/services/message/message-dialog-service";
import {WorkspaceModel} from '../../../../shared/models/workspace.model';


@Component({
  selector     : ' workspace-list',
  templateUrl  : './workspace.component.html',
  styleUrls      : ['./workspace.component.scss'],
  encapsulation: ViewEncapsulation.None,

  standalone: false
})
export class WorkspaceComponent implements OnInit
{
  dataSource: {[key: string]: MatTableDataSource<WorkspaceModel>};

  get drawerMode(): 'over' | 'side' {
    return this._parent.mobileQuery.matches ? 'over' : 'side';
  };
  get drawerOpened(): boolean {
    return this._parent.mobileQuery.matches === false;
  };

  constructor(
    private _parent: ClassyLayoutComponent,
    private _service: WorkspaceService,
    private _changeDetectorRef: ChangeDetectorRef,
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
  ngOnInit(): void  {
    this.refresh();

  }

  refresh() {
    this._service.getAll()
      .then(value => {
        this.dataSource = {};
        if (UtilFunctions.isValidStringOrArray(value) === true) {
          Object.keys(value).forEach(x => {
            this.dataSource[x] = new MatTableDataSource<WorkspaceModel>();
            this.dataSource[x].data = value[x];
            this.dataSource[x]._updateChangeSubscription();
          })

        }
        this._changeDetectorRef.detectChanges();
      }).catch(error => {
      console.error(error);
    });
  }

  start(row:  WorkspaceModel, table: WorkspaceTableComponent) {
    {
      this._messageService.open('Deseja realmente iniciar a wokspace?', 'CONFIRMAÇÃO', 'confirm').subscribe((result) => {
        if (result === 'confirmed') {
          this._service.start(row)
            .then(value => {
              let d = this.dataSource[row.account];

              const index = d.data.findIndex(r => r.id === row.id);
              if (index >= 0) {
                row.state = 'pending';
                d.data[index] = row;
                table.update();
              }
              this._messageService.open('Requisição de start foi enviada com sucesso', 'SUCESSO', 'success');
            }).catch(error => {
            console.error(error);
          });

        }
      });

    }
  }

  stop(row:  WorkspaceModel, table: WorkspaceTableComponent) {
    this._messageService.open('Deseja realmente parar a workspace?', 'CONFIRMAÇÃO', 'confirm').subscribe((result) => {
      if (result === 'confirmed') {
        this._service.stop(row)
          .then(value => {
            let d = this.dataSource[row.account];

            const index = d.data.findIndex(r => r.id === row.id);
            if (index >= 0) {
              row.state = 'pending';
              d.data[index] = row;
              table.update();
            }
            this._messageService.open('Requisição de stop foi enviada com sucesso', 'SUCESSO', 'success');
          }).catch(error => {
          console.error(error);
        });

      }
    });

  }


  reboot(row: WorkspaceModel, table: WorkspaceTableComponent) {
    this._messageService.open('Deseja realmente reiniciar a workspace?', 'CONFIRMAÇÃO', 'confirm').subscribe((result) => {
      if (result === 'confirmed') {
        this._service.reboot(row)
          .then(value => {
            let d = this.dataSource[row.account];

            const index = d.data.findIndex(r => r.id === row.id);
            if (index >= 0) {
              row.state = 'pending';
              d.data[index] = row;
              table.update();
            }
            this._messageService.open('Requisição de reboot foi enviada com sucesso', 'SUCESSO', 'success');
          }).catch(error => {
          console.error(error);
        });

      }
    });
  }
}
