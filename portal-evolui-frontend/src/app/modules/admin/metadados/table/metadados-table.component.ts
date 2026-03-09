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
import {MetadadosComponent} from '../metadados.component';
import {MatDialog} from '@angular/material/dialog';
import {MetadadosModalComponent} from '../modal/metadados-modal.component';
import {MetadadosBranchModel} from 'app/shared/models/metadados-branch.model';
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {ProjectModel} from '../../../../shared/models/project.model';

@Component({
  selector       : 'metadados-table',
  templateUrl    : './metadados-table.component.html',
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class MetadadosTableComponent implements AfterViewInit, OnInit, OnDestroy
{
  @Input()
  dataSource = new MatTableDataSource<MetadadosBranchModel>();
  private _onDestroy = new Subject<void>();
  public selection = new SelectionModel<MetadadosBranchModel>(true, []);
  displayedColumns = ['buttons', 'branch', 'dbType', 'host', 'port', 'database', 'dbUser', 'dbPassword', 'debugId', 'lthUser', 'lthPassword', 'licenseServer', 'clients', 'jvmOptions'];
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild(MatTable) table: MatTable<any>;
  private _target: ProjectModel = null;
  set target(value: ProjectModel) {
    this._target = null;
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
              private _parentComponent: MetadadosComponent,
              private _matDialog: MatDialog)
  {
  }


  ngOnInit(): void {
    this._parentComponent.service.target$.pipe((takeUntil(this._onDestroy)))
      .subscribe((value: ProjectModel) => {
        this.target = value;
      });
    this._parentComponent.service.model$.pipe((takeUntil(this._onDestroy)))
      .subscribe((value: Array<MetadadosBranchModel>) => {
        if (value) {
          this.dataSource.data = value; //Necessário para detectar a alteração no model
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
    this._parentComponent.service.getInitialData().then(value => {
      const modal = this._matDialog.open(MetadadosModalComponent, { disableClose: true, panelClass: 'metadados-modal-container' });
      modal.componentInstance.parent = this._parentComponent;
      modal.componentInstance.target = this.target;
      modal.componentInstance.model = new MetadadosBranchModel();
      modal.componentInstance.initialData = value;
    });

  }

  edit(id: number) {
    this._parentComponent.service.get(id).then(value => {
      this._parentComponent.service.getInitialData().then(initialData => {
        const modal = this._matDialog.open(MetadadosModalComponent, { disableClose: true, panelClass: 'metadados-modal-container' });
        modal.componentInstance.parent = this._parentComponent;
        modal.componentInstance.target = this.target;
        modal.componentInstance.model = value;
        modal.componentInstance.initialData = initialData;
      });

    });

  }

  delete(id) {
    this._parentComponent.messageService.open('Deseja realmente remover o metadados?', 'CONFIRMAÇÃO', 'confirm').subscribe((result) => {
      if (result === 'confirmed') {
        const index = this.dataSource.data.findIndex(r => r.id === id);
        if (index >= 0) {
          this._parentComponent.service.delete(id).then(value => {
            this._parentComponent.messageService.open('Metadados removido com sucesso', 'SUCESSO', 'success');
          });
        }
      }
    });

  }

  getClients(model: MetadadosBranchModel): string {
    if (model && UtilFunctions.isValidStringOrArray(model.clients)) {
      return  model.clients.map(x => x.client.identifier).join("; ");
    }
    return '';
  }

  copy(id) {
    this._parentComponent.service.get(id).then(value => {
      this._parentComponent.service.getInitialData().then(initialData => {
        const modal = this._matDialog.open(MetadadosModalComponent, { disableClose: true, panelClass: 'metadados-modal-container' });
        modal.componentInstance.parent = this._parentComponent;
        modal.componentInstance.target = this.target;
        value.id = null;
        value.branch = null;
        if (UtilFunctions.isValidStringOrArray(value.clients) === true) {
          for(const c of value.clients) {
            c.id = null;
          }
        }
        modal.componentInstance.model = value;
        modal.componentInstance.initialData = initialData;
      });

    });
  }
}
