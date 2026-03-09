import {Component, OnDestroy, OnInit, ViewEncapsulation} from "@angular/core";
import {AtualizacaoVersaoModel} from '../../../../shared/models/atualizacao-versao.model';
import {UserService} from '../../../../shared/services/user/user.service';
import {takeUntil} from 'rxjs/operators';
import {ReplaySubject, Subject} from 'rxjs';
import {UsuarioModel} from '../../../../shared/models/usuario.model';
import {AtualizacaoVersaoService} from '../atualizacao-versao.service';
import {FormArray, FormBuilder, FormGroup, Validators} from '@angular/forms';
import {MessageDialogService} from '../../../../shared/services/message/message-dialog-service';
import {MatDialogRef} from '@angular/material/dialog';
import {VersaoModel, VersionModel, VersionStatusEnum, VersionTypeEnum} from "app/shared/models/version.model";
import {AmbienteModel, AmbienteModuloModel} from '../../../../shared/models/ambiente.model';
import {EvoluiVersionModel} from '../../../../shared/models/evolui-version.model';
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {ProjectModel} from '../../../../shared/models/project.model';
import {MatSlideToggleChange} from "@angular/material/slide-toggle";
import {MAT_DATE_FORMATS} from '@angular/material/core';

export type ModuleStatusType = 'DESABILITADO' | 'ATUALIZADO' | 'DESATUALIZADO' | 'INDISPONIVEL';

@Component({
  selector       : 'atualizacao-versao-modal',
  styleUrls      : ['/atualizacao-versao-modal.component.scss'],
  templateUrl    : './atualizacao-versao-modal.component.html',
  encapsulation  : ViewEncapsulation.None,
  providers: [
    {
      provide: MAT_DATE_FORMATS,
      useValue: {
        parse: { dateInput: null },
        display: {
          dateInput: { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' },
          monthYearLabel: { year: 'numeric', month: 'short' },
          dateA11yLabel: { year: 'numeric', month: 'long', day: 'numeric' },
          monthYearA11yLabel: { year: 'numeric', month: 'long' },
        },
      },
    },
  ],

  standalone: false
})
export class AtualizacaoVersaoModalComponent implements OnInit, OnDestroy
{
  formSave: FormGroup;
  private _unsubscribeAll: Subject<any> = new Subject<any>();

  public model: AtualizacaoVersaoModel;
  public modules: {[key:string]: {
      status: ModuleStatusType,
      currentVersion: string,
      availableVersion: string
    }}
  VersionTypeEnum = VersionTypeEnum;
  title: string;
  user: UsuarioModel;
  service: AtualizacaoVersaoService;
  private _initialData: {versions: Array<VersaoModel>, environments: Array<AmbienteModel>, history: Array<AtualizacaoVersaoModel>};
  get initialData(): { versions: Array<VersaoModel>; environments: Array<AmbienteModel>; history: Array<AtualizacaoVersaoModel> } {
    return this._initialData;
  }

  set initialData(value: { versions: Array<VersaoModel>; environments: Array<AmbienteModel>; history: Array<AtualizacaoVersaoModel> }) {
    if (UtilFunctions.isValidStringOrArray(value.versions) === true && UtilFunctions.isValidStringOrArray(value.environments) === true && UtilFunctions.isValidStringOrArray(history) === true) {

      value.environments.forEach(x => {
        const currentVersionIndex = value.versions.findIndex(y => y.tag === x.tag);
        if (currentVersionIndex >= 0) {
          const currentVersion = value.versions[currentVersionIndex];
          if (currentVersion.beta === true) {
            const environmentsVersions = value.history.filter(y => y.environment.id === x.id);
            if (UtilFunctions.isValidStringOrArray(environmentsVersions) === true) {
              for (const y of value.versions) {
                if (!y.beta && new EvoluiVersionModel(y.tag).customCompare(new EvoluiVersionModel(x.tag)) < 0) {
                  const tagHistory = environmentsVersions.filter(k => k.tags.includes(y.tag));
                  if (UtilFunctions.isValidStringOrArray(tagHistory) === false) {
                    if (UtilFunctions.isValidStringOrArray(x['missingVersions']) === false) {
                      x['missingVersions'] = new Array<string>();
                    }
                    (x['missingVersions'] as Array<string>).push(y.tag);
                  }
                  else {
                    break;
                  }
                }
              }

            }
          }
        }


      })
    }
    this._initialData = value;
  }
  private _target: ProjectModel = null;
  possibleVersions: ReplaySubject<Array<VersaoModel>> = new ReplaySubject<Array<VersaoModel>>(1);
  set target(value: ProjectModel) {
    this._target = value;
    this.title = 'Atualização Versão ' + this._target.title;
  }

  get target(): ProjectModel {
    return  this._target;
  }
  constructor(public _userService: UserService,
              private _formBuilder: FormBuilder,
              public dialogRef: MatDialogRef<AtualizacaoVersaoModalComponent>,
              private _messageService: MessageDialogService)
  {
  }

  ngOnInit(): void {


    this._userService.user$
      .pipe((takeUntil(this._unsubscribeAll)))
      .subscribe((user: UsuarioModel) => {
        this.user = user;
      });
    // Create the form
    this.formSave = this._formBuilder.group({
      tag: ['', [Validators.required]],
      status: [VersionStatusEnum.queued],
      schedulerDate: [{value: null, disabled: true}],
      environment: [null, [Validators.required]],
      modules: this._formBuilder.array([])
    });


    this.possibleVersions.next([]);
    this.formSave.get('environment').valueChanges
      .pipe(takeUntil(this._unsubscribeAll))
      .subscribe(() => {
        this.filterVersions();
      });
    this.formSave.get('tag').valueChanges
      .pipe(takeUntil(this._unsubscribeAll))
      .subscribe(() => {
        this.fillModules();
      });
  }

  ngOnDestroy(): void {
    this._unsubscribeAll.next(undefined);
    this._unsubscribeAll.complete();
  }


  doSaving() {
    this.model = this.formSave.getRawValue();
    if (UtilFunctions.isValidStringOrArray(this.model.environment['missingVersions']) === true) {
      const builds = (this.model.environment['missingVersions'] as Array<string>).join("; ")
      this._messageService.open(`O ambiente escolhido pra atualização está em uma versão BETA e possui as seguintes builds que não foram aplicadas:</br> ${builds}.</br> Para aplicá-las, é preciso fazer o downgrade da versão do ambiente. Deseja realmente prosseguir sem aplicar essas versões?`, 'CONFIRMAÇÃO', 'confirm').subscribe((result) => {
        if (result === 'confirmed') {
          this.service.save(this.model).then(value => {
            this._messageService.open("Geração de nova versão foi iniciada com sucesso!", "SUCESSO", "success")
            this.dialogRef.close();
          });

        }
      });
    }
    else {
      this.service.save(this.model).then(value => {
        this._messageService.open("Geração de nova versão foi iniciada com sucesso!", "SUCESSO", "success")
        this.dialogRef.close();
      });

    }
  }

  canSave(): boolean {
    if (this.formSave) {
      return !this.formSave.invalid;
    }
    return false;
  }


  noImage() {
    this.user.image = null;
  }

  protected filterVersions() {
    this.modules = null;
    if (!this._initialData.versions) {
      return;
    }
    // get the search keyword
    let search = this.formSave.get('environment').value as AmbienteModel;
    if (!search) {
      this.possibleVersions.next([]);
      return;
    }
    // filter the banks
    this.possibleVersions.next(
      this._initialData.versions.filter(y => {
        const vAmbiente = new EvoluiVersionModel(search.tag);
        const v = new EvoluiVersionModel(y.tag);
        return v.customCompare(vAmbiente) >= 0;
      })
    );

  }

  protected fillModules() {
    this.getModules().clear();

    // get the search keyword
    let search = this.formSave.get('tag').value as string;
    if (UtilFunctions.isValidStringOrArray(search) === false) {
      this.modules = null;
      return;
    }
    const ambiente = this.formSave.get('environment').value as AmbienteModel;
    const version = this._initialData.versions.filter(x => x.tag === search)[0];
    const mod: {[key:string]: {
      status: ModuleStatusType,
        currentVersion: string,
        availableVersion: string
    }} = {}

    for (const ambienteModule of ambiente.modules) {
      const c = this.addModule(ambienteModule);
      const produto = ambienteModule.projectModule;
      const k = produto.identifier;

      const v = version.modules.filter(x => x.projectModule.id === produto.id)[0] as VersionModel;
      const config = ambienteModule.config;
      let status: ModuleStatusType = 'ATUALIZADO';
      let ambienteVersion = UtilFunctions.isValidStringOrArray(ambienteModule.tag) === false ? '' : ambienteModule.tag;
      let availableVersion = !v || UtilFunctions.isValidStringOrArray(v.tag) === false ? '' : v.tag;
      if (config.enabled === false) {
        status = 'DESABILITADO';
      }
      else if (UtilFunctions.isValidStringOrArray(availableVersion) === false) {
        status = 'INDISPONIVEL';
      }
      else if (UtilFunctions.isValidStringOrArray(ambienteVersion) === false) {
        status = 'DESATUALIZADO';
      }
      else {
        if (new EvoluiVersionModel(ambienteVersion).customCompare(new EvoluiVersionModel(availableVersion)) < 0) {
          status = 'DESATUALIZADO';
        } else {
          status = 'ATUALIZADO';
        }
      }
      mod[k] = {
        status: status,
        currentVersion: ambienteVersion,
        availableVersion: availableVersion
      }
      c.get('enabled').setValue(mod[k].status === 'DESATUALIZADO');
    }


    this.modules = mod;


  }

  compareFn(v1: AmbienteModel , v2: AmbienteModel): boolean {
    return v1 && v2 ? v1.id === v2.id : v1 === v2;
  }
  buildModule(environmentModule: AmbienteModuloModel): FormGroup {
    const c = this._formBuilder.group({
      id: [null],
      environmentModule: [environmentModule],
      enabled: [true, [Validators.required]],
      executeUpdateCommands: [true, [Validators.required]]
    });
    return c;
  }

  addModule(environmentModule: AmbienteModuloModel): FormGroup {
    const c = this.buildModule(environmentModule);
    (this.formSave.get('modules') as FormArray).push(c);
    return  c;
  }

  getModules(): FormArray {
    return this.formSave.get('modules') as FormArray;
  }

  changeScheduler(event: MatSlideToggleChange) {
    const schedulerDateCtrl = this.formSave.get("schedulerDate");
    if (event.checked) {
      this.formSave.get("status").setValue(VersionStatusEnum.scheduled);
      schedulerDateCtrl.enable();
      schedulerDateCtrl.setValue(null);
      schedulerDateCtrl.setValidators(Validators.required);
    } else {
      this.formSave.get("status").setValue(VersionStatusEnum.queued);
      schedulerDateCtrl.setValue(null);
      schedulerDateCtrl.clearValidators();
      schedulerDateCtrl.disable();
    }
    schedulerDateCtrl.updateValueAndValidity();
  }

  getMinDate() {
    const now = new Date();
    const minDate = new Date();
    minDate.setMinutes(now.getMinutes() + 1);
    return minDate;
  }

  getEnvironmentVersion(t: AmbienteModel): string {
    const v = this._initialData.versions.filter(x => x.tag === t.tag)[0];
    return t.identifier + '(' + t.tag + (v.beta === true ? ' (BETA)': '') +')';
  }
}
