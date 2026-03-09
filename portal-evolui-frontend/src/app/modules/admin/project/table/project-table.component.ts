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
import {ProjectComponent} from '../project.component';
import {MatDialog} from '@angular/material/dialog';
import {ProjectModalComponent} from '../modal/project-modal.component';
import {ProjectModel} from '../../../../shared/models/project.model';
import {UtilFunctions} from '../../../../shared/util/util-functions';

@Component({
  selector       : 'project-table',
  templateUrl    : './project-table.component.html',
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class ProjectTableComponent implements AfterViewInit, OnInit, OnDestroy
{
  @Input()
  dataSource = new MatTableDataSource<ProjectModel>();
  private _onDestroy = new Subject<void>();
  public selection = new SelectionModel<ProjectModel>(true, []);
  displayedColumns = ['buttons', 'icon', 'id', 'identifier', 'title', 'description', 'modules', 'framework', 'luthierProject', 'repository'];
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild(MatTable) table: MatTable<any>;

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
              private _parentComponent: ProjectComponent,
              private _matDialog: MatDialog)
  {
  }


  ngOnInit(): void {

    this._parentComponent.service.model$.pipe((takeUntil(this._onDestroy)))
      .subscribe((value: Array<ProjectModel>) => {
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
    const modal = this._matDialog.open(ProjectModalComponent, { disableClose: true, panelClass: 'project-modal-container' });
    modal.componentInstance.service = this._parentComponent.service;
    modal.componentInstance.model = new ProjectModel();
  }

  edit(id: number) {
    this._parentComponent.service.get(id).then(value => {
      const modal = this._matDialog.open(ProjectModalComponent, { disableClose: true, panelClass: 'project-modal-container' });
      modal.componentInstance.service = this._parentComponent.service;
      modal.componentInstance.model = value;
    });

  }

  delete(id) {
    this._parentComponent.messageService.open('Deseja realmente remover o project?', 'CONFIRMAÇÃO', 'confirm').subscribe((result) => {
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

  getModules(model: ProjectModel) {
    if (model && UtilFunctions.isValidStringOrArray(model.modules)) {
      return model.modules.map(x => x.identifier).join("; ");
    }
  }
}
