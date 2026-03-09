import {ChangeDetectorRef, Component, OnInit, ViewEncapsulation} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {RepositoryService} from "./repository.service";
import {ClassyLayoutComponent} from 'app/layout/layouts/classy/classy.component';
import {UtilFunctions} from "../../../../shared/util/util-functions";
import {MessageDialogService} from "../../../../shared/services/message/message-dialog-service";
import {DetailedRepositoryGithubModel} from '../../../../shared/models/github.model';


@Component({
  selector     : ' repository-list',
  templateUrl  : './repository.component.html',
  styleUrls      : ['./repository.component.scss'],
  encapsulation: ViewEncapsulation.None,

  standalone: false
})
export class RepositoryComponent implements OnInit
{
  dataSource = new MatTableDataSource<DetailedRepositoryGithubModel>();

  get drawerMode(): 'over' | 'side' {
    return this._parent.mobileQuery.matches ? 'over' : 'side';
  };
  get drawerOpened(): boolean {
    return this._parent.mobileQuery.matches === false;
  };

  constructor(
    private _parent: ClassyLayoutComponent,
    private _service: RepositoryService,
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
