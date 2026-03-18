import {Component, OnDestroy, OnInit, ViewEncapsulation} from "@angular/core";
import {
  VersionGenerationModuleRequestModel,
  VersionGenerationRequestModel
} from '../../../../shared/models/geracao-versao.model';
import {UserService} from '../../../../shared/services/user/user.service';
import {takeUntil} from 'rxjs/operators';
import {Subject} from 'rxjs';
import {UsuarioModel} from '../../../../shared/models/usuario.model';
import {GeracaoVersaoService} from '../geracao-versao.service';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormGroup,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import {MessageDialogService} from '../../../../shared/services/message/message-dialog-service';
import {MatDialog, MatDialogRef} from '@angular/material/dialog';
import {VersaoModel, VersionTypeEnum} from "app/shared/models/version.model";
import {ProjectModel, ProjectModuleModel} from "app/shared/models/project.model";
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {EvoluiVersionModel} from '../../../../shared/models/evolui-version.model';
import {GeracaoVersaoGitRefModalComponent, GitRefSelectionResult} from './geracao-versao-git-ref-modal.component';

@Component({
  selector       : 'geracao-versao-modal',
  styleUrls      : ['/geracao-versao-modal.component.scss'],
  templateUrl    : './geracao-versao-modal.component.html',
  encapsulation  : ViewEncapsulation.None,

  standalone: false
})
export class GeracaoVersaoModalComponent implements OnInit, OnDestroy
{
  formSave: FormGroup;
  private _unsubscribeAll: Subject<any> = new Subject<any>();

  VersionTypeEnum = VersionTypeEnum;
  title: string;
  user: UsuarioModel;
  service: GeracaoVersaoService;
  private _projectVersions: Array<VersaoModel>;
  nextBranchByCompileType: {[key: string]: Array<EvoluiVersionModel>} = {};

  get projectVersions(): Array<VersaoModel> {
    return this._projectVersions;
  }

  set projectVersions(value: Array<VersaoModel>) {
    if (UtilFunctions.isValidStringOrArray(value)) {
      this._projectVersions = value.sort((x, y) =>
        new EvoluiVersionModel(x.tag).customCompare(new EvoluiVersionModel(y.tag))
      );

      const groupedByBranch = this._projectVersions.reduce((acc, version) => {
        (acc[version.branch] ??= []).push(version);
        return acc;
      }, {} as Record<string, VersaoModel[]>);

      const pushNext = (type: VersionTypeEnum, version: EvoluiVersionModel) => {
        (this.nextBranchByCompileType[type] ??= []).push(version);
      };

      const entries = Object.entries(groupedByBranch);
      for (let i = 0; i < entries.length; i++) {
        const [branch, versions] = entries[i];
        const isLast = i === entries.length - 1;

        const stableBuilds = versions.filter(v =>
          v.versionType === VersionTypeEnum.stable ||
          v.versionType === VersionTypeEnum.patch
        );

        if (stableBuilds.length) {

          const lastStable = stableBuilds[stableBuilds.length - 1];
          const next = new EvoluiVersionModel(lastStable.tag);
          next.incrementBuildNumber();

          pushNext(VersionTypeEnum.patch, next);
          continue;
        }
        if (!isLast) {
          continue;
        }

        pushNext(VersionTypeEnum.stable, new EvoluiVersionModel(branch));
        pushNext(VersionTypeEnum.rc, new EvoluiVersionModel(branch));

        const rcBuilds = versions.filter(v => v.versionType === VersionTypeEnum.rc);
        const betaBuilds = versions.filter(v => v.versionType === VersionTypeEnum.beta);

        if (!rcBuilds.length) {
          pushNext(VersionTypeEnum.beta, new EvoluiVersionModel(branch));

          if (!betaBuilds.length) {
            pushNext(VersionTypeEnum.alpha, new EvoluiVersionModel(branch));
          }
        }
      }

      const lastBranch = this._projectVersions[this._projectVersions.length - 1].branch;
      const lastBuild = new EvoluiVersionModel(lastBranch);

      const nextBranch = new EvoluiVersionModel(
        `${lastBuild.major}.${lastBuild.minor}.${lastBuild.patch + 1}`
      );

      [
        VersionTypeEnum.stable,
        VersionTypeEnum.rc,
        VersionTypeEnum.beta,
        VersionTypeEnum.alpha
      ].forEach(type => {
        if (!this.nextBranchByCompileType[type]?.length) {
          this.nextBranchByCompileType[type] = [nextBranch];
        }
      });

    }
  }

  private _target: ProjectModel = null;

  set target(value: ProjectModel) {
    this._target = value;
    this.title = 'Geração Versão ' + this._target.title;
  }

  get target(): ProjectModel {
    return  this._target;
  }

  constructor(public _userService: UserService,
              private _formBuilder: FormBuilder,
              public dialogRef: MatDialogRef<GeracaoVersaoModalComponent>,
              private _messageService: MessageDialogService,
              private _dialog: MatDialog)
  {
  }

  ngOnInit(): void {
    this._userService.user$
      .pipe((takeUntil(this._unsubscribeAll)))
      .subscribe((user: UsuarioModel) => {
        this.user = user;
      });

    this.formSave = this._formBuilder.group({
      compileType: ['', [Validators.required]],
      branch: ['', [Validators.required, this.branchValidator()]],
      modules: this._formBuilder.array([])
    });

    if (UtilFunctions.isValidStringOrArray(this.target) === true) {
      for (const module of this.target.modules) {
        this.addModule(module);
      }
    }

    this.formSave.get('compileType').valueChanges.subscribe(val => {
      if (val === VersionTypeEnum.patch) {
        this.formSave.get('branch').setValue(null);
      } else {
        this.formSave.get('branch').setValue(this.nextBranchByCompileType[val][0].version);
      }
      this.formSave.get('branch').updateValueAndValidity();
    });
  }

  ngOnDestroy(): void {
    this._unsubscribeAll.next(undefined);
    this._unsubscribeAll.complete();
    this.service.clearBranchesCache();
  }

  doSaving() {
    const request = new VersionGenerationRequestModel();
    request.id = this.target.id;
    request.compileType = this.formSave.get('compileType').value;
    request.tag = this.formSave.get('branch').value;
    request.modules = this.getModules().controls
      .filter(m => !m.get('produto').value.framework || m.get('produto').value.main)
      .map(m => {
        const mod = new VersionGenerationModuleRequestModel();
        mod.id = m.get('produto').value.id;
        mod.branch = m.get('branch').value;
        mod.commit = m.get('commit').value;
        mod.enabled = m.get('enabled').value;
        return mod;
      });

    if (request.compileType !== VersionTypeEnum.stable || this.target.framework === true) {
      this.service.save(request).then(value => {
        this._messageService.open("Geração de nova versão foi iniciada com sucesso!", "SUCESSO", "success")
        this.dialogRef.close();
      });
    }
    else {
      this._messageService.open('Você está prestes a gerar uma versão stable. O metadados de versão já foi gerado? Deseja realmente prosseguir?', 'CONFIRMAÇÃO', 'confirm').subscribe((result) => {
        if (result === 'confirmed') {
          this.service.save(request).then(value => {
            this._messageService.open("Geração de nova versão foi iniciada com sucesso!", "SUCESSO", "success")
            this.dialogRef.close();
          });

        }
      });
    }
  }

  canSave(): boolean {
    if (this.formSave) {
      if (this.formSave.invalid) {
        return false;
      }
      const modules = this.getModules();
      const enabledModules = modules.controls.filter(m =>
        m.get('enabled').value && (!m.get('produto').value.framework || m.get('produto').value.main)
      );
      if (enabledModules.length === 0) {
        return false;
      }
      for (const m of enabledModules) {
        if (!m.get('branch').value && !m.get('commit').value) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  noImage() {
    this.user.image = null;
  }

  buildModule(produto: ProjectModuleModel): FormGroup {
    return this._formBuilder.group({
      id: [produto.id],
      produto: [produto],
      enabled: [true],
      branch: [''],
      commit: ['']
    });
  }

  addModule(produto: ProjectModuleModel) {
    const moduleGroup = this.buildModule(produto);
    (this.formSave.get('modules') as FormArray).push(moduleGroup);

    moduleGroup.get('enabled').valueChanges.subscribe(enabled => {
      if (enabled && produto.bond) {
        const parentCtrl = this.getModules().controls.find(
          m => m.get('produto').value.id === produto.bond.id
        );
        if (parentCtrl && !parentCtrl.get('enabled').value) {
          parentCtrl.get('enabled').setValue(true);
        }
      }
      if (!enabled) {
        for (const child of this.getModules().controls) {
          const childProduto = child.get('produto').value;
          if (childProduto.bond?.id === produto.id && child.get('enabled').value) {
            child.get('enabled').setValue(false);
          }
        }
      }
    });
  }

  getModules(): FormArray {
    return this.formSave.get('modules') as FormArray;
  }

  isBondChild(moduleControl: AbstractControl): boolean {
    const produto: ProjectModuleModel = moduleControl.get('produto').value;
    return produto?.bond != null;
  }

  getBondDepth(moduleControl: AbstractControl): number {
    let depth = 0;
    let produto: ProjectModuleModel = moduleControl.get('produto').value;

    while (produto?.bond) {
      depth++;
      produto = produto.bond as ProjectModuleModel;
    }

    return depth;
  }

  getBondClass(moduleControl: AbstractControl): string {
    return `module-card--level-${Math.min(this.getBondDepth(moduleControl), 3)}`;
  }

  getModuleBadgeLabel(moduleControl: AbstractControl): string {
    const depth = this.getBondDepth(moduleControl);
    if (depth === 0) {
      return 'Módulo raiz';
    }
    return `Bond nível ${depth}`;
  }

  openGitRefModal(moduleControl: AbstractControl): void {
    console.log(this.formSave.get('branch')?.errors);
    const produto: ProjectModuleModel = moduleControl.get('produto').value;
    this.service.getBranchesAndTagsCached(produto.id, produto.repository).then(data => {
      const dialogRef = this._dialog.open(GeracaoVersaoGitRefModalComponent, { data: data, disableClose: true, panelClass: 'geracao-versao-git-ref-modal-container' });
      dialogRef.afterClosed().subscribe((result: GitRefSelectionResult) => {
        if (result) {
          moduleControl.get('branch').setValue(result.name);
          moduleControl.get('commit').setValue(result.sha);
          this.propagateBondValues(produto, result);
        }
      });
    });
  }

  private propagateBondValues(parentModule: ProjectModuleModel, result: GitRefSelectionResult): void {
    for (const child of this.getModules().controls) {
      const childProduto = child.get('produto').value;
      if (childProduto.bond?.id === parentModule.id) {
        child.get('branch').setValue(result.name);
        child.get('commit').setValue(result.sha);
      }
    }
  }

  isModuleVisible(module: AbstractControl): boolean {
    const produto = module.get('produto').value;
    return produto.framework === false || produto.main === true;
  }

  private branchValidator(): ValidatorFn {

    return (control: AbstractControl): ValidationErrors | null => {

      if (!control.parent) {
        return null;
      }

      const compileType = control.parent.get('compileType')?.value;
      const branch = control.value;

      if (!compileType || !branch || compileType === VersionTypeEnum.patch) {
        return null;
      }

      const minBranch = this.nextBranchByCompileType[compileType]?.[0];

      if (!minBranch) {
        return null;
      }

      try {
        const current = new EvoluiVersionModel(branch);

        if (current.customCompare(minBranch) < 0) {
          return {
            branchTooLow: {
              min: minBranch.toString()
            }
          };
        }
      }
      catch (e) {
        return {
          invalidVersion: {
            error: e.toString()
          }
        };
      }
      return null;
    };

  }

}
