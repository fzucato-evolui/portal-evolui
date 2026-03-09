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
import {CICDComponent} from '../cicd.component';
import {MatDialog} from '@angular/material/dialog';
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {ProjectModel} from 'app/shared/models/project.model';
import {CICDModel, ModuleCICDModel} from 'app/shared/models/cicd.model';
import {CICDReportModalComponent} from '../modal/cicd-report-modal.component';
import {CICDModalComponent} from '../modal/cicd-modal.component';

export class CICDWithModules {
  cicd: CICDModel = null;
  module: ModuleCICDModel = null;
  parentId: number;
  parent: CICDWithModules;
  totalModules: number = 0;
  expanded = false;
  index: number;
  first: boolean = false;
  last: boolean = false;
  rowHeader: boolean = false;
}
@Component({
  selector       : 'cicd-table',
  templateUrl    : './cicd-table.component.html',
  styleUrls      : ['cicd-table.component.scss'],
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class CICDTableComponent implements AfterViewInit, OnInit, OnDestroy
{
  @Input()
  dataSource = new MatTableDataSource<CICDWithModules>();
  private _onDestroy = new Subject<void>();
  displayedColumns = [];
  modulesColumns = [];
  rowHeaderColumns = [];
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild(MatTable) table: MatTable<any>;
  private _target: ProjectModel = null;

  set target(value: ProjectModel) {
    this._target = null;
    this.displayedColumns = [ /*'id',*/ 'buttons', 'cicd.branch', 'image', 'cicd.user.name', 'cicd.user.email', 'cicd.status', 'cicd.conclusion', 'cicd.requestDate', 'cicd.conclusionDate'];
    this.modulesColumns = [ /*'id',*/ 'module.buttons', 'module.projectModule.title', 'module.status', 'module.commit', 'module.buildSumary', 'module.testSumary', 'module.fatalError'];
    this.rowHeaderColumns = [ /*'id',*/ 'rowHeader.buttons', 'rowHeader.projectModule.title', 'rowHeader.status', 'rowHeader.commit', 'rowHeader.buildSumary', 'rowHeader.testSumary', 'rowHeader.fatalError'];

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

  /**
   * Constructor
   */
  constructor(private _router: Router,
              private _changeDetectorRef: ChangeDetectorRef,
              private _parentComponent: CICDComponent,
              private _matDialog: MatDialog)
  {
    this.dataSource.filterPredicate = (data: any, filter) => {
      if (UtilFunctions.isValidStringOrArray(filter) === false) {
        return true;
      }
      const dataStr = JSON.stringify(data).toLowerCase();
      if (UtilFunctions.isValidStringOrArray(dataStr) === false) {
        return false;
      }
      return UtilFunctions.removeAccents(dataStr).indexOf(UtilFunctions.removeAccents(filter.toLowerCase())) != -1;
    }

    this.dataSource.sortingDataAccessor = (item, property) => {
      if (UtilFunctions.isValidObject(item['cicd']) === true) {
        if (property.includes('.') && property.startsWith('cicd') === true) {
          return property.split('.').reduce((o, i) => o[i], item);
        }
      }

      return item[property];
    };

  }


  ngOnInit(): void {
    this._parentComponent.service.target$.pipe((takeUntil(this._onDestroy)))
      .subscribe((value: ProjectModel) => {
        this.target = value;
      });
    this._parentComponent.service.model$.pipe((takeUntil(this._onDestroy)))
      .subscribe((value: Array<CICDModel>) => {
        if (value) {
          const rows = new Array<CICDWithModules>()
          for(let i = 0; i < value.length; i++) {
            const c = value[i];

            const cicd = new CICDWithModules();
            cicd.cicd = c;
            cicd.index = i;
            rows.push(cicd);
            if (c.modules && UtilFunctions.isValidStringOrArray(c.modules) === true) {
              cicd.totalModules = c.modules.length;
              const rowHeader = new CICDWithModules();
              rowHeader.rowHeader = true;
              rowHeader.parent = cicd;
              rowHeader.index = i;
              rowHeader.first = true;
              rows.push(rowHeader);
              for(let j = 0; j < c.modules.length; j++) {
                const m = c.modules[j];
                const module = new CICDWithModules();
                module.module = m;
                module.parentId = c.id;
                module.parent = cicd;
                module.index = i;
                if (j === c.modules.length - 1) {
                  module.last = true;
                }
                rows.push(module);
              }

            }
          }
          //console.log(rows);
          this.dataSource.data = rows; //Necessário para detectar a alteração no model
          this.update();
        }
      });

  }

  ngOnDestroy(): void {
    this._onDestroy.next();
    this._onDestroy.complete();
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

  refresh() {
    this._parentComponent.filter();
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

  clearImage(model: CICDModel) {
    model.user.image = 'assets/images/noPicture.png';
  }


  getJson(version: string) : any {
    if (UtilFunctions.isValidStringOrArray(version)) {
      return JSON.parse(version);
    }
  }

  gotoLink(workflowId: number) {
    this._parentComponent.service.getLink(workflowId).then(value => {
      window.open(value.resp, '_blank').focus();
    });

  }

  viewReport(workflowId: number) {
    this._parentComponent.service.getDetailedReport(workflowId).then(value => {
      //window.open(value.resp, '_blank').focus();
      const modal = this._matDialog.open(CICDReportModalComponent, { disableClose: true, panelClass: 'cicd-report-modal-container' });
      modal.componentInstance.target = this.target;
      modal.componentInstance.modelMap = value;
    });
  }

  gotoCheckrunLink(checkrun: number, productIdentifier: string) {
    this._parentComponent.service.getCheckrunLink(productIdentifier, checkrun).then(value => {
      window.open(value.resp, '_blank').focus();
    });
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

  isModule(index, item: CICDWithModules): boolean {
    return UtilFunctions.isValidObject(item.module) === true && item.module.id > 0;
  }

  isRowHeader(index, item: CICDWithModules): boolean {
    return item.rowHeader;
  }

  isNotModule(index, item: CICDWithModules): boolean {
    return (UtilFunctions.isValidObject(item.module) === false || UtilFunctions.isValidObject(item.module.id) === false) && item.rowHeader === false;
  }

  expandModel(cicd: CICDWithModules) {
    /*
    this.dataSource.data.filter(x => (x.cicd != null && x.cicd.id === cicd.cicd.id) || (x.cicd === null && x.parentId === cicd.cicd.id)).forEach(y => {
      y.expanded = !cicd.expanded;
    })
     */
    cicd.expanded = !cicd.expanded;
  }

  isVisibleRow(row: CICDWithModules): boolean {
    return row.parent.expanded === true;
  }

  getClass(row: CICDWithModules): string {
    const classes = [];
    if (row.rowHeader === true) {
      classes.push('rowHeader')
    }
    else {
      if (row.index % 2) {
        classes.push('even-row')
      } else {
        classes.push('odd-row')
      }
      if (UtilFunctions.isValidObject(row.cicd) === false) {
        classes.push('expanded-row');
      }
      if (row.last === true) {
        classes.push('last-expanded-row');
      }
      if (row.first === true) {
        classes.push('first-expanded-row');
      }
    }
    return classes.join(' ');
  }


  add() {
    this._parentComponent.service.getBranches().then(value => {
      const modal = this._matDialog.open(CICDModalComponent, {disableClose: true, panelClass: 'cicd-modal-container'});
      modal.componentInstance.target = this.target;
      modal.componentInstance.service = this._parentComponent.service;
      modal.componentInstance.branches = value;
    })
  }

  delete(id) {
    this._parentComponent.messageService.open('Deseja realmente remover a integração (CI/CD)?', 'CONFIRMAÇÃO', 'confirm').subscribe((result) => {
      if (result === 'confirmed') {
        const index = this.dataSource.data.findIndex(r => r.cicd && r.cicd.id === id);
        if (index >= 0) {
          this._parentComponent.service.delete(id).then(value => {
            let message = 'Integração (CI/CD) removida com sucesso';
            if (UtilFunctions.isValidStringOrArray(value.alerts)) {
              const alerts = value.alerts.join("<br />");
              message += ', mas com alertas: <br/>' + alerts;
              this._parentComponent.messageService.open(message, 'SUCESSO', 'warning');
            } else {
              this._parentComponent.messageService.open(message, 'SUCESSO', 'success');
            }
          });
        }
      }
    });
  }

  getVersion(cicd: CICDModel): string {
    if (cicd && cicd.id > 0) {
      if (cicd.branch === cicd.tag) {
        return cicd.branch;
      }
      else {
        return `${cicd.branch} (${cicd.tag})`;
      }

    }
    return null;
  }

  getFormattedVersion(model: ModuleCICDModel) {
    return `Repositório:${model.repository}\r\nCommit: ${model.commit}\r\nCaminho Relativo: ${model.relativePath}`;
  }
}
