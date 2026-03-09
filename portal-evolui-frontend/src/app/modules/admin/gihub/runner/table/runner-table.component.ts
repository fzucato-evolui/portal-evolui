import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  Output,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {MatTable, MatTableDataSource} from "@angular/material/table";
import {Router} from "@angular/router";
import {MatSort} from "@angular/material/sort";
import {RunnerGithubModel} from '../../../../../shared/models/github.model';
import {UtilFunctions} from '../../../../../shared/util/util-functions';

@Component({
  selector       : 'runner-table',
  templateUrl    : './runner-table.component.html',
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class RunnerTableComponent implements AfterViewInit
{
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild(MatTable) table: MatTable<any>;
  @Input()
  multiple = false
  @Input()
  showActionsButtons = true
  @Input()
  showFastFilter = true
  @Input()
  dataSource = new MatTableDataSource<RunnerGithubModel>();
  @Output()
  onRefreshClicked: EventEmitter<any> = new EventEmitter();


  displayedColumns = [ 'buttons', 'id', 'name', 'os', 'status', 'busy', 'labels.name'];
  /**
   * Constructor
   */
  constructor(private _router: Router, private _changeDetectorRef: ChangeDetectorRef,)
  {
  }

  refresh() {
    //this._router.navigate(['/admin/users', -1]);
    this.onRefreshClicked.emit(null);
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

  update() {
    const me = this;
    setTimeout(() =>{
      me.table.renderRows();
      me._changeDetectorRef.detectChanges();
    });
  }

  getRunnerLabel(model: RunnerGithubModel) {
    if (UtilFunctions.isValidStringOrArray(model.labels) === true) {
      const customLabel = model.labels.filter(x => x.type === 'custom');
      if (UtilFunctions.isValidStringOrArray(customLabel) === true) {
        return customLabel[0].name;
      }
    }
    return '';

  }
}
