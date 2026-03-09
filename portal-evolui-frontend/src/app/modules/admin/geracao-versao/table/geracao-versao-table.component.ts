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
import {GeracaoVersaoComponent} from '../geracao-versao.component';
import {MatDialog} from '@angular/material/dialog';
import {GeracaoVersaoModalComponent} from '../modal/geracao-versao-modal.component';
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {GeracaoVersaoDiffModalComponent} from '../modal/geracao-versao-diff-modal.component';
import {ProjectModel} from 'app/shared/models/project.model';
import {GeracaoVersaoModel} from '../../../../shared/models/geracao-versao.model';

export type SelectionModeType = 'none' | 'diff';
@Component({
  selector       : 'geracao-versao-table',
  templateUrl    : './geracao-versao-table.component.html',
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class GeracaoVersaoTableComponent implements AfterViewInit, OnInit, OnDestroy
{
  @Input()
  dataSource = new MatTableDataSource<GeracaoVersaoModel>();
  private _onDestroy = new Subject<void>();
  public selection = new SelectionModel<number>(true, []);
  displayedColumns = [];
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild(MatTable) table: MatTable<any>;
  canGenerate = false;
  selectionMode: SelectionModeType = 'none';
  private _target: ProjectModel = null;
  set target(value: ProjectModel) {
    this._target = null;
    this.displayedColumns = [ /*'id',*/ 'buttons', 'branch', 'tag', 'image', 'user', 'userEmail', 'status', 'conclusion', 'requestDate',
      'conclusionDate', 'compileType'];
    if (UtilFunctions.isValidStringOrArray(value.modules) === true) {
      for (const module of value.modules) {
        this.displayedColumns.push(module.identifier);
        if (module.framework != true) {
          this.displayedColumns.push('include_' + module.identifier);
        }
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
              private _parentComponent: GeracaoVersaoComponent,
              private _matDialog: MatDialog)
  {
  }


  ngOnInit(): void {
    this._parentComponent.service.target$.pipe((takeUntil(this._onDestroy)))
      .subscribe((value: ProjectModel) => {
        this.target = value;
      });
    this._parentComponent.service.model$.pipe((takeUntil(this._onDestroy)))
      .subscribe((value: Array<GeracaoVersaoModel>) => {
        if (value) {
          this.dataSource.data = value; //Necessário para detectar a alteração no model
          this.update();
        }
      });
    this._parentComponent.service.canGenerateVersion$.pipe((takeUntil(this._onDestroy)))
      .subscribe((value: boolean) => {
        this.canGenerate = value;
        this._changeDetectorRef.detectChanges();
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
      me.setMode('none');
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

  clearImage(model: GeracaoVersaoModel) {
    model.user.image = 'assets/images/noPicture.png';
  }

  add() {

    this._parentComponent.service.getAvailableBranches().then(result => {

      const modal = this._matDialog.open(GeracaoVersaoModalComponent, { disableClose: true, panelClass: 'geracao-versao-modal-container' });
      modal.componentInstance.service = this._parentComponent.service;
      modal.componentInstance.target = this.target;
      modal.componentInstance.projectVersions = result;
    });

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

  gotoMondayLink(id: string) {
    this._parentComponent.service.getMondayLink(id).then(value => {
      window.open(value.resp, '_blank').focus();
    });

  }

  getDiff() {

    this._parentComponent.service.getDiff(this.selection.selected[0], this.selection.selected[1]).then(value => {
      if (!value || !value.modulesDiff || value.modulesDiff.length === 0) {
        this._parentComponent.messageService.open('Nenhuma alteração encontrada', 'INFO', 'info');
      } else {
        const modal = this._matDialog.open(GeracaoVersaoDiffModalComponent, { disableClose: true, panelClass: 'geracao-versao-diff-modal-container' });
        modal.componentInstance.target = this.target;
        modal.componentInstance.model = value;
      }
    });
  }

  cancel(id) {
    this._parentComponent.service.cancel(id).then(value => {
      this._parentComponent.messageService.open('A requisição de cancelamento foi enviada com sucesso. Aguardando a confirmação do repositório...', 'INFO', 'info');
    });
  }

  rerunFailed(id) {
    this._parentComponent.service.rerunFailed(id).then(value => {
      this._parentComponent.messageService.open('A requisição do reprocessamento foi enviada com sucesso. Aguardando a confirmação do repositório...', 'INFO', 'info');
    });
  }

  delete(id) {
    this._parentComponent.messageService.open('Deseja realmente remover a geração?', 'CONFIRMAÇÃO', 'confirm').subscribe((result) => {
      if (result === 'confirmed') {
        const index = this.dataSource.data.findIndex(r => r.id === id);
        if (index >= 0) {
          this._parentComponent.service.delete(id).then(value => {
            this._parentComponent.messageService.open('Geração removida com sucesso', 'SUCESSO', 'success');
          });
        }
      }
    });

  }

  getFormattedVersion(identifier: string, model: GeracaoVersaoModel) {
    const index = model.modules.findIndex(x => x.projectModule.identifier === identifier);
    if (index >= 0) {
      return `Versão: ${model.modules[index].tag}\r\nRepositório:${model.modules[index].repository}\r\nCommit: ${model.modules[index].commit}\r\nCaminho Relativo: ${model.modules[index].relativePath}`;
    }
    return '';
  }

  getInclude(identifier: string, model: GeracaoVersaoModel): boolean {
    const index = model.modules.findIndex(x => x.projectModule.identifier === identifier);
    if (index >= 0) {
      return model.modules[index].enabled;
    }
    return false;
  }

  setMode(mode: SelectionModeType) {
    this.selectionMode = mode;
    if (mode === 'none') {
      this.selection = new SelectionModel<number>(true, [])
    }
  }
}
