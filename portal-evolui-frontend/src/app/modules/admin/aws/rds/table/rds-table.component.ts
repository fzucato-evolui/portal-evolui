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
import {RDSModel} from "../../../../../shared/models/rds.model";

@Component({
  selector       : 'rds-table',
  templateUrl    : './rds-table.component.html',
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class RdsTableComponent implements AfterViewInit
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
  dataSource = new MatTableDataSource<RDSModel>();
  @Output()
  onStartClicked: EventEmitter<RDSModel> = new EventEmitter<RDSModel>();
  @Output()
  onRefreshClicked: EventEmitter<any> = new EventEmitter();
  @Output()
  onStopClicked: EventEmitter<RDSModel> = new EventEmitter<RDSModel>();
  @Output()
  onRowDoubleClicked: EventEmitter<RDSModel> = new EventEmitter<RDSModel>();

  public selection = new SelectionModel<RDSModel>(true, []);

  displayedColumns = [ 'buttons', 'id', 'endpoint', 'status', 'engine', 'port', 'dbName', 'privateIpAddress', 'publicIpAddress', 'instanceType'];
  /**
   * Constructor
   */
  constructor(private _router: Router, private _changeDetectorRef: ChangeDetectorRef,)
  {
  }

  start(row: RDSModel) {
    this.onStartClicked.emit(row);
  }

  stop(row: RDSModel) {
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

  rowDoubleClicked(row: RDSModel, event: MouseEvent) {
    const target = event.target as HTMLElement;

    const cell = target.closest('td');
    console.log("cell", cell);
    if (!cell) return;

    const cellIndex = Array.from(cell.parentElement!.children).indexOf(cell);
    if (cellIndex !== 0) {
      this.onRowDoubleClicked.emit(row);
    }
  }
}
