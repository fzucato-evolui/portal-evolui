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
import {SelectionModel} from "@angular/cdk/collections";
import {LogAWSActionModel} from "../../../../../shared/models/log.aws.action.model";

@Component({
  selector       : 'log-aws-table',
  templateUrl    : './log-aws-table.component.html',
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class LogAwsTableComponent implements AfterViewInit
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
  dataSource = new MatTableDataSource<LogAWSActionModel>();
  @Output()
  onStartClicked: EventEmitter<LogAWSActionModel> = new EventEmitter<LogAWSActionModel>();
  @Output()
  onRefreshClicked: EventEmitter<any> = new EventEmitter();
  @Output()
  onStopClicked: EventEmitter<LogAWSActionModel> = new EventEmitter<LogAWSActionModel>();

  public selection = new SelectionModel<LogAWSActionModel>(true, []);

  displayedColumns = [ /*'id',*/ 'image', 'user', 'userEmail', 'logDate', 'action', 'instance'];
  /**
   * Constructor
   */
  constructor(private _router: Router, private _changeDetectorRef: ChangeDetectorRef,)
  {
  }

  start(row: LogAWSActionModel) {
    this.onStartClicked.emit(row);
  }

  stop(row: LogAWSActionModel) {
    this.onStopClicked.emit(row);
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

  getJson(model: LogAWSActionModel) : any {
    return JSON.parse(model.instance);
  }
  clearImage(model: LogAWSActionModel) {
    model.user.image = 'assets/images/noPicture.png';
  }

}
