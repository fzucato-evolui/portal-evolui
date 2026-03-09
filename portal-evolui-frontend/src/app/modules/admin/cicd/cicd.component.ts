import {ChangeDetectorRef, Component, OnDestroy, OnInit, ViewChild, ViewEncapsulation} from '@angular/core';
import {CicdService} from "./cicd.service";
import {ClassyLayoutComponent} from 'app/layout/layouts/classy/classy.component';
import {MessageDialogService} from "../../../shared/services/message/message-dialog-service";
import {CICDFilterComponent} from './filter/cicd-filter.component';
import {takeUntil} from 'rxjs/operators';
import {ActivatedRoute} from '@angular/router';
import {Subject} from 'rxjs';
import {ProjectModel} from 'app/shared/models/project.model';


@Component({
  selector     : 'cicd',
  templateUrl  : './cicd.component.html',
  styleUrls      : ['./cicd.component.scss'],
  encapsulation: ViewEncapsulation.None,

  standalone: false
})
export class CICDComponent implements OnInit, OnDestroy
{
  @ViewChild('filtro', {static: false}) filtroComponent: CICDFilterComponent;
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
    public service: CicdService,
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
        this.title = 'CI/CD ' + this.target.title;
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
