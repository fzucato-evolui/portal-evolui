import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewEncapsulation} from "@angular/core";
import {FormBuilder, FormGroup} from "@angular/forms";
import {cloneDeep} from "lodash-es";
import {
  AWSConfigModel,
  AWSInstanceRunnerTypeEnum,
  SystemConfigModel,
  SystemConfigModelEnum
} from "../../../../shared/models/system-config.model";
import {MessageDialogService} from "../../../../shared/services/message/message-dialog-service";
import {ConfigSystemComponent} from "../config-system.component";
import {RunnerGithubModel} from '../../../../shared/models/github.model';
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {MatSlideToggleChange} from '@angular/material/slide-toggle';


@Component({
  selector       : 'config-system-aws',
  templateUrl    : './config-system-aws.component.html',
  styleUrls      : ['./config-system-aws.component.scss'],
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class ConfigSystemAwsComponent implements OnInit{
  awsForm: FormGroup;
  _model: SystemConfigModel = new SystemConfigModel();
  AWSInstanceRunnerTypeEnum = AWSInstanceRunnerTypeEnum;
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



  awsModel: AWSConfigModel;
  editTabAccount = null;

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
    if (!this.awsForm) {
      this.awsForm = this._formBuilder.group({
        daysForKeep: [null],
        accountConfigs: this._formBuilder.group([])
      });


    }
    this.awsModel = new AWSConfigModel();
    if (this.model && this.model.id > 0 && this.model.configType === SystemConfigModelEnum.AWS) {
      this.awsModel = this.model.config ? this.model.config as AWSConfigModel : new AWSConfigModel();
      const keys = Object.keys(this.awsModel.accountConfigs);
      keys.forEach((keyName, index) => {
        (this.awsForm.get('accountConfigs') as FormGroup).addControl(
          keyName, this._formBuilder.group({
            enabled: [true],
            main: [false],
            region: ['', []],
            accessKey: ['', []],
            secretKey: ['', []],
            bucketVersions: ['', []],
            bucketTempDump: ['', []],
            bucketLocalMountPath: ['', []],
            runnerVersions: this._formBuilder.group({
              id: ['', []],
              instanceType: [null, []],
              runnerGithubId: ['', []]
            }),
            runnerTests: this._formBuilder.group({
              id: ['', []],
              instanceType: [null, []],
              runnerGithubId: ['', []]
            })

          }))
      });
    }
    this.awsForm.patchValue(this.awsModel);
  }

  salvar() {

    if (UtilFunctions.isValidStringOrArray(this.getKeys()) === true) {
      let mainCount = 0;
      for (let key of this.getKeys()) {
        if (this.getAccount(key).get('main').value === true) {
          mainCount++;
        }
      }
      if (mainCount === 0) {
        this._messageService.open('Uma conta deve ser definida como a conta principal', 'ERRO', 'warning');
        return;
      }
      if (mainCount > 1) {
        this._messageService.open('Apenas uma conta deve ser definida como a conta principal', 'ERRO', 'warning');
        return;
      }
    }

    this.model.configType = SystemConfigModelEnum.AWS;
    this.awsModel = this.awsForm.value;
    this.model.config = this.awsModel;
    console.log(this.model);
    this.parent.service.save(this.model)
      .then(value => {
        this.model = value;
        this._messageService.open('Configuração de Sistema para autenticação AWS salva com sucesso', 'SUCESSO', 'success');
      });
  }

  getKeys(): Array<string> {
    const keys = Object.keys((this.awsForm.get('accountConfigs') as FormGroup).controls);
    return keys.sort((x,y) => x.localeCompare(y));
  }

  getAccount(key: string) {
    return (this.awsForm.get('accountConfigs') as FormGroup).get(key);
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


  mainAccountCchanged(event: MatSlideToggleChange, key: any) {
    if (event.checked === true) {
      this.getKeys().forEach(x => {
        if (x !== key) {
          this.getAccount(x).get('main').setValue(false);
        }
        else {
          this.getAccount(x).get('enabled').setValue(true);
        }
      })
    }
  }

  removeAccount(account: string) {
    this._messageService.open('Deseja realmente remover a configuração dessa conta?', 'CONFIRMAÇÃO', 'confirm').subscribe((result) => {
      if (result === 'confirmed') {
        (this.awsForm.get('accountConfigs') as FormGroup).removeControl(account);
        this._changeDetectorRef.detectChanges();
      }
    });
  }

  changeAccountName(value: string, index: number, account: string) {
    const c = this.getAccount(account);
    (this.awsForm.get('accountConfigs') as FormGroup).removeControl(account);
    (this.awsForm.get('accountConfigs') as FormGroup).addControl(value, c);
    this.editTabAccount = null;
    this._changeDetectorRef.detectChanges();

  }

  addAccount() {
    const keyName = 'Account'+ this.getKeys().length;
    (this.awsForm.get('accountConfigs') as FormGroup).addControl(
      keyName, this._formBuilder.group({
        enabled: [true],
        main: [false],
        region: ['', []],
        accessKey: ['', []],
        secretKey: ['', []],
        bucketVersions: ['', []],
        runnerVersions: this._formBuilder.group({
          id: ['', []],
          instanceType: [null, []],
          runnerGithubId: ['', []]
        }),
        runnerTests: this._formBuilder.group({
          id: ['', []],
          instanceType: [null, []],
          runnerGithubId: ['', []]
        })

      }));

  }
}
