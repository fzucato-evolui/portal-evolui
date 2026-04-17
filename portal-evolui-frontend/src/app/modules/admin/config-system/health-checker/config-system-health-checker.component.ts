import {ChangeDetectionStrategy, Component, Input, OnInit, ViewEncapsulation} from '@angular/core';
import {FormBuilder, FormGroup} from '@angular/forms';
import {cloneDeep} from 'lodash-es';
import {
  HealthCheckerConfigModel,
  SystemConfigModel,
  SystemConfigModelEnum
} from '../../../../shared/models/system-config.model';
import {MessageDialogService} from '../../../../shared/services/message/message-dialog-service';
import {ConfigSystemComponent} from '../config-system.component';

@Component({
  selector       : 'config-system-health-checker',
  templateUrl    : './config-system-health-checker.component.html',
  styleUrls      : ['./config-system-health-checker.component.scss'],
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class ConfigSystemHealthCheckerComponent implements OnInit {
  healthCheckerForm: FormGroup;
  _model: SystemConfigModel = new SystemConfigModel();
  @Input()
  set model(value: SystemConfigModel) {
    if (value && (value.id !== this._model.id || value.configType !== this._model.configType)) {
      this._model = cloneDeep(value);
      this.init();
    }
  }

  get model(): SystemConfigModel {
    return this._model;
  }

  healthCheckerModel: HealthCheckerConfigModel;

  constructor(
    private _formBuilder: FormBuilder,
    private _messageService: MessageDialogService,
    public parent: ConfigSystemComponent
  ) {
  }

  ngOnInit(): void {
    this.init();
  }

  init(): void {
    if (!this.healthCheckerForm) {
      this.healthCheckerForm = this._formBuilder.group({
        monitorDownloadUrl: ['', []],
        monitorMinVersion: ['', []],
      });
    }
    this.healthCheckerModel = new HealthCheckerConfigModel();
    if (this.model && this.model.configType === SystemConfigModelEnum.HEALTH_CHECKER) {
      if (this.model.id > 0 && this.model.config) {
        this.healthCheckerModel = this.model.config as HealthCheckerConfigModel;
      } else if (this.model.config) {
        this.healthCheckerModel = this.model.config as HealthCheckerConfigModel;
      }
    }
    this.healthCheckerForm.patchValue(this.healthCheckerModel);
  }

  salvar(): void {
    this.model.configType = SystemConfigModelEnum.HEALTH_CHECKER;
    this.healthCheckerModel = this.healthCheckerForm.value;
    this.model.config = this.healthCheckerModel;
    this.parent.service.save(this.model)
      .then(value => {
        this.model = value;
        this._messageService.open('Configuração do Health Checker (Evolui Monitor) salva com sucesso', 'SUCESSO', 'success');
      });
  }
}
