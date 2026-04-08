import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {MatTable, MatTableDataSource} from "@angular/material/table";
import {Router} from "@angular/router";
import {MatSort} from "@angular/material/sort";
import {RunnerGithubModel} from '../../../../../shared/models/github.model';
import {UtilFunctions} from '../../../../../shared/util/util-functions';
import {MatDialog} from '@angular/material/dialog';
import {RunnerInstallerModalComponent} from '../modal/runner-installer-modal.component';
import {RunnerRemoveModalComponent} from '../modal/runner-remove-modal.component';
import {PerfilUsuarioEnum} from '../../../../../shared/models/usuario.model';
import {ConfigSystemService} from "../../../config-system/config-system.service";
import {take, takeUntil} from "rxjs/operators";
import {
  GithubConfigModel,
  SystemConfigModel,
  SystemConfigModelEnum
} from "../../../../../shared/models/system-config.model";
import {Subject} from "rxjs";

@Component({
  selector       : 'runner-table',
  templateUrl    : './runner-table.component.html',
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class RunnerTableComponent implements OnInit, OnDestroy, AfterViewInit
{
  PerfilUsuarioEnum = PerfilUsuarioEnum;
  private _destroy$ = new Subject<void>();
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
  hasVersionClientConfig: boolean = false;

  displayedColumns = [ 'buttons', 'id', 'name', 'os', 'status', 'busy', 'labels.name'];
  /**
   * Constructor
   */
  constructor(
    private _router: Router,
    private _changeDetectorRef: ChangeDetectorRef,
    private _matDialog: MatDialog,
    private _configService: ConfigSystemService,
  )
  {
  }

  ngOnDestroy(): void {
    this._destroy$.next();
    this._destroy$.complete();
  }

  ngOnInit(): void {
    this._configService.get().pipe(take(1), takeUntil(this._destroy$)).subscribe({
      next: (configs: SystemConfigModel[]) => {
        const row = configs.find(c => c.configType === SystemConfigModelEnum.GITHUB);
        const githubConfig = row?.config as GithubConfigModel;
        this.hasVersionClientConfig = githubConfig && UtilFunctions.isValidStringOrArray(githubConfig.runnerInstallerMinVersion);
        this._changeDetectorRef.markForCheck();
      },
      error: () => undefined
    });
  }

  openRunnerInstaller(): void {
    this._matDialog.open(RunnerInstallerModalComponent, {
      width: 'min(880px, 98vw)',
      maxHeight: '90vh',
      panelClass: 'runner-installer-dialog-panel'
    });
  }

  openRemoveRunner(runner: RunnerGithubModel): void {
    this._matDialog.open(RunnerRemoveModalComponent, {
      width: 'min(560px, 96vw)',
      maxHeight: '90vh',
      data: {runner}
    }).afterClosed().subscribe((r: 'refresh' | void) => {
      if (r === 'refresh') {
        this.refresh();
      }
    });
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
