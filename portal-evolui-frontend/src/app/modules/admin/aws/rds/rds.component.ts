import {ChangeDetectorRef, Component, OnInit, ViewEncapsulation} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {RdsService} from "./rds.service";
import {ClassyLayoutComponent} from 'app/layout/layouts/classy/classy.component';
import {RDSModel} from "../../../../shared/models/rds.model";
import {UtilFunctions} from "../../../../shared/util/util-functions";
import {RdsTableComponent} from "./table/rds-table.component";
import {MessageDialogService} from "../../../../shared/services/message/message-dialog-service";


@Component({
  selector     : 'rds-list',
  templateUrl  : './rds.component.html',
  styleUrls      : ['./rds.component.scss'],
  encapsulation: ViewEncapsulation.None,

  standalone: false
})
export class RdsComponent implements OnInit
{
  dataSource: {[key: string]: MatTableDataSource<RDSModel>};

  get drawerMode(): 'over' | 'side' {
    return this._parent.mobileQuery.matches ? 'over' : 'side';
  };
  get drawerOpened(): boolean {
    return this._parent.mobileQuery.matches === false;
  };

  constructor(
    private _parent: ClassyLayoutComponent,
    private _service: RdsService,
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
            this.dataSource[x] = new MatTableDataSource<RDSModel>();
            this.dataSource[x].data = value[x];
            this.dataSource[x]._updateChangeSubscription();
          })
          this._changeDetectorRef.detectChanges();
        }
      }).catch(error => {
      console.error(error);
    });
  }

  start(row: RDSModel, table: RdsTableComponent) {
    {
      this._messageService.open('Deseja realmente iniciar a instância?', 'CONFIRMAÇÃO', 'confirm').subscribe((result) => {
        if (result === 'confirmed') {
          this._service.start(row)
            .then(value => {
              let d = this.dataSource[row.account];
              const index = d.data.findIndex(r => r.id === row.id);
              if (index >= 0) {
                row.instanceState = 'pending';
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

  stop(row: RDSModel, table: RdsTableComponent) {
    this._messageService.open('Deseja realmente parar a instância?', 'CONFIRMAÇÃO', 'confirm').subscribe((result) => {
      if (result === 'confirmed') {
        this._service.stop(row)
          .then(value => {
            let d = this.dataSource[row.account];
            const index = d.data.findIndex(r => r.id === row.id);
            if (index >= 0) {
              row.instanceState = 'pending';
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


}
