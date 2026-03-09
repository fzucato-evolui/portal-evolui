import {ChangeDetectorRef, Component, OnDestroy, OnInit, ViewEncapsulation} from "@angular/core";
import {ReplaySubject, Subject} from 'rxjs';
import {AmbienteService} from '../ambiente.service';
import {FormArray, FormBuilder, FormGroup, Validators} from '@angular/forms';
import {MessageDialogService} from '../../../../shared/services/message/message-dialog-service';
import {MatDialogRef} from '@angular/material/dialog';
import {AmbienteModel} from "app/shared/models/ambiente.model";
import {ClientModel} from '../../../../shared/models/client.model';
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {VersaoModel} from "app/shared/models/version.model";
import {MatSlideToggleChange} from "@angular/material/slide-toggle";
import {ProjectModel, ProjectModuleModel} from "app/shared/models/project.model";
import {MatSelectChange} from '@angular/material/select';
import {AtualizacaoVersaoModel} from '../../../../shared/models/atualizacao-versao.model';
import {RunnerGithubModel} from '../../../../shared/models/github.model';

@Component({
  selector       : 'ambiente-modal',
  styleUrls      : ['/ambiente-modal.component.scss'],
  templateUrl    : './ambiente-modal.component.html',
  encapsulation  : ViewEncapsulation.None,

  standalone: false
})
export class AmbienteModalComponent implements OnInit, OnDestroy
{

  formSave: FormGroup;
  private _unsubscribeAll: Subject<any> = new Subject<any>();

  public model: AmbienteModel;

  private _initialData: {runners: Array<RunnerGithubModel>, clients: Array<ClientModel>, versions: Array<VersaoModel>, history: Array<AtualizacaoVersaoModel>};
  get initialData(): { runners: Array<RunnerGithubModel>; clients: Array<ClientModel>; versions: Array<VersaoModel>; history: Array<AtualizacaoVersaoModel> } {
    return this._initialData;
  }

  set initialData(value: { runners: Array<RunnerGithubModel>; clients: Array<ClientModel>; versions: Array<VersaoModel>; history: Array<AtualizacaoVersaoModel> }) {

    if (UtilFunctions.isValidStringOrArray(value.history) === true && UtilFunctions.isValidStringOrArray(value.versions)) {
      value.versions.forEach( x => {
        const tagHistory = value.history.filter(y => y.tags.includes(x.tag));
        if (UtilFunctions.isValidStringOrArray(tagHistory) === true) {
          x['tooltip'] = ` ATUALIZADO EM ${UtilFunctions.dateToString(tagHistory[0].conclusionDate, 'DD/MM/YY HH:mm')}`;
        }
      })
    }
    this._initialData = value;
  }
  title: string;
  service: AmbienteService;
  filteredVersionsOptions: ReplaySubject<Array<VersaoModel>> = new ReplaySubject<Array<VersaoModel>>(1);
  private _target: ProjectModel = null;
  public customPatterns = { 'I': { pattern: new RegExp('\[a-zA-Z0-9_\]')} };
  set target(value: ProjectModel) {
    this._target = value;
    this.title = 'Ambiente ' + this._target.title;
  }

  get target(): ProjectModel {
    return  this._target;
  }

  constructor(private _formBuilder: FormBuilder,
              private _cdr: ChangeDetectorRef,
              public dialogRef: MatDialogRef<AmbienteModalComponent>,
              private _messageService: MessageDialogService)
  {
  }

  ngOnInit(): void {

    this.filteredVersionsOptions.next(this.initialData.versions);
    // Create the form
    this.formSave = this._formBuilder.group({
      id: [null],
      tag: ['', Validators.required],
      identifier: ['', Validators.required],
      description: ['', Validators.required],
      client: [null, Validators.required],
      modules: this._formBuilder.array([]),

    });
    this.formSave.patchValue(this.model);

    if (UtilFunctions.isValidStringOrArray(this.target) === true) {
      for (const module of this.target.modules) {
        this.addModule(module);
      }
    }

    // Create the form
    //this.formSave.patchValue(this.model);
  }

  ngOnDestroy(): void {
    this._unsubscribeAll.next(undefined);
    this._unsubscribeAll.complete();
  }


  doSaving() {
    this.model = this.formSave.value;
    this.model.identifier = this.model.identifier.toUpperCase();
    this.model.project = this.target;
    this.service.save(this.model).then(value => {
      this._messageService.open("Atualização de versão foi iniciada com sucesso!", "SUCESSO", "success")
      this.dialogRef.close();
    });
  }

  canSave(): boolean {
    if (this.formSave) {
      return !this.formSave.invalid;
    }
    return false;
  }


  getFilesMap(c: FormGroup): FormArray {
    return c.get('filesMap') as FormArray;
  }

  addFileMap(a: FormArray) {
    const g = this._formBuilder.group({
      source: ['', [Validators.required]],
      destination: ['', [Validators.required]]
    });
    a.push(g);
  }

  deleteFileMap(c: FormGroup, index) {
    (c.get('filesMap') as FormArray).removeAt(index);
  }

  compareFn(v1: RunnerGithubModel | ClientModel , v2: RunnerGithubModel | ClientModel): boolean {
    return v1 && v2 ? v1.id === v2.id : v1 === v2;
  }
  getRunnerString(runner: RunnerGithubModel): string {
    if (UtilFunctions.isValidObject(runner) === false) {
      return '';
    }
    let value: string;
    value = runner.name;
    if (UtilFunctions.isValidStringOrArray(runner.labels) === true) {
      const customLabels = runner.labels.filter(x => x.type === 'custom');
      if (customLabels && customLabels.length > 0) {
        value += ' ('+ customLabels.map((l) => l.name).join(', ') + ')';
      }
    }

    value += ` , ${runner.os},  ${runner.status}`;
    return  value;
  }

  changeEnabled(event: MatSlideToggleChange, group: FormGroup) {

    if (event.checked) {
      group.get('runnerId').enable();
      group.get('destinationPath').enable();
      group.get('destinationServer').enable();
    } else {
      group.get('runnerId').disable();
      group.get('destinationPath').disable();
      group.get('destinationServer').disable();
    }
    this._cdr.detectChanges();
  }

  buildModule(projectModule: ProjectModuleModel): FormGroup {
    let module = null;
    if (this.model && this.model.modules) {
      module = this.model.modules.filter(x => x.projectModule.id === projectModule.id)[0];
    }
    const c = this._formBuilder.group({
      id: [null],
      projectModule: [projectModule],
      tag: ['', [Validators.required]],
      commit: [''],
      repository: [''],
      relativePath: [''],
      config: this._formBuilder.group({
        contextURL: ['', projectModule.main? Validators.required: null],
        contextName: ['', projectModule.main? Validators.required: null],
        lthUser: ['', projectModule.main? Validators.required: null],
        lthPassword: ['', projectModule.main? Validators.required: null],
        jvmOptionsCommand: [''],
        jvmOptionsLiquibase: [''],
        enabled: [true, Validators.required],
        destinationPath: [
          {
            value: '',
            disabled: module && module.config && module.config.enabled === false
          }, projectModule.framework === false? Validators.required: null
        ],
        beforeUpdateModuleCommand: [''],
        afterUpdateModuleCommand: [''],
        runnerId: [
          {
            value: null,
            disabled: module && module.config && module.config.enabled === false
          }, projectModule.framework === false? Validators.required: null
        ],
        destinationServer: this._formBuilder.group({
          host: [''],
          user: [''],
          password: [''],
          port: [22],
          privateKey: [''],
          workDirectory: ['']
        }),
        filesMap: this._formBuilder.array([])
      })
    });
    if (module && module.config && UtilFunctions.isValidStringOrArray(module.config.filesMap)) {
      for (const x of module.config.filesMap) {
        this.addFileMap(c.get('config').get('filesMap') as FormArray);
      }
    }
    c.patchValue(module);
    return c;
  }

  addModule(projectModule: ProjectModuleModel) {
    (this.formSave.get('modules') as FormArray).push(this.buildModule(projectModule));
  }

  getModules(): FormArray {
    return this.formSave.get('modules') as FormArray;
  }

  getModule(projectModule: ProjectModuleModel): FormGroup {
    return this.getModules().controls.filter(x => {
      return x.get('projectModule').value['identifier'] === projectModule.identifier;
    })[0] as FormGroup;
  }

  onTagChanged(e: MatSelectChange) {
    const value = e.value;
    const values = this.initialData.versions.filter(x => x.tag.startsWith(value));
    values[0].modules.forEach(x => {
      const fg = this.getModule(x.projectModule);
      fg.get('tag').setValue(x.tag);
      fg.get('commit').setValue(x.commit);
    })
  }
  onSearchChange(value: string) {
    const values = this.initialData.versions.filter(x => x.tag.startsWith(value));
    this.filteredVersionsOptions.next(values);
    const currentTag = this.formSave.get('tag').value;

    if (UtilFunctions.isValidStringOrArray(currentTag) && (values.length === 0 || values.filter(x => x.tag === currentTag).length === 0)) {
      console.log('passou 0');
      this.formSave.get('tag').setValue(null);
    }

  }

  getProductTag(projectModule: ProjectModel, tag: string): string {
    if (UtilFunctions.isValidStringOrArray(tag) === true) {
      return this.initialData.versions.filter(x => x.tag === tag)[0].modules.filter(x => x.projectModule.id === projectModule.id)[0].tag;
    }
    return null;
  }

  getProductCommit(projectModule: ProjectModel, tag: string): string {
    if (UtilFunctions.isValidStringOrArray(tag) === true) {
      return this.initialData.versions.filter(x => x.tag === tag)[0].modules.filter(x => x.projectModule.id === projectModule.id)[0].commit;
    }
    return null;
  }
}
