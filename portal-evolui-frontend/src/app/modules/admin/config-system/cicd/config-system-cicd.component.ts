import {ChangeDetectorRef, Component, Input, OnInit, ViewEncapsulation} from "@angular/core";
import {AbstractControl, FormArray, FormBuilder, FormGroup, Validators} from "@angular/forms";
import {cloneDeep} from "lodash-es";
import {CICDConfigModel, SystemConfigModel, SystemConfigModelEnum} from "../../../../shared/models/system-config.model";
import {MessageDialogService} from "../../../../shared/services/message/message-dialog-service";
import {ConfigSystemComponent} from "../config-system.component";
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {ProjectModel} from '../../../../shared/models/project.model';
import parser from 'cron-parser';
import {MatSelectChange} from "@angular/material/select";

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


  constructor(
    private _formBuilder: FormBuilder,
    private _messageService: MessageDialogService,
    private _changeDetectorRef: ChangeDetectorRef,
    public parent: ConfigSystemComponent
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
            for (const y of x.modules) {
              const e = new MatSelectChange(null, x.productId);
              this.productChanged(g, e);
            }
          }
        }

      }


    }
    this.cicdForm.patchValue(this.cicdModel);
  }

  salvar() {
    this.model.configType = SystemConfigModelEnum.CICD;
    this.cicdModel = this.cicdForm.value;
    this.model.config = this.cicdModel;
    console.log(this.model);
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
      branch: [null, Validators.required],
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

  addProduct2(product: ProjectModel) {
    const g = this._formBuilder.group({
      productId: [product.id, Validators.required],
      branch: [null, Validators.required],
      enabled: [false],
      modules: this._formBuilder.array([

        this._formBuilder.group({
          productId: [product.id, Validators.required],
          productTitle: [product.title, Validators.required],
          enabled: [false],
          includeTests: [false],
          ignoreHashCommit: [false]
        })

      ])
    });
    for (const m of product.modules) {
      const moduleGroup = this._formBuilder.group({
        productId: [m.id, Validators.required],
        productTitle: [m.title, Validators.required],
        enabled: [false],
        includeTests: [false],
        ignoreHashCommit: [false]
      });
      (g.get('modules') as FormArray).push(moduleGroup);
    }
    const products = this.cicdForm.get('products') as FormGroup;
    if (Object.keys(product).findIndex( x => x === product.id.toString()) < 0) {
      products.addControl(product.id.toString(), this._formBuilder.array([g]));
    }
    else {
      (products.get(product.id.toString()) as FormArray).push(g);
    }

      //g.patchValue(model);


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
          enabled: [false],
          includeTests: [false],
          ignoreHashCommit: [false]
        }));

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
