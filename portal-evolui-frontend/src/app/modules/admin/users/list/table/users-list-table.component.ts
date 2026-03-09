import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {Router} from "@angular/router";
import {MatSort} from "@angular/material/sort";
import {SelectionModel} from "@angular/cdk/collections";
import {PerfilUsuarioEnum, UsuarioModel} from "../../../../../shared/models/usuario.model";

@Component({
  selector       : 'users-list-table',
  templateUrl    : './users-list-table.component.html',
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class UsersListTableComponent implements AfterViewInit
{
  @ViewChild(MatSort) sort: MatSort;
  @Input()
  multiple = false
  @Input()
  showActionsButtons = true
  @Input()
  showFastFilter = true
  @Input()
  dataSource = new MatTableDataSource<UsuarioModel>();
  @Output()
  onRowClicked: EventEmitter<UsuarioModel> = new EventEmitter<UsuarioModel>();
  @Output()
  onDeleteClicked: EventEmitter<UsuarioModel> = new EventEmitter<UsuarioModel>();
  @Output()
  onAddClicked: EventEmitter<any> = new EventEmitter();
  @Output()
  onEditClicked: EventEmitter<UsuarioModel> = new EventEmitter<UsuarioModel>();

  @Output()
  onConfigClicked: EventEmitter<UsuarioModel> = new EventEmitter<UsuarioModel>();
  public selection = new SelectionModel<UsuarioModel>(true, []);

  displayedColumns = [ 'buttons', 'imagem', 'ativo', 'id', 'perfil', 'nome', 'login', 'email'];
  PerfilUsuarioEnum = PerfilUsuarioEnum;
  /**
   * Constructor
   */
  constructor(private _router: Router)
  {
  }

  getRecord(row) {
    this.onRowClicked.emit(row);
  }

  edit(row: UsuarioModel) {
    this.onEditClicked.emit(row);
  }

  delete(row: UsuarioModel) {
    this.onDeleteClicked.emit(row);
  }

  adicionar() {
    //this._router.navigate(['/admin/users', -1]);
    this.onAddClicked.emit(null);
  }




  ngAfterViewInit(): void {
    this.dataSource.sort = this.sort;
  }


  announceSortChange($event) {
    //console.log($event);
  }

  clearImage(model: UsuarioModel) {
    model.image = 'assets/images/noPicture.png';
  }

  fastFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();
  }

  config(row: UsuarioModel) {
    this.onConfigClicked.emit(row);
    this.onRowClicked.emit(row);
  }

}
