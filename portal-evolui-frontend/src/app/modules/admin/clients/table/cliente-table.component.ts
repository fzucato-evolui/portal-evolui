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
import {ClienteComponent} from '../cliente.component';
import {MatDialog} from '@angular/material/dialog';
import {ClienteModalComponent} from '../modal/cliente-modal.component';
import {ClientModel} from '../../../../shared/models/client.model';
import {ProjectModel} from '../../../../shared/models/project.model';

@Component({
  selector       : 'cliente-table',
  templateUrl    : './cliente-table.component.html',
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class ClienteTableComponent implements AfterViewInit, OnInit, OnDestroy
{
  @Input()
  dataSource = new MatTableDataSource<ClientModel>();
  private _onDestroy = new Subject<void>();
  public selection = new SelectionModel<ClientModel>(true, []);
  displayedColumns = ['buttons', 'identifier', 'description', 'keywords'];
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
              private _parentComponent: ClienteComponent,
              private _matDialog: MatDialog)
  {
  }


  ngOnInit(): void {
    this._parentComponent.service.target$.pipe((takeUntil(this._onDestroy)))
      .subscribe((value: ProjectModel) => {
        this.target = value;
      });
    this._parentComponent.service.model$.pipe((takeUntil(this._onDestroy)))
      .subscribe((value: Array<ClientModel>) => {
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
    const modal = this._matDialog.open(ClienteModalComponent, { disableClose: true, panelClass: 'client-modal-container' });
    modal.componentInstance.parent = this._parentComponent;
    modal.componentInstance.target = this.target;
    modal.componentInstance.model = new ClientModel();
  }

  edit(id: number) {
    this._parentComponent.service.get(id).then(value => {
      const modal = this._matDialog.open(ClienteModalComponent, { disableClose: true, panelClass: 'client-modal-container' });
      modal.componentInstance.parent = this._parentComponent;
      modal.componentInstance.target = this.target;
      modal.componentInstance.model = value;
    });

  }

  delete(id) {
    this._parentComponent.messageService.open('Deseja realmente remover o cliente?', 'CONFIRMAÇÃO', 'confirm').subscribe((result) => {
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
}
