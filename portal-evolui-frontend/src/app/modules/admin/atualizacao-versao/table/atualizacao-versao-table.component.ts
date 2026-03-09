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
import {AtualizacaoVersaoComponent} from '../atualizacao-versao.component';
import {MatDialog} from '@angular/material/dialog';
import {AtualizacaoVersaoModalComponent} from '../modal/atualizacao-versao-modal.component';
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {AtualizacaoVersaoModel} from 'app/shared/models/atualizacao-versao.model';
import {ProjectModel} from 'app/shared/models/project.model';
import {EvoluiVersionModel} from '../../../../shared/models/evolui-version.model';

@Component({
  selector       : 'atualizacao-versao-table',
  templateUrl    : './atualizacao-versao-table.component.html',
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class AtualizacaoVersaoTableComponent implements AfterViewInit, OnInit, OnDestroy
{
  @Input()
  dataSource = new MatTableDataSource<AtualizacaoVersaoModel>();
  private _onDestroy = new Subject<void>();
  public selection = new SelectionModel<AtualizacaoVersaoModel>(true, []);
  displayedColumns = [];
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild(MatTable) table: MatTable<any>;
  canGenerate = false;
  private _target: ProjectModel = null;
  set target(value: ProjectModel) {
    this._target = null;
    this.displayedColumns = [ /*'id',*/ 'buttons', 'environment.identifier', 'tag', 'image', 'user.name', 'user.email', 'status', 'conclusion', 'schedulerDate', 'requestDate',
      'conclusionDate', 'tags'];
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
              private _parentComponent: AtualizacaoVersaoComponent,
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
      if (property.includes('.')) {
        return property.split('.').reduce((o,i)=>o[i], item);
      }
      else if (property === 'tag' && UtilFunctions.isValidStringOrArray(item[property]) === true) {
        const v = new EvoluiVersionModel(item[property]);
        return EvoluiVersionModel.comparableString(v);
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
      .subscribe((value: Array<AtualizacaoVersaoModel>) => {
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

  clearImage(model: AtualizacaoVersaoModel) {
    model.user.image = 'assets/images/noPicture.png';
  }

  add() {
    this._parentComponent.service.getInitialData().then(value => {
      value.versions = value.versions.sort((y, x) => {
        return new EvoluiVersionModel(x.tag).customCompare(new EvoluiVersionModel(y.tag));
      });
      const modal = this._matDialog.open(AtualizacaoVersaoModalComponent, { disableClose: true, panelClass: 'atualizacao-versao-modal-container' });
      modal.componentInstance.service = this._parentComponent.service;
      modal.componentInstance.target = this.target;
      modal.componentInstance.model= new AtualizacaoVersaoModel();

      modal.componentInstance.initialData = value;
    });

  }

  edit(id: number) {
    this._parentComponent.service.get(id).then(value => {
      this._parentComponent.service.getInitialData().then(initialData => {
        initialData.versions = initialData.versions.sort((y, x) => {
          return new EvoluiVersionModel(x.tag).customCompare(new EvoluiVersionModel(y.tag));
        });
        const modal = this._matDialog.open(AtualizacaoVersaoModalComponent, { disableClose: true, panelClass: 'atualizacao-versao-modal-container' });
        modal.componentInstance.service = this._parentComponent.service;
        modal.componentInstance.target = this.target;
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

  gotoLink(workflowId: number) {
    this._parentComponent.service.getLink(workflowId).then(value => {
      window.open(value.resp, '_blank').focus();
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

  getFormattedVersion(identifier: string, model: AtualizacaoVersaoModel) {
    const index = model.modules.findIndex(x => x.environmentModule.projectModule.identifier === identifier);
    if (index >= 0) {
      if (model.modules[index].enabled === true) {
        return `Versão: ${model.modules[index].tag}\r\nRepositório:${model.modules[index].repository}\r\nCommit: ${model.modules[index].commit}\r\nCaminho Relativo: ${model.modules[index].relativePath}`;
      }
      else if (model.modules[index].environmentModule.projectModule.framework === true) {
        const mainIsEnabled = model.modules.findIndex(x => x.environmentModule.projectModule.main === true && x.enabled === true);
        if (mainIsEnabled >= 0) {
          return `Versão: ${model.modules[index].tag}\r\nCommit: ${model.modules[index].commit}`;
        }
      }
    }
    return '';
  }

  getInclude(identifier: string, model: AtualizacaoVersaoModel): boolean {
    const index = model.modules.findIndex(x => x.environmentModule.projectModule.identifier === identifier);
    if (index >= 0) {
      return model.modules[index].enabled;
    }
    return false;
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
    this._parentComponent.messageService.open('Deseja realmente remover a atualização?', 'CONFIRMAÇÃO', 'confirm').subscribe((result) => {
      if (result === 'confirmed') {
        const index = this.dataSource.data.findIndex(r => r.id === id);
        if (index >= 0) {
          this._parentComponent.service.delete(id).then(value => {
            this._parentComponent.messageService.open('Atualização removida com sucesso', 'SUCESSO', 'success');
          });
        }
      }
    });

  }

  getConclusionClass(conclusion: string | null | undefined): string {
    switch (conclusion) {
      case 'success':
        return 'text-green-500';
      case 'warning':
        return 'text-orange-500';
      case 'failure':
        return 'text-red-500';
      default:
        return 'text-gray-500';
    }
  }
}
