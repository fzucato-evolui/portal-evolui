import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewEncapsulation} from "@angular/core";
import {FormBuilder, FormControl, FormGroup, Validators} from "@angular/forms";
import {cloneDeep} from "lodash-es";
import {SMTPConfigModel, SystemConfigModel, SystemConfigModelEnum} from "../../../../shared/models/system-config.model";
import {MessageDialogService} from "../../../../shared/services/message/message-dialog-service";
import {ConfigSystemComponent} from "../config-system.component";
import {UtilFunctions} from '../../../../shared/util/util-functions';


@Component({
  selector       : 'config-system-smtp',
  templateUrl    : './config-system-smtp.component.html',
  styleUrls      : ['./config-system-smtp.component.scss'],
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class ConfigSystemSmtpComponent implements OnInit{
  smtpForm: FormGroup;
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



  smtpModel: SMTPConfigModel;

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
    if (!this.smtpForm) {
      this.smtpForm = this._formBuilder.group({
        server: ['', [Validators.required]],
        port: [-1, Validators.required],
        ssl: [true, []],
        user: ['', Validators.required],
        password: ['', Validators.required],
        senderName: ['', Validators.required],
        senderEmail: ['', [Validators.required, Validators.email]],
      });
    }
    this.smtpModel = new SMTPConfigModel();
    if (this.model && this.model.id > 0 && this.model.configType === SystemConfigModelEnum.SMTP) {
      this.smtpModel = this.model.config ? this.model.config as SMTPConfigModel : new SMTPConfigModel();
    }
    this.smtpForm.patchValue(this.smtpModel);
  }

  serverValidator(c: FormControl) {
    const host = c.value as string;

    if (UtilFunctions.isValidStringOrArray(host) === true) {

      if (UtilFunctions.isValidURL(host) === false) {
        return {'invalid': true};
      }
    }

    return null;
  }

  salvar() {
    this.model.configType = SystemConfigModelEnum.SMTP;
    this.smtpModel = this.smtpForm.value;
    this.model.config = this.smtpModel;
    console.log(this.model);
    this.parent.service.save(this.model)
      .then(value => {
        this.model = value;
        this._messageService.open('Configuração de Sistema para autenticação SMTP salva com sucesso', 'SUCESSO', 'success');
      });
  }

  getAccount(key: string) {
    return (this.smtpForm.get('accountConfigs') as FormGroup).get(key);
  }


}
