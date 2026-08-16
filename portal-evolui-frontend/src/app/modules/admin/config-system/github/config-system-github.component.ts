import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewEncapsulation} from "@angular/core";
import {AbstractControl, FormBuilder, FormGroup} from "@angular/forms";
import {cloneDeep} from "lodash-es";
import {
  GithubConfigModel,
  SystemConfigModel,
  SystemConfigModelEnum
} from "../../../../shared/models/system-config.model";
import {MessageDialogService} from "../../../../shared/services/message/message-dialog-service";
import {ConfigSystemComponent} from "../config-system.component";
import {UtilFunctions} from '../../../../shared/util/util-functions';
import parser from 'cron-parser';

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
  selector       : 'config-system-github',
  templateUrl    : './config-system-github.component.html',
  styleUrls      : ['./config-system-github.component.scss'],
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class ConfigSystemGithubComponent implements OnInit{
  githubForm: FormGroup;
  public customPatterns = { 'I': { pattern: new RegExp("[0-9|\\*|/|L| |\\-|,]+")} };
  _model: SystemConfigModel = new SystemConfigModel();
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

  githubModel: GithubConfigModel;

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
    if (!this.githubForm) {
      this.githubForm = this._formBuilder.group({
        user: ['', []],
        token: ['', []],
        owner: ['', []],
        daysForKeep: [null, []],
        runnerInstallerDownloadUrl: ['', []],
        runnerInstallerMinVersion: ['', []],
        runnerCheckEnabled: [false],
        runnerCheckCronExpression: [null, [cronValidator]],
      });
    }
    this.githubModel = new GithubConfigModel();
    if (this.model && this.model.id > 0 && this.model.configType === SystemConfigModelEnum.GITHUB) {
      this.githubModel = this.model.config ? this.model.config as GithubConfigModel : new GithubConfigModel();
    }
    this.githubForm.patchValue({
      user: this.githubModel.user,
      token: this.githubModel.token,
      owner: this.githubModel.owner,
      daysForKeep: this.githubModel.daysForKeep,
      runnerInstallerDownloadUrl: this.githubModel.runnerInstallerDownloadUrl,
      runnerInstallerMinVersion: this.githubModel.runnerInstallerMinVersion,
      runnerCheckEnabled: this.githubModel.runnerCheckEnabled || false,
      runnerCheckCronExpression: this.githubModel.runnerCheckCronExpression,
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

  salvar() {
    this.model.configType = SystemConfigModelEnum.GITHUB;
    this.githubModel = this.githubForm.value;
    this.model.config = this.githubModel;
    this.parent.service.save(this.model)
      .then(value => {
        this.model = value;
        this._messageService.open('Configuração de Sistema para autenticação Github salva com sucesso', 'SUCESSO', 'success');
      });
  }

}
