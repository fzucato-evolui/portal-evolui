import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewEncapsulation} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {cloneDeep} from "lodash-es";
import {AXConfigModel, SystemConfigModel, SystemConfigModelEnum} from "../../../../shared/models/system-config.model";
import {MessageDialogService} from "../../../../shared/services/message/message-dialog-service";
import {ConfigSystemComponent} from "../config-system.component";


@Component({
  selector       : 'config-system-ax',
  templateUrl    : './config-system-ax.component.html',
  styleUrls      : ['./config-system-ax.component.scss'],
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class ConfigSystemAxComponent implements OnInit{
  axForm: FormGroup;
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

  axModel: AXConfigModel;

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
    if (!this.axForm) {
      this.axForm = this._formBuilder.group({
        enabled: [false, []],
        server: ['', [Validators.required]],
        user: ['', [Validators.required]],
        token: ['', [Validators.required]],
      });
    }
    this.axModel = new AXConfigModel();
    if (this.model && this.model.id > 0 && this.model.configType === SystemConfigModelEnum.AX) {
      this.axModel = this.model.config ? this.model.config as AXConfigModel : new AXConfigModel();
    }
    this.axForm.patchValue(this.axModel);
  }

  salvar() {
    this.model.configType = SystemConfigModelEnum.AX;
    this.axModel = this.axForm.value;
    this.model.config = this.axModel;
    this.parent.service.save(this.model)
      .then(value => {
        this.model = value;
        this._messageService.open('Configuração de Sistema para autenticação AX salva com sucesso', 'SUCESSO', 'success');
      });
  }

}
