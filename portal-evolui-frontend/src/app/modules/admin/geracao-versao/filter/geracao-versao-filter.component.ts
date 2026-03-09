import {ChangeDetectorRef, Component, OnDestroy, OnInit, ViewEncapsulation} from '@angular/core';
import {ClassyLayoutComponent} from "../../../../layout/layouts/classy/classy.component";
import {GeracaoVersaoComponent} from '../geracao-versao.component';
import {Subject} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import {VersionConclusionEnum, VersionStatusEnum, VersionTypeEnum} from 'app/shared/models/version.model';
import {GeracaoVersaoFilterModel} from 'app/shared/models/geracao-versao.model';
import {ProjectModel} from 'app/shared/models/project.model';


@Component({
  selector       : 'geracao-versao-filter',
  templateUrl    : './geracao-versao-filter.component.html',
  styleUrls      : ['./geracao-versao-filter.component.scss'],
  encapsulation  : ViewEncapsulation.None,

  standalone: false
})
export class GeracaoVersaoFilterComponent implements OnInit, OnDestroy
{
  public model: GeracaoVersaoFilterModel = new GeracaoVersaoFilterModel();
  private _onDestroy = new Subject<void>();
  private _target: ProjectModel;

  set target(value: ProjectModel) {
    this._target = value;
    this._changeDetectorRef.detectChanges();
  }

  get target() {
    return this._target;
  }
  VersionTypeEnum = VersionTypeEnum;
  VersionStatusEnum = VersionStatusEnum;
  VersionConclusionEnum = VersionConclusionEnum;
  indeterminate: boolean = true;

  /**
   * Constructor
   */
  constructor(public parent: ClassyLayoutComponent, private _changeDetectorRef: ChangeDetectorRef,  private _parentComponent: GeracaoVersaoComponent) {

  }

  filtrar() {
    this._parentComponent.service.filter(this.model);
  }

  ngOnInit(): void {
    this._parentComponent.service.target$.pipe((takeUntil(this._onDestroy)))
      .subscribe((value: ProjectModel) => {
        this.target = value;
      });
  }

  ngOnDestroy(): void {
    this._onDestroy.next();
    this._onDestroy.complete();
  }

  limpar() {
    this.model = new GeracaoVersaoFilterModel();
  }

}
