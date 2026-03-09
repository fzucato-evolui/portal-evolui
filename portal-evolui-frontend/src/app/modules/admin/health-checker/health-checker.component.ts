import {ChangeDetectorRef, Component, OnDestroy, OnInit, ViewEncapsulation} from '@angular/core';
import {HealthCheckerService} from "./health-checker.service";
import {ClassyLayoutComponent} from 'app/layout/layouts/classy/classy.component';
import {MessageDialogService} from "../../../shared/services/message/message-dialog-service";
import {ActivatedRoute} from '@angular/router';
import {TargetType} from '../../../shared/models/enum.model';
import {Subject} from 'rxjs';


@Component({
  selector     : 'health-checker',
  templateUrl  : './health-checker.component.html',
  styleUrls      : ['./health-checker.component.scss'],
  encapsulation: ViewEncapsulation.None,

  standalone: false
})
export class HealthCheckerComponent implements OnInit, OnDestroy
{
  private _onDestroy = new Subject<void>();
  target: TargetType;
  title: string = 'Health Checkers';

  get drawerMode(): 'over' | 'side' {
    return this._parent.mobileQuery.matches ? 'over' : 'side';
  };
  get drawerOpened(): boolean {
    return this._parent.mobileQuery.matches === false;
  };

  constructor(
    private _parent: ClassyLayoutComponent,
    private _changeDetectorRef: ChangeDetectorRef,
    public messageService: MessageDialogService,
    private _route: ActivatedRoute,
    public service: HealthCheckerService
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

  }

  ngOnDestroy(): void {
    this._onDestroy.next();
    this._onDestroy.complete();
  }


}
