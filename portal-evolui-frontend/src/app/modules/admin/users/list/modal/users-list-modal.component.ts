import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from "@angular/core";
import {MatTableDataSource} from "@angular/material/table";
import {UsuarioListService} from "../usuario-list.service";
import {Subject} from "rxjs";
import {UsuarioFilterModel, UsuarioModel} from "../../../../../shared/models/usuario.model";
import {UtilFunctions} from "../../../../../shared/util/util-functions";
import {MessageDialogService} from "../../../../../shared/services/message/message-dialog-service";
import {UsersListTableComponent} from "../table/users-list-table.component";
import {UsersListFilterComponent} from "../filter/users-list-filter.component";

@Component({
    selector       : 'users-list-modal',
    styleUrls      : ['/users-list-modal.component.scss'],
    templateUrl    : './users-list-modal.component.html',
    encapsulation  : ViewEncapsulation.None,

    standalone: false
})
export class UsersListModalComponent implements OnInit, AfterViewInit, OnDestroy
{
    @ViewChild('filtro', {static: false}) filtroComponent: UsersListFilterComponent;
    @ViewChild('tabela', {static: false}) tableComponent: UsersListTableComponent;
    @Input()
    multiple = false
    dataSource = new MatTableDataSource<UsuarioModel>();
    drawerMode: 'over' | 'side' = 'side';
    drawerOpened: boolean = true;
    preFilter: UsuarioFilterModel;
    defaultFilters: UsuarioFilterModel = null;
    onDataSelected: EventEmitter<UsuarioModel> = new EventEmitter<UsuarioModel>();
    onDataMultipleSelected: EventEmitter<Array<UsuarioModel>> = new EventEmitter<Array<UsuarioModel>>();
    showTableButtons = true;
    showTableFastFilter = true;
    private _unsubscribeAll: Subject<any> = new Subject<any>();
    constructor(private _changeDetectorRef: ChangeDetectorRef,
                private _service: UsuarioListService,
                //private _fuseMediaWatcherService: any,
                private _messageService: MessageDialogService
                )
    {
    }

    filtrar(filtro: UsuarioFilterModel) {
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

    ngAfterViewInit(): void {
        if (UtilFunctions.isValidObject(this.preFilter) === true) {
            //this.filtroComponent.filtrar();
        }
    }

    ngOnInit(): void
    {


      /*
        // Subscribe to media changes
        this._fuseMediaWatcherService.onMediaChange$
            .pipe(takeUntil(this._unsubscribeAll))
            .subscribe(({matchingAliases}) => {

                // Set the drawerMode and drawerOpened
                if ( matchingAliases.includes('lg') )
                {
                    this.drawerMode = 'side';
                    this.drawerOpened = true;
                }
                else
                {
                    this.drawerMode = 'over';
                    this.drawerOpened = false;
                }

                // Mark for check
                this._changeDetectorRef.markForCheck();
            });
            */
    }

    /**
     * On destroy
     */
    ngOnDestroy(): void
    {
        // Unsubscribe from all subscriptions
        this._unsubscribeAll.next(undefined);
        this._unsubscribeAll.complete();
    }

    selectData(model: UsuarioModel) {
        if (this.multiple === false) {
            this.onDataSelected.emit(model);
        } else {
            this.onDataMultipleSelected.emit([model]);
        }
    }

    delete(row: UsuarioModel) {
        this._messageService.open('Deseja realmente remover o usuário?', 'CONFIRMAÇÃO', 'confirm').subscribe((result) => {
            if (result === 'confirmed') {
                const index = this.dataSource.data.findIndex(r => r.id === row.id);
                if (index >= 0) {
                    this._service.delete(row.id).then(value => {
                        this.dataSource.data.splice(index,1);
                        this.dataSource._updateChangeSubscription();
                        this._messageService.open('Usuário removido com sucesso', 'SUCESSO', 'success');
                    })
                }
            }
        });
    }

    selectedMultiple() {
        this.onDataMultipleSelected.emit(this.tableComponent.selection.selected);
    }

    hasSelected(): boolean {
        if (UtilFunctions.isValidObject(this.tableComponent) === true) {
            return this.tableComponent.selection.isEmpty() === false;
        }
        return false;
    }
}
