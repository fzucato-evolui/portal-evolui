import {ChangeDetectorRef, Component, OnDestroy, OnInit, ViewEncapsulation} from "@angular/core";
import {Subject} from 'rxjs';
import {AbstractControl, FormArray, FormBuilder, FormGroup, Validators} from '@angular/forms';
import {MatDialogRef} from '@angular/material/dialog';
import {ProjectModel, ProjectModuleModel} from '../../../../shared/models/project.model';
import {UtilFunctions} from '../../../../shared/util/util-functions';
import parser from 'cron-parser';
import {CicdService} from '../cicd.service';
import {MessageDialogService} from '../../../../shared/services/message/message-dialog-service';
import {VersaoModel, VersionTypeEnum} from '../../../../shared/models/version.model';
import {EvoluiVersionModel} from '../../../../shared/models/evolui-version.model';
import {cloneDeep} from 'lodash-es';

export function cronValidator(c: AbstractControl) {
  if (UtilFunctions.isValidStringOrArray(c.value) === true) {
    try {
      var interval = parser.parseExpression(c.value);

    } catch (err) {
      return {cronInvalid: {value: c.value}}
    }
  }

  return null;
}
@Component({
  selector       : 'cicd-modal',
  styleUrls      : ['/cicd-modal.component.scss'],
  templateUrl    : './cicd-modal.component.html',
  encapsulation  : ViewEncapsulation.None,

  standalone: false
})
export class CICDModalComponent implements OnInit, OnDestroy
{
  formSave: FormGroup;
  private _unsubscribeAll: Subject<any> = new Subject<any>();

  public customPatterns = { 'I': { pattern: new RegExp("[0-9|\*|/|L| |\-]+")} };

  title: string;
  private _target: ProjectModel = null;
  service: CicdService;

  nextBranchByCompileType: {[key: string]: Array<EvoluiVersionModel>} = {};
  moduleBranchesCache: {[key: string]: Array<string>} = {};
  moduleBranchesLoading: {[key: string]: boolean} = {};
  moduleBranchFilter: {[key: number]: string} = {};
  moduleLastValidBranch: {[key: number]: string} = {};

  set target(value: ProjectModel) {
    this._target = value;
    this.title = 'Geração de Integração CI/CD ' + this._target.title;
  }

  get target(): ProjectModel {
    return  this._target;
  }

  constructor(private _formBuilder: FormBuilder,
              public dialogRef: MatDialogRef<CICDModalComponent>,
              private _messageService: MessageDialogService,
              private _changeDetectorRef: ChangeDetectorRef)
  {

  }

  ngOnInit(): void {
    this.formSave = this._formBuilder.group({
      productId: [this.target.id, Validators.required],
      compileType: [null, Validators.required],
      branch: [null],
      cronExpression: [null],
      enabled: [true],
      modules: this._formBuilder.array([])
    });
    for (let m of this.target.modules) {
      if (m.framework === true) {
        continue;
      }
      const moduleGroup = this._formBuilder.group({
        productId: [m.id, Validators.required],
        productTitle: [m.title, Validators.required],
        produto: [m],
        branch: [null],
        enabled: [false],
        includeTests: [false],
        ignoreHashCommit: [false]
      });
      this.getProductModules().push(moduleGroup);
      this.moduleLastValidBranch[m.id] = null;
    }

    this.service.getVersions().then(versions => {
      this.computeNextBranches(versions);
      this._changeDetectorRef.markForCheck();
    });

    this.formSave.get('compileType').valueChanges.subscribe(val => {
      if (val === 'stable') {
        if (this.nextBranchByCompileType['stable']?.length > 0) {
          this.formSave.get('branch').setValue(this.nextBranchByCompileType['stable'][0].version);
        } else {
          this.formSave.get('branch').setValue('1.0.0');
        }
      } else if (val === 'patch') {
        this.formSave.get('branch').setValue(null);
      }
    });
  }

  ngOnDestroy(): void {
    this._unsubscribeAll.next(undefined);
    this._unsubscribeAll.complete();
  }

  private computeNextBranches(versions: Array<VersaoModel>) {
    this.nextBranchByCompileType = {};

    if (!UtilFunctions.isValidStringOrArray(versions)) {
      this.nextBranchByCompileType['stable'] = [new EvoluiVersionModel('1.0.0')];
      return;
    }

    const sorted = versions.sort((x, y) =>
      new EvoluiVersionModel(x.tag).customCompare(new EvoluiVersionModel(y.tag))
    );

    const groupedByBranch = sorted.reduce((acc, version) => {
      (acc[version.branch] ??= []).push(version);
      return acc;
    }, {} as Record<string, VersaoModel[]>);

    const pushNext = (type: string, version: EvoluiVersionModel) => {
      (this.nextBranchByCompileType[type] ??= []).push(version);
    };

    for (const [branch, branchVersions] of Object.entries(groupedByBranch)) {
      const stableBuilds = branchVersions.filter(v =>
        v.versionType === VersionTypeEnum.stable ||
        v.versionType === VersionTypeEnum.patch
      );

      if (stableBuilds.length) {
        const lastStable = stableBuilds[stableBuilds.length - 1];
        const next = new EvoluiVersionModel(lastStable.tag);
        next.incrementBuildNumber();
        pushNext('patch', next);
      }
    }

    const lastBranch = sorted[sorted.length - 1].branch;
    const lastBuild = new EvoluiVersionModel(lastBranch);
    const nextBranch = new EvoluiVersionModel(
      `${lastBuild.major}.${lastBuild.minor}.${lastBuild.patch + 1}`
    );
    this.nextBranchByCompileType['stable'] = [nextBranch];
  }

  private getModuleRepository(module: AbstractControl): string {
    const produto: ProjectModuleModel = module.get('produto')?.value;
    if (!produto) {
      return this.target?.repository || '';
    }
    return produto.repository || this.target?.repository || '';
  }

  ensureModuleBranchesLoaded(module: AbstractControl): void {
    const produto: ProjectModuleModel = module.get('produto')?.value;
    if (!produto) {
      return;
    }

    const repo = this.getModuleRepository(module);
    if (!UtilFunctions.isValidStringOrArray(repo)) {
      return;
    }
    if (this.moduleBranchesCache[repo] || this.moduleBranchesLoading[repo]) {
      return;
    }

    this.moduleBranchesLoading[repo] = true;
    this.service.getModuleBranches(produto.id)
    .then(branches => {
      this.moduleBranchesCache[repo] = branches || [];
    })
    .finally(() => {
      this.moduleBranchesLoading[repo] = false;
      this._changeDetectorRef.markForCheck();
    });
  }

  isModuleBranchesLoading(module: AbstractControl): boolean {
    const repo = this.getModuleRepository(module);
    return this.moduleBranchesLoading[repo] === true;
  }

  getProductModules(): FormArray {
    return this.formSave.get('modules') as FormArray;
  }

  getPatchBranches(): Array<EvoluiVersionModel> {
    return this.nextBranchByCompileType['patch'] || [];
  }

  getNextStableDisplay(): string {
    if (this.nextBranchByCompileType['stable']?.length > 0) {
      return this.nextBranchByCompileType['stable'][0].version;
    }
    return '(sem versões)';
  }

  getModuleBranchOptions(module: AbstractControl): Array<string> {
    const repo = this.getModuleRepository(module);
    const branches = this.moduleBranchesCache[repo] || [];
    const moduleId = module.get('productId').value;
    const filter = this.moduleBranchFilter[moduleId];

    if (UtilFunctions.isValidStringOrArray(filter)) {
      return branches.filter(b => b.toLowerCase().includes(filter.toLowerCase()));
    }
    return branches;
  }

  onModuleBranchInput(module: AbstractControl, value: string) {
    const moduleId = module.get('productId').value;
    this.moduleBranchFilter[moduleId] = value;
    module.get('branch').setValue(value, {emitEvent: false});
  }

  onModuleBranchSelected(module: AbstractControl, branch: string) {
    const moduleId = module.get('productId').value;
    this.moduleBranchFilter[moduleId] = branch;
    this.moduleLastValidBranch[moduleId] = branch;
    module.get('branch').setValue(branch);
    this.onModuleBranchChanged(module);
  }

  validateModuleBranch(module: AbstractControl) {
    const moduleId = module.get('productId').value;
    const value = module.get('branch').value;
    const options = this.getModuleBranchOptions(module);

    if (!UtilFunctions.isValidStringOrArray(value)) {
      module.get('branch').setValue(null, {emitEvent: false});
      this.moduleBranchFilter[moduleId] = null;
      return;
    }

    if (options.includes(value)) {
      this.moduleLastValidBranch[moduleId] = value;
      this.moduleBranchFilter[moduleId] = value;
      return;
    }

    const fallback = this.moduleLastValidBranch[moduleId];
    module.get('branch').setValue(fallback || null, {emitEvent: false});
    this.moduleBranchFilter[moduleId] = fallback || null;
    this._changeDetectorRef.markForCheck();
  }

  getModuleBranchNoDataText(module: AbstractControl): string {
    if (this.isModuleBranchesLoading(module)) {
      return 'Carregando branches...';
    }
    return 'Nenhuma branch encontrada';
  }

  isModuleBondChild(module: AbstractControl): boolean {
    const produto: ProjectModuleModel = module.get('produto')?.value;
    return produto?.bond != null;
  }

  onModuleBranchChanged(module: AbstractControl) {
    const produto: ProjectModuleModel = module.get('produto')?.value;
    if (!produto) return;
    const branch = module.get('branch').value;
    for (const child of this.getProductModules().controls) {
      const childProduto: ProjectModuleModel = child.get('produto')?.value;
      if (childProduto?.bond?.id === produto.id) {
        child.get('branch').setValue(branch);
        const childId = child.get('productId').value;
        this.moduleBranchFilter[childId] = branch;
        this.moduleLastValidBranch[childId] = branch;
      }
    }
  }

  onModuleEnabledChanged(module: AbstractControl) {
    const produto: ProjectModuleModel = module.get('produto')?.value;
    const enabled = module.get('enabled').value;
    if (!produto) return;

    if (enabled && produto.bond) {
      const parentCtrl = this.getProductModules().controls.find(
        m => m.get('produto')?.value?.id === produto.bond.id
      );
      if (parentCtrl && !parentCtrl.get('enabled').value) {
        parentCtrl.get('enabled').setValue(true);
      }
    }
    if (!enabled) {
      for (const child of this.getProductModules().controls) {
        const childProduto: ProjectModuleModel = child.get('produto')?.value;
        if (childProduto?.bond?.id === produto.id && child.get('enabled').value) {
          child.get('enabled').setValue(false);
        }
      }
    }
  }

  canSave(): boolean {
    if (!this.formSave || this.formSave.invalid) return false;
    if (!this.formSave.get('compileType').value) return false;
    if (this.formSave.get('compileType').value === 'patch' && !this.formSave.get('branch').value) return false;
    const enabledModules = this.getProductModules().controls.filter(m => m.get('enabled').value);
    if (enabledModules.length === 0) return false;
    for (const m of enabledModules) {
      if (!m.get('branch').value) return false;
    }
    return true;
  }

  doSaving() {
    const formValue = cloneDeep(this.formSave.value);
    if (formValue.compileType === 'stable') {
      formValue.branch = 'master';
    }
    if (formValue.modules) {
      for (const m of formValue.modules) {
        delete m.productTitle;
        delete m.produto;
      }
    }
    this.service.save(formValue).then(value => {
      this._messageService.open("Nova integração foi iniciada com sucesso!", "SUCESSO", "success")
      this.dialogRef.close();
    });
  }
}
