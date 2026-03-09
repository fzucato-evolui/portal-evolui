import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewEncapsulation} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {cloneDeep} from "lodash-es";
import {
  PortalLuthierConfigModel,
  SystemConfigModel,
  SystemConfigModelEnum
} from "../../../../shared/models/system-config.model";
import {MessageDialogService} from "../../../../shared/services/message/message-dialog-service";
import {ConfigSystemComponent} from "../config-system.component";


@Component({
  selector       : 'config-system-portal-luthier',
  templateUrl    : './config-system-portal-luthier.component.html',
  styleUrls      : ['./config-system-portal-luthier.component.scss'],
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class ConfigSystemPortalLuthierComponent implements OnInit{
  portalLuthierForm: FormGroup;
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

  portalLuthierModel: PortalLuthierConfigModel;

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
    if (!this.portalLuthierForm) {
      this.portalLuthierForm = this._formBuilder.group({
        enabled: [false, []],
        server: ['', [Validators.required]],
        user: ['', [Validators.required]],
        password: ['', [Validators.required]],
        luthierUser: ['', [Validators.required]],
        luthierPassword: ['', [Validators.required]],
      });
    }
    this.portalLuthierModel = new PortalLuthierConfigModel();
    if (this.model && this.model.id > 0 && this.model.configType === SystemConfigModelEnum.PORTAL_LUTHIER) {
      this.portalLuthierModel = this.model.config ? this.model.config as PortalLuthierConfigModel : new PortalLuthierConfigModel();
    }
    this.portalLuthierForm.patchValue(this.portalLuthierModel);
  }

  salvar() {
    this.model.configType = SystemConfigModelEnum.PORTAL_LUTHIER;
    this.portalLuthierModel = this.portalLuthierForm.value;
    this.model.config = this.portalLuthierModel;
    this.parent.service.save(this.model)
      .then(value => {
        this.model = value;
        this._messageService.open('Configuração de Sistema para autenticação Portal Luthier salva com sucesso', 'SUCESSO', 'success');
      });
  }

}
