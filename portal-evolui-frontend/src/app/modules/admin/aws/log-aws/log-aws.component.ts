import {ChangeDetectorRef, Component, OnInit, ViewChild, ViewEncapsulation} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {LogAwsService} from "./log-aws.service";
import {ClassyLayoutComponent} from 'app/layout/layouts/classy/classy.component';
import {LogAWSActionFilterModel, LogAWSActionModel} from "../../../../shared/models/log.aws.action.model";
import {UtilFunctions} from "../../../../shared/util/util-functions";
import {MessageDialogService} from "../../../../shared/services/message/message-dialog-service";
import {LogAwsFilterComponent} from "./filter/log-aws-filter.component";


@Component({
  selector     : 'log-aws-list',
  templateUrl  : './log-aws.component.html',
  styleUrls      : ['./log-aws.component.scss'],
  encapsulation: ViewEncapsulation.None,

  standalone: false
})
export class LogAwsComponent implements OnInit
{
  @ViewChild('filtro', {static: false}) filtroComponent: LogAwsFilterComponent;
  dataSource = new MatTableDataSource<LogAWSActionModel>();

  get drawerMode(): 'over' | 'side' {
    return this._parent.mobileQuery.matches ? 'over' : 'side';
  };
  get drawerOpened(): boolean {
    return this._parent.mobileQuery.matches === false;
  };

  constructor(
    private _parent: ClassyLayoutComponent,
    private _service: LogAwsService,
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
  ngOnInit(): void  {
    //this.refresh();

  }

  refresh() {
    this._service.getAll()
      .then(value => {
        if (UtilFunctions.isValidStringOrArray(value) === true) {
          this.dataSource.data = value;
        } else {
          this.dataSource.data = [];
        }
        this.dataSource._updateChangeSubscription();
      }).catch(error => {
      console.error(error);
    });
  }

  filtrar(filtro: LogAWSActionFilterModel) {
    this._service.filtrar(filtro)
      .then(value => {
        if (UtilFunctions.isValidStringOrArray(value) === true) {
          this.dataSource.data = value;
        } else {
          this.dataSource.data = [];
        }
        this.dataSource._updateChangeSubscription();
      }).catch(error => {
      console.error(error);
    });
  }


}
