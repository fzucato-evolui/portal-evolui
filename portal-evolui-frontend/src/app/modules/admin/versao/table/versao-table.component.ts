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
import {Subject} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import {VersaoComponent} from '../versao.component';
import {MatDialog} from '@angular/material/dialog';
import {VersaoModalComponent} from '../modal/versao-modal.component';
import {VersaoModel} from '../../../../shared/models/version.model';
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {groupBy} from 'lodash';
import {ProjectModel} from '../../../../shared/models/project.model';
import {UsuarioModel} from 'app/shared/models/usuario.model';

export class Group {
  level = 0;
  branch: string;
  parent: Group;
  expanded = true;
  totalCounts = 0;
  get visible(): boolean {
    return !this.parent || (this.parent.visible && this.parent.expanded);
  }
}

@Component({
  selector       : 'versao-table',
  templateUrl    : './versao-table.component.html',
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class VersaoTableComponent implements AfterViewInit, OnInit, OnDestroy
{
  @Input()
  dataSource = new MatTableDataSource<any | Group>([]);
  private _onDestroy = new Subject<void>();
  displayedColumns = [];
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild(MatTable) table: MatTable<any>;
  private _target: ProjectModel = null;
  set target(value: ProjectModel) {
    this._target = null;
    this.displayedColumns = ['buttons', 'branch', 'tag', 'versionType'];
    if (UtilFunctions.isValidStringOrArray(value.modules) === true) {
      for (const module of value.modules) {
        this.displayedColumns.push(module.identifier);
      }
    }
    this._target = value;
    this.clear();
    this._changeDetectorRef.detectChanges();
  }

  get target() {
    return this._target;
  }
  @Input()
  multiple = false;
  @Input()
  showActionsButtons = true;
  @Input()
  showFastFilter = true;

  allData: any[];

  groupedBranchData: {[key: string]: Array<VersaoModel>}

  get user(): UsuarioModel {
    return this._parentComponent.user;
  }

  /**
   * Constructor
   */
  constructor(private _router: Router,
              private _changeDetectorRef: ChangeDetectorRef,
              private _parentComponent: VersaoComponent,
              private _matDialog: MatDialog)
  {
  }


  ngOnInit(): void {
    this._parentComponent.service.target$.pipe((takeUntil(this._onDestroy)))
      .subscribe((value: ProjectModel) => {
        this.target = value;
      });
    this._parentComponent.service.model$.pipe((takeUntil(this._onDestroy)))
      .subscribe((value: Array<VersaoModel>) => {
        if (value) {
          /*
          value.forEach((item, index) => {
            item.id = index + 1;
          });*/
          this.groupedBranchData = groupBy(value, 'branch');
          this.allData = this.buildGroups();
          this.dataSource.data = this.allData;
          //this.dataSource.filterPredicate = this.customFilterPredicate.bind(this);
          //this.dataSource.filter = performance.now().toString();

          //this.dataSource.data = value; //Necessário para detectar a alteração no model
          this.update();
        }
      });

  }

  ngOnDestroy(): void {
    this._onDestroy.next();
    this._onDestroy.complete();
  }
  ngAfterViewInit(): void {
    //this.dataSource.sort = this.sort;
  }


  announceSortChange($event) {
    this.allData = this.buildGroups();
    this.dataSource.data = this.allData;
  }

  fastFilter(event: Event) {

    const filterValue = (event.target as HTMLInputElement).value;
    if (UtilFunctions.isValidStringOrArray(filterValue) === false) {
      this.dataSource.data = this.allData;
    } else {
      this.dataSource.data = this.allData.filter(x => {
        if (x instanceof Group) {
          return true;
        } else {
          let match = false;
          for (const c of this.displayedColumns) {
            if (!x[c]) {
              continue;
            }
            const val = JSON.stringify(x[c]);
            if(UtilFunctions.removeAccents(val).toLowerCase().includes(UtilFunctions.removeAccents(filterValue).toLowerCase())) {
              return true;
            }
          }
          return false;
        }
      })
    }
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
    const modal = this._matDialog.open(VersaoModalComponent, { disableClose: true, panelClass: 'versao-modal-container' });
    modal.componentInstance.service = this._parentComponent.service;
    modal.componentInstance.target = this.target;
    modal.componentInstance.model = new VersaoModel();
  }

  edit(id: number) {
    this._parentComponent.service.get(id).then(value => {
      const modal = this._matDialog.open(VersaoModalComponent, { disableClose: true, panelClass: 'versao-modal-container' });
      modal.componentInstance.service = this._parentComponent.service;
      modal.componentInstance.target = this.target;
      modal.componentInstance.model = value;
    });

  }

  delete(branch) {
    this._parentComponent.messageService.open('Deseja realmente a branch? Isso apagará a pasta do bucket, branch e tags referentes a branch de todos os módulos no github', 'CONFIRMAÇÃO', 'confirm').subscribe((result) => {
      if (result === 'confirmed') {
        this._parentComponent.service.deleteBranch(branch).then(value => {
          let message = 'Branch removida com sucesso';
          if (UtilFunctions.isValidStringOrArray(value.alerts)) {
            const alerts = value.alerts.join("<br />");
            message += ', mas com alertas: <br/>' + alerts;
            this._parentComponent.messageService.open(message, 'SUCESSO', 'warning');
          } else {
            this._parentComponent.messageService.open(message, 'SUCESSO', 'success');
          }
        });
      }
    });

  }

  groupHeaderClick(row) {
    row.expanded = !row.expanded;
    //this.dataSource.filter = performance.now().toString();  // bug here need to fix
  }

  buildGroups(): any[] {
    let data = [];
    let keys = Object.keys(this.groupedBranchData);
    if (this.sort.active && this.sort.direction !== '' && this.sort.active === 'branch') {
      const isAsc = this.sort.direction === 'asc';
      if (isAsc) {
        keys = keys.sort();
      } else {
        keys = keys.reverse();
      }

    }
    keys.forEach(k => {
      const group = new Group();
      group.expanded = true;
      group.totalCounts = this.groupedBranchData[k].length;
      group.branch = k;
      group.level = 1;
      data.push(group);
      let rows = this.groupedBranchData[k];
      if (this.sort.active && this.sort.direction !== '') {
        const isAsc = this.sort.direction === 'asc';
        rows = this.groupedBranchData[k].sort((x, y) => {
          console.log(this.sort.active, x[this.sort.active]);
          return (x[this.sort.active] as string).localeCompare(y[this.sort.active] as string)* (isAsc ? 1 : -1);
        });
      }
      for (const v of rows) {
        data.push(v)
      }
    });

    return data;
  }

  isGroup(index, item): boolean {
    return item.level;
  }

  getFormattedVersion(identifier: string, model: VersaoModel) {
    const index = model.modules.findIndex(x => x.projectModule.identifier === identifier);
    if (index >= 0) {
      return `Versão: ${model.modules[index].tag}\r\nRepositório:${model.modules[index].repository}\r\nCommit: ${model.modules[index].commit}\r\nCaminho Relativo: ${model.modules[index].relativePath}`;
    }
    return '';
  }

}
