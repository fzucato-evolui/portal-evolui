import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  Output,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {MatTable, MatTableDataSource} from "@angular/material/table";
import {Router} from "@angular/router";
import {MatSort} from "@angular/material/sort";
import {SelectionModel} from "@angular/cdk/collections";
import {LogAWSActionModel} from "../../../../../shared/models/log.aws.action.model";
import {ActionRdsModel, ActionRDSTypeEnum} from '../../../../../shared/models/action-rds.model';
import {UtilFunctions} from '../../../../../shared/util/util-functions';
import {ActionRdsComponent} from '../action-rds.component';
import {ActionRdsModalComponent} from '../modal/action-rds-modal.component';
import {RDSModel} from '../../../../../shared/models/rds.model';
import {MatDialog} from '@angular/material/dialog';
import {ActionRdsStatusModalComponent} from '../modal/action-rds-status-modal.component';

@Component({
  selector       : 'action-rds-table',
  templateUrl    : './action-rds-table.component.html',
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class ActionRdsTableComponent implements AfterViewInit
{
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild(MatTable) table: MatTable<any>;
  @Input()
  multiple = false
  @Input()
  showActionsButtons = true
  @Input()
  showFastFilter = true
  @Input()
  dataSource = new MatTableDataSource<ActionRdsModel>();
  @Output()
  onRefreshClicked: EventEmitter<any> = new EventEmitter();

  public selection = new SelectionModel<LogAWSActionModel>(true, []);
  readonly ActionRDSTypeEnum = ActionRDSTypeEnum;

  displayedColumns = [ 'buttons', 'actionType', 'image', 'user.name', 'user.email', 'status',
    'conclusion', 'schedulerDate', 'requestDate', 'conclusionDate', 'destinationDatabase', 'sourceDatabase', 'rds', 'dumpFile', 'excludeBlobs'];
  /**
   * Constructor
   */
  constructor(private _router: Router,
              private _changeDetectorRef: ChangeDetectorRef,
              private _matDialog: MatDialog,
              private _parentComponent: ActionRdsComponent)
  {
  }

  refresh() {
    //this._router.navigate(['/admin/users', -1]);
    this.onRefreshClicked.emit(null);
  }

  ngAfterViewInit(): void {
    this.dataSource.sort = this.sort;
  }

  announceSortChange($event) {
    //console.log($event);
  }

  fastFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
  }

  update() {
    const me = this;
    setTimeout(() =>{
      me.table.renderRows();
      me._changeDetectorRef.detectChanges();
    });
  }

  getJson(key: string, model: LogAWSActionModel) : any {
    return JSON.parse(model[key]);
  }
  clearImage(model: LogAWSActionModel) {
    model.user.image = 'assets/images/noPicture.png';
  }

  add(type: ActionRDSTypeEnum) {
    this._parentComponent.service.getAllRDSs()
      .then(value => {
        const modal = this._matDialog.open(ActionRdsModalComponent, { disableClose: true, panelClass: 'action-rds-modal-container' });
        modal.componentInstance.service = this._parentComponent.service;
        modal.componentInstance.dataSource = {};
        Object.keys(value).forEach(x => {
          modal.componentInstance.dataSource[x] = new MatTableDataSource<RDSModel>();
          modal.componentInstance.dataSource[x].data = value[x];
          modal.componentInstance.dataSource[x]._updateChangeSubscription();
        })
        const restoreModel = new ActionRdsModel();
        restoreModel.actionType = type;
        modal.componentInstance.model = restoreModel;
      })
  }

  getErrors(id) {
    this._parentComponent.service.getErrors(id).then(value => {
      if (UtilFunctions.isValidStringOrArray(value.resp) === false) {
        this._parentComponent.messageService.open('Nenhum erro encontrado.', 'ALERTA', 'warning');
      } else {
        this._parentComponent.messageService.open('<pre>'+ value.resp + '</pre>', 'INFO', 'info');
      }
    });
  }

  delete(id) {
    this._parentComponent.messageService.open('Deseja realmente remover o processo?', 'CONFIRMAÇÃO', 'confirm').subscribe((result) => {
      if (result === 'confirmed') {
        const index = this.dataSource.data.findIndex(r => r.id === id);
        if (index >= 0) {
          this._parentComponent.service.delete(id).then(value => {
            this._parentComponent.messageService.open('Processo removido com sucesso', 'SUCESSO', 'success');
          });
        }
      }
    });

  }

  viewLogs(model: ActionRdsModel) {
    const modal = this._matDialog.open(ActionRdsStatusModalComponent, { disableClose: true, panelClass: 'action-rds-status-modal-container' });
    modal.componentInstance.service = this._parentComponent.service;
    modal.componentInstance.id = model.id;
    modal.componentInstance.title = `Logs do ${model.actionType} ${model.id}`;
    modal.componentInstance.caller = this._parentComponent;
  }

  cancel(id) {
    this._parentComponent.service.cancel(id).then(value => {
      this._parentComponent.messageService.open('A requisição de cancelamento foi enviada com sucesso. Aguardando a confirmação do servidor...', 'INFO', 'info');
    });
  }

  rerunFailed(id) {
    this._parentComponent.service.rerun(id).then(value => {
      this._parentComponent.messageService.open('A requisição do reprocessamento foi enviada com sucesso. Aguardando a confirmação do servidor...', 'INFO', 'info');
    });
  }

  clone(id) {
    Promise.all([this._parentComponent.service.get(id), this._parentComponent.service.getAllRDSs()])
      .then(([model, databases]) => {
        const modal = this._matDialog.open(ActionRdsModalComponent, { disableClose: true, panelClass: 'action-rds-modal-container' });
        modal.componentInstance.service = this._parentComponent.service;
        modal.componentInstance.dataSource = {};
        Object.keys(databases).forEach(x => {
          modal.componentInstance.dataSource[x] = new MatTableDataSource<RDSModel>();
          modal.componentInstance.dataSource[x].data = databases[x];
          modal.componentInstance.dataSource[x]._updateChangeSubscription();
        })
        modal.componentInstance.model = model;

        // Faça algo com os resultados aqui...
      })

  }
}
