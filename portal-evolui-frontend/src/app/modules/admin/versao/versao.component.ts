import {ChangeDetectorRef, Component, OnDestroy, OnInit, ViewEncapsulation} from '@angular/core';
import {VersaoService} from "./versao.service";
import {ClassyLayoutComponent} from 'app/layout/layouts/classy/classy.component';
import {MessageDialogService} from "../../../shared/services/message/message-dialog-service";
import {takeUntil} from 'rxjs/operators';
import {ActivatedRoute} from '@angular/router';
import {Subject} from 'rxjs';
import {ProjectModel} from '../../../shared/models/project.model';
import {UsuarioModel} from 'app/shared/models/usuario.model';


@Component({
  selector     : 'versao',
  templateUrl  : './versao.component.html',
  styleUrls      : ['./versao.component.scss'],
  encapsulation: ViewEncapsulation.None,

  standalone: false
})
export class VersaoComponent implements OnInit, OnDestroy
{
  private _onDestroy = new Subject<void>();
  target: ProjectModel;
  title: string;

  get drawerMode(): 'over' | 'side' {
    return this._parent.mobileQuery.matches ? 'over' : 'side';
  };
  get drawerOpened(): boolean {
    return this._parent.mobileQuery.matches === false;
  };

  get user(): UsuarioModel {
    return this._parent.user;
  }

  constructor(
    private _parent: ClassyLayoutComponent,
    public service: VersaoService,
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
        this.title = 'Versões do ' + this.target.title;
      });

  }

  ngOnDestroy(): void {
    this._onDestroy.next();
    this._onDestroy.complete();
  }


}
