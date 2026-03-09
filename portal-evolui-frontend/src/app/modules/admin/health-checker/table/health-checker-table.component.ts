import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {MatTable, MatTableDataSource} from "@angular/material/table";
import {Router} from "@angular/router";
import {MatSort} from "@angular/material/sort";
import {SelectionModel} from "@angular/cdk/collections";
import {Subject} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import {HealthCheckerComponent} from '../health-checker.component';
import {MatDialog} from '@angular/material/dialog';
import {HealthCheckerModalComponent} from '../modal/health-checker-modal.component';
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {
  HealthCheckerConfigModel,
  HealthCheckerModel,
  HealthCheckerModuleModel,
  HealthCheckerSimpleSystemInfoModel
} from 'app/shared/models/health-checker.model';
import {UsuarioModel} from '../../../../shared/models/usuario.model';
import {MatInput} from '@angular/material/input';
import {HealthCheckerMonitorModalComponent} from '../modal/health-checker-monitor-modal.component';

export class DataSourceHealthChecker {
  public groupHeader: boolean;
  public id: number;
  public identifier: string;
  public description: string;
  public user: UsuarioModel;
  public lastHealthDate: Date;
  public  lastUpdate: Date;
  public health: boolean;
  public systemInfo: HealthCheckerSimpleSystemInfoModel;
  public online: boolean;
  public modules: Array<HealthCheckerModuleModel>;
  public parentId: number;
  public index: number;
  public first: boolean = false;
  public last: boolean = false;
}
@Component({
  selector       : 'health-checker-table',
  templateUrl    : './health-checker-table.component.html',
  styleUrls      : ['health-checker-table.component.scss'],
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,


  standalone: false
})

export class HealthCheckerTableComponent implements AfterViewInit, OnInit, OnDestroy
{
  @Input()
  dataSource = new MatTableDataSource<DataSourceHealthChecker>();
  private _onDestroy = new Subject<void>();
  public selection = new SelectionModel<DataSourceHealthChecker>(false, []);
  displayedColumns = ['buttons', 'online', 'health', 'identifier', 'description', 'user', 'lastHealthDate', 'lastUpdate', 'systemInfo'];
  expandedElements: Array<number> = [];
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild(MatTable) table: MatTable<any>;
  @ViewChild('filter') filter: MatInput;

  @Input()
  multiple = false;
  @Input()
  showActionsButtons = true;
  @Input()
  showFastFilter = true;
  allData: Array<DataSourceHealthChecker> = [];
  originalData: Array<HealthCheckerModel> = [];
  /**
   * Constructor
   */
  constructor(private _router: Router,
              private _changeDetectorRef: ChangeDetectorRef,
              private _parentComponent: HealthCheckerComponent,
              private _matDialog: MatDialog)
  {
  }


  ngOnInit(): void {


  }

  ngAfterViewInit(): void {

    this._parentComponent.service.model$.pipe((takeUntil(this._onDestroy)))
      .subscribe((value: Array<HealthCheckerModel>) => {
        if (value) {
          this.originalData = value;
          this.sortData();
          this.fillDataSource();
          if (this.filter) {
            this.filterData(this.filter.value);
          } else {
            this.filterData('');
          }
          this.update();
        }
      });
  }

  sortData() {
    if (!this.sort) {
      return;
    }
    let sortedColumn = this.sort.active;
    let isAsc = this.sort.direction === 'asc';
    if (this.sort.direction === '') {
      sortedColumn = 'id';
      isAsc = true;
    }
    this.originalData.sort((a, b) => {
      if (sortedColumn === 'health') {
        let healthA = a.health;
        a.modules.forEach((x) => {
          healthA = healthA || x.health;
        });
        let healthB = b.health;
        b.modules.forEach((x) => {
          healthB = healthB || x.health;
        });
        return (healthA.toString()).localeCompare(healthB.toString())* (isAsc ? 1 : -1);
      } else {
        const valueA = JSON.stringify(a[sortedColumn]);
        const valueB = JSON.stringify(b[sortedColumn]);
        if (UtilFunctions.isValidStringOrArray(valueA) === true) {
          return valueA.localeCompare(valueB) * (isAsc ? 1 : -1);
        } else {
          if (UtilFunctions.isValidStringOrArray(valueB) === false) {
            return  0;
          } else  {
            return -1 * (isAsc ? 1 : -1)
          }
        }
      }
    });
    for (const d of this.originalData) {
      d.modules.sort((a,b) => {
        const valueA = JSON.stringify(a[sortedColumn]);
        const valueB = JSON.stringify(b[sortedColumn]);
        if (UtilFunctions.isValidStringOrArray(valueA) === true) {
          return valueA.localeCompare(valueB) * (isAsc ? 1 : -1);
        } else {
          if (UtilFunctions.isValidStringOrArray(valueB) === false) {
            return  0;
          } else  {
            return -1 * (isAsc ? 1 : -1)
          }
        }
      })
    }
  }
  fillDataSource() {

    this.allData = [];
    for (let i = 0; i < this.originalData.length; i++) {
      const h = this.originalData[i];
      // @ts-ignore
      const r: DataSourceHealthChecker = {... h} as DataSourceHealthChecker;
      r.groupHeader = true;
      r.index = i;
      this.allData.push(r);
      if (UtilFunctions.isValidStringOrArray(h.modules)) {
        for(let j = 0; j < h.modules.length; j++) {
          const m = h.modules[j];
          // @ts-ignore
          const c: DataSourceHealthChecker = {... m} as DataSourceHealthChecker;
          c.groupHeader = false;
          c.parentId = h.id;
          c.index = i;
          if (j === 0) {
            c.first = true;
          }
          if (j === h.modules.length - 1) {
            c.last = true;
          }
          this.allData.push(c);
        }
      }
    }
  }
  filterData(filterValue: string) {
    if (UtilFunctions.isValidStringOrArray(filterValue) === false) {
      this.dataSource.data = this.allData;
    } else {
      this.dataSource.data = this.allData.filter(x => {
        let match = false;
        for (const c of this.displayedColumns) {
          if (!x[c]) {
            continue;
          }
          const val = JSON.stringify(x[c]);
          if(UtilFunctions.removeAccents(val).toLowerCase().includes(UtilFunctions.removeAccents(filterValue).toLowerCase())) {
            match = true;
            break;
          }
        }

        if (!match && x.groupHeader === true) {
          match = false;
          for (const m of x.modules) {
            for (const c of this.displayedColumns) {
              if (!m[c]) {
                continue;
              }
              const val = JSON.stringify(m[c]);
              if(UtilFunctions.removeAccents(val).toLowerCase().includes(UtilFunctions.removeAccents(filterValue).toLowerCase())) {
                match = true;
                break;
              }
            }
          }

        }
        return match;
      })
    }
  }


  ngOnDestroy(): void {
    this._onDestroy.next();
    this._onDestroy.complete();
  }


  announceSortChange($event) {
    this.sortData();
    this.fillDataSource();
    this.filterData(this.filter.value);
  }

  fastFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value;
    this.filterData(filterValue);
  }

  refresh() {
    this._parentComponent.service.getAll();
  }

  update() {
    const me = this;
    setTimeout(() =>{
      me.table.renderRows();
      me._changeDetectorRef.detectChanges();
    });
  }

  clear() {
    const me = this;
    setTimeout(() =>{
      me.dataSource.data = [];
      me._changeDetectorRef.detectChanges();
    });
  }

  add() {
    this._parentComponent.service.getPossibleUsers().then(value => {
      const modal = this._matDialog.open(HealthCheckerModalComponent, { disableClose: true, panelClass: 'health-checker-modal-container' });
      modal.componentInstance.service = this._parentComponent.service;
      modal.componentInstance.model = new HealthCheckerConfigModel();
      modal.componentInstance.possibleUsers = value;
    })

  }

  edit(id: number) {
    this._parentComponent.service.get(id).then(value => {
      this._parentComponent.service.getPossibleUsers().then(users => {
        const modal = this._matDialog.open(HealthCheckerModalComponent, {
          disableClose: true,
          panelClass: 'health-checker-modal-container'
        });
        if (!users) {
          users = [];
        }
        users.push(value.user);
        modal.componentInstance.service = this._parentComponent.service;
        modal.componentInstance.model = value.config;
        modal.componentInstance.possibleUsers = users;
        modal.componentInstance.online = value.online;
      })
    });

  }

  delete(id) {
    this._parentComponent.messageService.open('Deseja realmente remover o produto?', 'CONFIRMAÇÃO', 'confirm').subscribe((result) => {
      if (result === 'confirmed') {
        const index = this.dataSource.data.findIndex(r => r.id === id);
        if (index >= 0) {
          this._parentComponent.service.delete(id).then(value => {
            this._parentComponent.messageService.open('Cliente removido com sucesso', 'SUCESSO', 'success');
          });
        }
      }
    });

  }

  monitor(id) {
    this._parentComponent.service.get(id).then(value => {
      const modal = this._matDialog.open(HealthCheckerMonitorModalComponent, {
        disableClose: true,
        panelClass: 'health-checker-monitor-modal-container'
      });

      modal.componentInstance.service = this._parentComponent.service;
      modal.componentInstance.model = value.config;
      modal.componentInstance.online = value.online;
    });
  }

  getModules(model: HealthCheckerModel) {
    if (model && UtilFunctions.isValidStringOrArray(model.modules)) {
      return model.modules.map(x => x.identifier).join("; ");
    }
  }

  isHealth(model: DataSourceHealthChecker): boolean | null {
    if (model.health === null) {
      return null;
    }
    if (model.health === false) {
      return false;
    }
    if (UtilFunctions.isValidStringOrArray(model.modules)) {
      for(const c of model.modules) {
        if (c.health === false) {
          return false;
        }
      }
    }
    return true;

  }

  expandModel(model: HealthCheckerModel): boolean {
    const index = this.expandedElements.indexOf(model.id);
    if (index >= 0) {
      this.expandedElements.splice(index, 1);
      return false;
    } else {
      this.expandedElements.push(model.id);
      return true;
    }
  }

  isVisibleRow(row: DataSourceHealthChecker): boolean {
    return row.groupHeader || (row.groupHeader === false && this.expandedElements.indexOf(row.parentId) >= 0);
  }

  showErrors(model: DataSourceHealthChecker) {
    this._parentComponent.service.getAlerts(model.id, model.groupHeader === false).then(value => {
      if (UtilFunctions.isValidStringOrArray(value)) {
        let errors = `<pre>${JSON.stringify(value, null, 2)}</pre>`;
        this._parentComponent.messageService.open(errors, 'Erros', 'info');
      } else {
        this._parentComponent.messageService.open('Nenhum alerta encontrado', 'Alerta', 'warning');
      }
    })
  }

  getClass(row: DataSourceHealthChecker): string {
    const classes = [];
    if (row.index % 2) {
      classes.push('even-row')
    } else {
      classes.push('odd-row')
    }
    if (row.groupHeader === false) {
      classes.push('expanded-row');
    }
    if (row.last === true) {
      classes.push('last-expanded-row');
    }
    if (row.first === true) {
      classes.push('first-expanded-row');
    }
    return classes.join(' ');
  }

}
