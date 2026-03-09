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
import {AmbienteComponent} from '../ambiente.component';
import {MatDialog} from '@angular/material/dialog';
import {AmbienteModel} from 'app/shared/models/ambiente.model';
import {AmbienteModalComponent} from '../modal/ambiente-modal.component';
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {ProjectModel} from 'app/shared/models/project.model';
import {VersaoModel} from '../../../../shared/models/version.model';

@Component({
  selector       : 'ambiente-table',
  templateUrl    : './ambiente-table.component.html',
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class AmbienteTableComponent implements AfterViewInit, OnInit, OnDestroy
{
  @Input()
  dataSource = new MatTableDataSource<AmbienteModel>();
  private _onDestroy = new Subject<void>();
  public selection = new SelectionModel<AmbienteModel>(true, []);
  displayedColumns = [];
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild(MatTable) table: MatTable<any>;
  private _target: ProjectModel = null;
  set target(value: ProjectModel) {
    this._target = null;
    this.displayedColumns = [ /*'id',*/ 'buttons', 'tag', 'identifier', 'description', 'client'];
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


  /**
   * Constructor
   */
  constructor(private _router: Router,
              private _changeDetectorRef: ChangeDetectorRef,
              private _parentComponent: AmbienteComponent,
              private _matDialog: MatDialog)
  {
  }


  ngOnInit(): void {
    this._parentComponent.service.target$.pipe((takeUntil(this._onDestroy)))
      .subscribe((value: ProjectModel) => {
        this.target = value;
      });
    this._parentComponent.service.model$.pipe((takeUntil(this._onDestroy)))
      .subscribe((value: Array<AmbienteModel>) => {
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
    this._parentComponent.service.getInitialData(-1).then(value => {
      const modal = this._matDialog.open(AmbienteModalComponent, { disableClose: true, panelClass: 'ambiente-modal-container' });
      modal.componentInstance.service = this._parentComponent.service;
      modal.componentInstance.target = this.target;
      modal.componentInstance.model = new AmbienteModel();
      modal.componentInstance.initialData = value;
    });

  }

  edit(id: number) {
    this._parentComponent.service.get(id).then(value => {
      this._parentComponent.service.getInitialData(id).then(initialData => {
        const modal = this._matDialog.open(AmbienteModalComponent, { disableClose: true, panelClass: 'ambiente-modal-container' });
        modal.componentInstance.service = this._parentComponent.service;
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

  copy(id) {
    this._parentComponent.service.get(id).then(value => {
      this._parentComponent.service.getInitialData(-1).then(initialData => {
        const modal = this._matDialog.open(AmbienteModalComponent, { disableClose: true, panelClass: 'ambiente-modal-container' });
        modal.componentInstance.service = this._parentComponent.service;
        modal.componentInstance.target = this.target;
        value.id = null;
        if (UtilFunctions.isValidStringOrArray(value.modules) === true) {
          for(const mod of value.modules) {
            mod.id = null;
          }
        }
        modal.componentInstance.model = value;
        modal.componentInstance.initialData = initialData;
      });

    });
  }

  getJson(version: string) : any {
    if (UtilFunctions.isValidStringOrArray(version)) {
      return JSON.parse(version);
    }
  }

  getFormattedVersion(identifier: string, model: VersaoModel) {
    const index = model.modules.findIndex(x => x.projectModule.identifier === identifier);
    if (index >= 0) {
      return `Versão: ${model.modules[index].tag}\r\nRepositório:${model.modules[index].repository}\r\nCommit: ${model.modules[index].commit}\r\nCaminho Relativo: ${model.modules[index].relativePath}`;
    }
    return '';
  }
}
