import {ChangeDetectorRef, Component, OnInit, ViewEncapsulation} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {RunnerService} from "./runner.service";
import {ClassyLayoutComponent} from 'app/layout/layouts/classy/classy.component';
import {UtilFunctions} from "../../../../shared/util/util-functions";
import {MessageDialogService} from "../../../../shared/services/message/message-dialog-service";
import {RunnerGithubModel} from '../../../../shared/models/github.model';


@Component({
  selector     : ' runner-list',
  templateUrl  : './runner.component.html',
  styleUrls      : ['./runner.component.scss'],
  encapsulation: ViewEncapsulation.None,

  standalone: false
})
export class RunnerComponent implements OnInit
{
  dataSource = new MatTableDataSource<RunnerGithubModel>();

  get drawerMode(): 'over' | 'side' {
    return this._parent.mobileQuery.matches ? 'over' : 'side';
  };
  get drawerOpened(): boolean {
    return this._parent.mobileQuery.matches === false;
  };

  constructor(
    private _parent: ClassyLayoutComponent,
    private _service: RunnerService,
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
    this.refresh();

  }

  refresh() {
    this._service.getAll()
      .then(value => {

        if (UtilFunctions.isValidStringOrArray(value) === true) {
          this.dataSource.data = value;

        }
        this._changeDetectorRef.detectChanges();
      }).catch(error => {
      console.error(error);
    });
  }
}
