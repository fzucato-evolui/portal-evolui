import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewEncapsulation} from "@angular/core";
import {FormBuilder, FormGroup} from "@angular/forms";
import {UtilFunctions} from "app/shared/util/util-functions";
import {cloneDeep} from "lodash-es";
import {
  GoogleConfigModel,
  GoogleServiceAccountModel,
  SystemConfigModel,
  SystemConfigModelEnum
} from "../../../../shared/models/system-config.model";
import {MessageDialogService} from "../../../../shared/services/message/message-dialog-service";
import {ConfigSystemComponent} from "../config-system.component";


@Component({
  selector       : 'config-system-google',
  templateUrl    : './config-system-google.component.html',
  styleUrls      : ['./config-system-google.component.scss'],
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class ConfigSystemGoogleComponent implements OnInit{
  googleForm: FormGroup;
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

  googleModel: GoogleConfigModel;

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
    if (!this.googleForm) {
      this.googleForm = this._formBuilder.group({
        apiKey: ['', []],
        clientID: ['', []],
        secretKey: ['', []],
      });
    }
    this.googleModel = new GoogleConfigModel();
    if (this.model && this.model.id > 0 && this.model.configType === SystemConfigModelEnum.GOOGLE) {
      this.googleModel = this.model.config ? this.model.config as GoogleConfigModel : new GoogleConfigModel();
    }
    this.googleForm.patchValue({
      apiKey: this.googleModel.apiKey,
      clientID: this.googleModel.clientID,
      secretKey: this.googleModel.secretKey,
    });
  }

  salvar() {
    this.model.configType = SystemConfigModelEnum.GOOGLE;
    this.googleModel = {...this.googleForm.value, ... this.googleModel};
    this.model.config = this.googleModel;
    this.parent.service.save(this.model)
      .then(value => {
        this.model = value;
        this._messageService.open('Configuração de Sistema para autenticação Google salva com sucesso', 'SUCESSO', 'success');
      });
  }

  public dropped(files: File[]) {
    this.googleModel.serviceAccount = null;
    const me = this;
    files.forEach(file => {
      const ext = UtilFunctions.getFileExtension(file.name);
      if (ext !== '.json') {
        this._messageService.open('Extensão não permitida', 'ERRO', 'error');
        return;
      }

      file.arrayBuffer().then(buffer => {
        const googleServiceAccount: GoogleServiceAccountModel = JSON.parse(UtilFunctions.arrayBufferToString(buffer));
        if (!GoogleServiceAccountModel.validate(googleServiceAccount)) {
          throw Error("Arquivo inválido");
        }
        me.googleModel.serviceAccount = googleServiceAccount;
        me._changeDetectorRef.markForCheck();

        setTimeout(function () {
          me._changeDetectorRef.markForCheck();
        }, 200);

      }).catch(reason => {
        me._messageService.open(reason, 'ERRO', 'error');
      });
    });
  }

  isValidConfigJson () {
    if (this.googleModel && this.googleModel.serviceAccount) {
      GoogleServiceAccountModel.validate(this.googleModel.serviceAccount);
    }
    return true;
  }

}
