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
import {WorkspaceModel} from 'app/shared/models/workspace.model';

@Component({
  selector       : 'workspace-table',
  templateUrl    : './workspace-table.component.html',
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class WorkspaceTableComponent implements AfterViewInit
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
  dataSource = new MatTableDataSource<WorkspaceModel>();
  @Output()
  onStartClicked: EventEmitter<WorkspaceModel> = new EventEmitter<WorkspaceModel>();
  @Output()
  onRebootClicked: EventEmitter<WorkspaceModel> = new EventEmitter<WorkspaceModel>();
  @Output()
  onRefreshClicked: EventEmitter<any> = new EventEmitter();
  @Output()
  onStopClicked: EventEmitter<WorkspaceModel> = new EventEmitter<WorkspaceModel>();

  public selection = new SelectionModel<WorkspaceModel>(true, []);

  displayedColumns = [ 'buttons', 'id', 'os', 'computerName', 'userName', 'state', 'rootVolumeSizeGib', 'userVolumeSizeGib', 'privateIpAddress', 'publicIpAddress', 'runningMode', 'platform', 'protocol'];
  /**
   * Constructor
   */
  constructor(private _router: Router, private _changeDetectorRef: ChangeDetectorRef,)
  {
  }

  start(row: WorkspaceModel) {
    this.onStartClicked.emit(row);
  }

  reboot(row: WorkspaceModel) {
    this.onRebootClicked.emit(row);
  }
  stop(row: WorkspaceModel) {
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

}
