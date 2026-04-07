import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewEncapsulation} from "@angular/core";
import {FormBuilder, FormGroup} from "@angular/forms";
import {cloneDeep} from "lodash-es";
import {
  GithubConfigModel,
  SystemConfigModel,
  SystemConfigModelEnum
} from "../../../../shared/models/system-config.model";
import {MessageDialogService} from "../../../../shared/services/message/message-dialog-service";
import {ConfigSystemComponent} from "../config-system.component";


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
    });
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
