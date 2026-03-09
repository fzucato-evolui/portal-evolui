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
import {EC2Model} from "../../../../../shared/models/ec2.model";

@Component({
  selector       : 'ec2-table',
  templateUrl    : './ec2-table.component.html',
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class Ec2TableComponent implements AfterViewInit
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
  dataSource = new MatTableDataSource<EC2Model>();
  @Output()
  onStartClicked: EventEmitter<EC2Model> = new EventEmitter<EC2Model>();
  @Output()
  onRefreshClicked: EventEmitter<any> = new EventEmitter();
  @Output()
  onStopClicked: EventEmitter<EC2Model> = new EventEmitter<EC2Model>();
  @Output()
  onRebootClicked: EventEmitter<EC2Model> = new EventEmitter<EC2Model>();

  public selection = new SelectionModel<EC2Model>(true, []);

  displayedColumns = [ 'buttons', 'id', 'name', 'status', 'os', 'privateIp', 'publicIp', 'instanceType'];
  /**
   * Constructor
   */
  constructor(private _router: Router, private _changeDetectorRef: ChangeDetectorRef,)
  {
  }

  start(row: EC2Model) {
    this.onStartClicked.emit(row);
  }

  stop(row: EC2Model) {
    this.onStopClicked.emit(row);
  }

  reboot(row: EC2Model) {
    this.onRebootClicked.emit(row);
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
