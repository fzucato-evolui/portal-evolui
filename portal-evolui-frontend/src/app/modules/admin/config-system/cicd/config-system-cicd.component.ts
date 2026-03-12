import {ChangeDetectorRef, Component, Input, OnInit, ViewEncapsulation} from "@angular/core";
import {AbstractControl, FormArray, FormBuilder, FormGroup, Validators} from "@angular/forms";
import {cloneDeep} from "lodash-es";
import {CICDConfigModel, SystemConfigModel, SystemConfigModelEnum} from "../../../../shared/models/system-config.model";
import {MessageDialogService} from "../../../../shared/services/message/message-dialog-service";
import {ConfigSystemComponent} from "../config-system.component";
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {ProjectModel, ProjectModuleModel} from '../../../../shared/models/project.model';
import parser from 'cron-parser';
import {MatSelectChange} from "@angular/material/select";
import {HttpClient} from "@angular/common/http";
import {VersaoModel, VersionTypeEnum} from "../../../../shared/models/version.model";
import {EvoluiVersionModel} from "../../../../shared/models/evolui-version.model";

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
  selector       : 'config-system-cicd',
  templateUrl    : './config-system-cicd.component.html',
  styleUrls      : ['./config-system-cicd.component.scss'],
  encapsulation  : ViewEncapsulation.None,

  standalone: false
})
export class ConfigSystemCicdComponent implements OnInit{
  cicdForm: FormGroup;

  _model: SystemConfigModel = new SystemConfigModel();

  public customPatterns = { 'I': { pattern: new RegExp("[0-9|\*|/|L| |\-]+")} };
  @Input()
  set model(value: SystemConfigModel) {
    if (value && value.id !== this._model.id) {
      this._model = cloneDeep(value);
      this.init();
    }
  };

  get model(): SystemConfigModel {
    return this._model;
  }
  cicdModel: CICDConfigModel;

  public get produtos(): Array<ProjectModel> {
    return this.parent.produtos;
  }

  public get branches(): {[key: string]: Array<string>} {
    return this.parent.initialData.productBranches;
  }

  productVersions: {[key: string]: Array<VersaoModel>} = {};
  nextBranchByProduct: {[key: string]: {[key: string]: Array<EvoluiVersionModel>}} = {};
  moduleBranchesCache: {[key: string]: Array<string>} = {};
  moduleBranchesLoading: {[key: string]: boolean} = {};
  moduleBranchFilter: {[key: number]: string} = {};
  moduleLastValidBranch: {[key: number]: string} = {};

  constructor(
    private _formBuilder: FormBuilder,
    private _messageService: MessageDialogService,
    private _changeDetectorRef: ChangeDetectorRef,
    public parent: ConfigSystemComponent,
    private _httpClient: HttpClient
  )
  {
  }

  ngOnInit(): void {
    this.init();
  }

  init() {
    if (!this.cicdForm) {
      this.cicdForm = this._formBuilder.group({
        enabled: [false],
        daysForKeep: [null],
        products: this._formBuilder.array([])
      });

    }
    this.cicdModel = new CICDConfigModel();
    if (this.model && this.model.id > 0 && this.model.configType === SystemConfigModelEnum.CICD) {
      this.cicdModel = this.model.config ? this.model.config as CICDConfigModel : new CICDConfigModel();
      if (this.cicdModel.products && UtilFunctions.isValidStringOrArray(this.cicdModel.products) === true) {
        this.getProducts().clear();
        for (const x of this.cicdModel.products) {
          const g = this.addProduct();
          if (x.modules && UtilFunctions.isValidStringOrArray(x.modules) === true) {
            const e = new MatSelectChange(null, x.productId);
            this.productChanged(g, e);
          }
        }

      }


    }
    this.cicdForm.patchValue(this.cicdModel);
  }

  salvar() {
    const formValue = cloneDeep(this.cicdForm.value);
    if (formValue.products) {
      for (const p of formValue.products) {
        if (p.compileType === 'stable') {
          p.branch = 'master';
        }
        if (p.modules) {
          for (const m of p.modules) {
            delete m.productTitle;
            delete m.produto;
          }
        }
      }
    }
    this.model.configType = SystemConfigModelEnum.CICD;
    this.model.config = formValue;
    this.parent.service.save(this.model)
    .then(value => {
      this.model = value;
      this._messageService.open('Configuração de CI/CD salva com sucesso', 'SUCESSO', 'success');
    });
  }

  getNextSchedulers(val: string): string[] {
    try {
      const options = {
        currentDate: new Date(),
        iterator: true,

      };

      var interval = parser.parseExpression(val, options);

      let count = 0;
      let nextValues = [];
      while (count < 5) {
        try {
          var obj = interval.next();
          // @ts-ignore
          nextValues.push(obj.value.toString())
          count++;
        } catch (e) {
          break;
        }
      }

      return nextValues;
    } catch (err) {
      console.log('Error: ' + err.message);
    }
  }

  addProduct(): FormGroup {
    const g = this._formBuilder.group({
      productId: [null, Validators.required],
      compileType: [null, Validators.required],
      branch: [null],
      cronExpression: [, [Validators.required, cronValidator]],
      enabled: [false],
      modules: this._formBuilder.array([])
    });
    const products = this.getProducts();
    products.push(g);
    return g;
  }

  getProducts(): FormArray {
    return this.cicdForm.get('products') as FormArray;
  }

  removeProduct(i: number) {
    this.getProducts().removeAt(i);
  }

  getProductBranches(value: string): Array<string> {
    if(UtilFunctions.isValidStringOrArray(value) === false) {
      return null;
    }
    return this.branches[value];
  }

  getProductModules(c: FormGroup): FormArray {

    return c.get('modules') as FormArray;
  }

  productChanged(c: FormGroup, event: MatSelectChange) {
    c.get('branch').setValue(null);
    c.get('compileType').setValue(null);
    this.getProductModules(c).clear();
    if (UtilFunctions.isValidStringOrArray(event.value) === true) {
      const p = this.produtos[this.produtos.findIndex(x => x.id === event.value)];

      for (const m of p.modules) {
        if (m.framework === true) {
          continue;
        }
        this.getProductModules(c).push(this._formBuilder.group({
          productId: [m.id, Validators.required],
          productTitle: [m.title, Validators.required],
          produto: [m],
          branch: [null],
          enabled: [false],
          includeTests: [false],
          ignoreHashCommit: [false]
        }));
        this.moduleLastValidBranch[m.id] = null;
      }

      this.loadProductVersions(event.value);
    }
  }

  compileTypeChanged(c: FormGroup) {
    const compileType = c.get('compileType').value;
    const productId = c.get('productId').value;

    if (compileType === 'stable') {
      const nextVersions = this.nextBranchByProduct[productId];
      if (nextVersions && nextVersions['stable'] && nextVersions['stable'].length > 0) {
        c.get('branch').setValue(nextVersions['stable'][0].version);
      } else {
        c.get('branch').setValue('1.0.0');
      }
    } else if (compileType === 'patch') {
      c.get('branch').setValue(null);
    }
  }

  getPatchBranches(productId: number): Array<EvoluiVersionModel> {
    const nextVersions = this.nextBranchByProduct[productId];
    if (nextVersions && nextVersions['patch']) {
      return nextVersions['patch'];
    }
    return [];
  }

  getNextStableDisplay(productId: number): string {
    const nextVersions = this.nextBranchByProduct[productId];
    if (nextVersions && nextVersions['stable'] && nextVersions['stable'].length > 0) {
      return nextVersions['stable'][0].version;
    }
    return '(sem versões)';
  }

  private loadProductVersions(productId: number) {
    const p = this.produtos.find(x => x.id === productId);
    if (!p) return;
    this._httpClient.get<Array<VersaoModel>>(`api/admin/cicd/${p.identifier}/versions`).toPromise().then(versions => {
      this.productVersions[productId] = versions;
      this.computeNextBranches(productId, versions);
      this._changeDetectorRef.markForCheck();
    });
  }

  private computeNextBranches(productId: number, versions: Array<VersaoModel>) {
    this.nextBranchByProduct[productId] = {};

    if (!UtilFunctions.isValidStringOrArray(versions)) {
      this.nextBranchByProduct[productId]['stable'] = [new EvoluiVersionModel('1.0.0')];
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
      (this.nextBranchByProduct[productId][type] ??= []).push(version);
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
    this.nextBranchByProduct[productId]['stable'] = [nextBranch];
  }

  private getModuleRepository(module: AbstractControl, product: ProjectModel): string {
    const produto: ProjectModuleModel = module.get('produto')?.value;
    return produto?.repository || product?.repository || '';
  }

  ensureModuleBranchesLoaded(module: AbstractControl, product: ProjectModel): void {
    const produto: ProjectModuleModel = module.get('produto')?.value;
    if (!produto) {
      return;
    }

    const repo = this.getModuleRepository(module, product);
    if (!UtilFunctions.isValidStringOrArray(repo)) {
      return;
    }
    if (this.moduleBranchesCache[repo] || this.moduleBranchesLoading[repo]) {
      return;
    }

    this.moduleBranchesLoading[repo] = true;
    this._httpClient.get<Array<string>>(`api/admin/cicd/module-branches/${produto.id}`).toPromise()
    .then(branches => {
      this.moduleBranchesCache[repo] = branches || [];
    })
    .finally(() => {
      this.moduleBranchesLoading[repo] = false;
      this._changeDetectorRef.markForCheck();
    });
  }

  isModuleBranchesLoading(module: AbstractControl, product: ProjectModel): boolean {
    const repo = this.getModuleRepository(module, product);
    return this.moduleBranchesLoading[repo] === true;
  }

  getModuleBranchOptions(module: AbstractControl, product: ProjectModel): Array<string> {
    const repo = this.getModuleRepository(module, product);
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

  onModuleBranchSelected(module: AbstractControl, branch: string, c: FormGroup) {
    const moduleId = module.get('productId').value;
    this.moduleBranchFilter[moduleId] = branch;
    this.moduleLastValidBranch[moduleId] = branch;
    module.get('branch').setValue(branch);
    this.onModuleBranchChanged(module, c);
  }

  validateModuleBranch(module: AbstractControl, product: ProjectModel) {
    const moduleId = module.get('productId').value;
    const value = module.get('branch').value;
    const options = this.getModuleBranchOptions(module, product);

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

  getModuleBranchNoDataText(module: AbstractControl, product: ProjectModel): string {
    if (this.isModuleBranchesLoading(module, product)) {
      return 'Carregando branches...';
    }
    return 'Nenhuma branch encontrada';
  }

  isModuleBondChild(module: AbstractControl): boolean {
    const produto: ProjectModuleModel = module.get('produto')?.value;
    return produto?.bond != null;
  }

  onModuleBranchChanged(module: AbstractControl, c: FormGroup) {
    const produto: ProjectModuleModel = module.get('produto')?.value;
    if (!produto) return;
    const branch = module.get('branch').value;
    for (const child of this.getProductModules(c).controls) {
      const childProduto: ProjectModuleModel = child.get('produto')?.value;
      if (childProduto?.bond?.id === produto.id) {
        child.get('branch').setValue(branch);
        const childId = child.get('productId').value;
        this.moduleBranchFilter[childId] = branch;
        this.moduleLastValidBranch[childId] = branch;
      }
    }
  }

  onModuleEnabledChanged(module: AbstractControl, c: FormGroup) {
    const produto: ProjectModuleModel = module.get('produto')?.value;
    const enabled = module.get('enabled').value;
    if (!produto) return;

    if (enabled && produto.bond) {
      const parentCtrl = this.getProductModules(c).controls.find(
        m => m.get('produto')?.value?.id === produto.bond.id
      );
      if (parentCtrl && !parentCtrl.get('enabled').value) {
        parentCtrl.get('enabled').setValue(true);
      }
    }
    if (!enabled) {
      for (const child of this.getProductModules(c).controls) {
        const childProduto: ProjectModuleModel = child.get('produto')?.value;
        if (childProduto?.bond?.id === produto.id && child.get('enabled').value) {
          child.get('enabled').setValue(false);
        }
      }
    }
  }

  upProduct(c: FormGroup, i: number) {
    let numberOfDeletedElm = 1;

    const elm = this.getProducts().controls.splice(i, numberOfDeletedElm)[0];

    numberOfDeletedElm = 0;

    this.getProducts().controls.splice(i-1, numberOfDeletedElm, elm);
  }

  downProduct(c: FormGroup, i: number) {
    let numberOfDeletedElm = 1;

    const elm = this.getProducts().controls.splice(i, numberOfDeletedElm)[0];

    numberOfDeletedElm = 0;

    this.getProducts().controls.splice(i+1, numberOfDeletedElm, elm);
  }
}
