import {ChangeDetectorRef, Component, OnInit, ViewChild, ViewEncapsulation} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {ActionRdsService} from "./action-rds.service";
import {ClassyLayoutComponent} from 'app/layout/layouts/classy/classy.component';
import {MessageDialogService} from "../../../../shared/services/message/message-dialog-service";
import {ActionRdsFilterComponent} from "./filter/action-rds-filter.component";
import {ActionRdsFilterModel, ActionRdsModel} from '../../../../shared/models/action-rds.model';
import {takeUntil} from 'rxjs/operators';
import {Subject} from 'rxjs';


@Component({
  selector     : 'action-rds-list',
  templateUrl  : './action-rds.component.html',
  styleUrls      : ['./action-rds.component.scss'],
  encapsulation: ViewEncapsulation.None,

  standalone: false
})
export class ActionRdsComponent implements OnInit
{
  @ViewChild('filtro', {static: false}) filtroComponent: ActionRdsFilterComponent;
  dataSource = new MatTableDataSource<ActionRdsModel>();
  private _onDestroy = new Subject<void>();
  get drawerMode(): 'over' | 'side' {
    return this._parent.mobileQuery.matches ? 'over' : 'side';
  };
  get drawerOpened(): boolean {
    return this._parent.mobileQuery.matches === false;
  };
  get service(): ActionRdsService {
    return this._service;
  }
  get messageService(): MessageDialogService {
    return this._messageService;
  }

  get parent(): ClassyLayoutComponent {
    return this._parent;
  }

  constructor(
    private _parent: ClassyLayoutComponent,
    private _service: ActionRdsService,
    private _changeDetectorRef: ChangeDetectorRef,
    private _messageService: MessageDialogService
  )
  {
  }

  // -----------------------------------------------------------------------------------------------------
  // @ Lifecycle hooks
  // -----------------------------------------------------------------------------------------------------

  /**
   * On init
   */
  ngOnInit(): void {

    this._service.model$.pipe((takeUntil(this._onDestroy)))
      .subscribe((value: Array<ActionRdsModel>) => {
        if (value) {
          this.dataSource.data = value; //Necessário para detectar a alteração no model
        }
      });

  }

  ngOnDestroy(): void {
    this._onDestroy.next();
    this._onDestroy.complete();
  }

  filtrar(filtro: ActionRdsFilterModel) {
    this._service.filtrar(filtro);
  }

  refresh() {
    this._service.filtrar(this.filtroComponent.model);
  }
}
