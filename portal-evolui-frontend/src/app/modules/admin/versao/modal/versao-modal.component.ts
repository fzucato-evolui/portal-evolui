import {Component, OnDestroy, OnInit, ViewEncapsulation} from "@angular/core";
import {VersaoModel, VersionTypeEnum,} from '../../../../shared/models/version.model';
import {Subject} from 'rxjs';
import {VersaoService} from '../versao.service';
import {FormArray, FormBuilder, FormGroup, Validators} from '@angular/forms';
import {MessageDialogService} from '../../../../shared/services/message/message-dialog-service';
import {MatDialogRef} from '@angular/material/dialog';
import {ProjectModel, ProjectModuleModel} from '../../../../shared/models/project.model';
import {UtilFunctions} from '../../../../shared/util/util-functions';

@Component({
  selector       : 'versao-modal',
  styleUrls      : ['/versao-modal.component.scss'],
  templateUrl    : './versao-modal.component.html',
  encapsulation  : ViewEncapsulation.None,

  standalone: false
})
export class VersaoModalComponent implements OnInit, OnDestroy
{
  formSave: FormGroup;
  private _unsubscribeAll: Subject<any> = new Subject<any>();

  public model: VersaoModel;
  VersionTypeEnum = VersionTypeEnum;
  title: string;
  service: VersaoService;
  private _target: ProjectModel = null;

  public customPatterns = { 'I': { pattern: new RegExp('\[a-zA-Z0-9_\]')} };

  set target(value: ProjectModel) {
    this._target = value;
    this.title = 'Versão ' + this._target.title;
  }

  get target(): ProjectModel {
    return  this._target;
  }
  constructor(private _formBuilder: FormBuilder,
              public dialogRef: MatDialogRef<VersaoModalComponent>,
              private _messageService: MessageDialogService)
  {
  }

  ngOnInit(): void {
    this.formSave = this._formBuilder.group({
      id: [null],
      tag: ['', Validators.required],
      versionType: [null, Validators.required],
      modules: this._formBuilder.array([])
    });
    if (UtilFunctions.isValidStringOrArray(this.target) === true) {
      for (const module of this.target.modules) {
        this.addModule(module);
      }
    }

    this.formSave.patchValue({
      id: this.model.id,
      tag: this.model.tag,
      versionType: this.model.versionType
    });

    if (this.model.modules) {
      const formModules = this.getModules();
      for (let i = 0; i < formModules.length; i++) {
        const projectModule = this.target.modules[i];
        const modelModule = this.model.modules.find(m => m.projectModule?.id === projectModule.id);
        if (modelModule) {
          formModules.at(i).patchValue(modelModule);
        }
      }
    }


  }

  ngOnDestroy(): void {
    this._unsubscribeAll.next(undefined);
    this._unsubscribeAll.complete();
  }


  doSaving() {
    this.model = this.formSave.value;
    this.model.project = this.target;
    this.service.save(this.model).then(value => {
      this._messageService.open("Versão salva com sucesso!", "SUCESSO", "success")
      this.dialogRef.close();
    });
  }

  canSave(): boolean {
    if (this.formSave) {
      return !this.formSave.invalid;
    }
    return false;
  }

  buildModule(produto: ProjectModuleModel): FormGroup {
    const c = this._formBuilder.group({
      id: [null],
      projectModule: [produto],
      tag: ['', [Validators.required]],
      commit: [''],
      repository: [''],
      relativePath: ['']
    });
    return c;
  }

  addModule(produto: ProjectModuleModel) {
    (this.formSave.get('modules') as FormArray).push(this.buildModule(produto));
  }

  getModules(): FormArray {
    return this.formSave.get('modules') as FormArray;
  }


}
