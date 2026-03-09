import {ChangeDetectorRef, Component, OnDestroy, OnInit, ViewChild, ViewEncapsulation} from '@angular/core';
import {AtualizacaoVersaoService} from "./atualizacao-versao.service";
import {ClassyLayoutComponent} from 'app/layout/layouts/classy/classy.component';
import {MessageDialogService} from "../../../shared/services/message/message-dialog-service";
import {AtualizacaoVersaoFilterComponent} from './filter/atualizacao-versao-filter.component';
import {takeUntil} from 'rxjs/operators';
import {ActivatedRoute} from '@angular/router';
import {Subject} from 'rxjs';
import {ProjectModel} from 'app/shared/models/project.model';


@Component({
  selector     : 'atualizacao-versao',
  templateUrl  : './atualizacao-versao.component.html',
  styleUrls      : ['./atualizacao-versao.component.scss'],
  encapsulation: ViewEncapsulation.None,

  standalone: false
})
export class AtualizacaoVersaoComponent implements OnInit, OnDestroy
{
  @ViewChild('filtro', {static: false}) filtroComponent: AtualizacaoVersaoFilterComponent;
  private _onDestroy = new Subject<void>();
  target: ProjectModel;
  title: string;

  get drawerMode(): 'over' | 'side' {
    return this._parent.mobileQuery.matches ? 'over' : 'side';
  };
  get drawerOpened(): boolean {
    return this._parent.mobileQuery.matches === false;
  };

  constructor(
    private _parent: ClassyLayoutComponent,
    public service: AtualizacaoVersaoService,
    private _changeDetectorRef: ChangeDetectorRef,
    public messageService: MessageDialogService,
    private _route: ActivatedRoute
  )
  {
  }

  // -----------------------------------------------------------------------------------------------------
  // @ Lifecycle hooks
  // -----------------------------------------------------------------------------------------------------

  /**
   * On init
   */
  ngOnInit(): void  {
    this.service.target$.pipe((takeUntil(this._onDestroy)))
      .subscribe((value: ProjectModel) => {
        this.target = value;
        this.title = 'Atualização Versão ' + this.target.title;
      });

  }

  ngOnDestroy(): void {
    this._onDestroy.next();
    this._onDestroy.complete();
  }

  public filter() {
    this.filtroComponent.filtrar();
  }


}
