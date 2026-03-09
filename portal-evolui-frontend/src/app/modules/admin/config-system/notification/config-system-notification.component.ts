import {ChangeDetectorRef, Component, Input, OnInit, ViewEncapsulation} from "@angular/core";
import {FormArray, FormBuilder, FormGroup, Validators} from "@angular/forms";
import {cloneDeep} from "lodash-es";
import {
  GoogleSpaceModel,
  NotificationBasicConfigModel,
  NotificationConfigModel,
  NotificationConfigTypeEnum,
  NotificationTriggerConfigModel,
  NotificationTriggerEnum,
  SystemConfigModel,
  SystemConfigModelEnum
} from "../../../../shared/models/system-config.model";
import {MessageDialogService} from "../../../../shared/services/message/message-dialog-service";
import {ConfigSystemComponent} from "../config-system.component";
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {ProjectModel} from '../../../../shared/models/project.model';
import {AmbienteModel} from '../../../../shared/models/ambiente.model';
import {HealthCheckerModel} from '../../../../shared/models/health-checker.model';
import {RDSModel} from '../../../../shared/models/rds.model';


@Component({
  selector       : 'config-system-notification',
  templateUrl    : './config-system-notification.component.html',
  styleUrls      : ['./config-system-notification.component.scss'],
  encapsulation  : ViewEncapsulation.None,

  standalone: false
})
export class ConfigSystemNotificationComponent implements OnInit{
  notificationForm: FormGroup;

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
  notificationModel: NotificationConfigModel;
  NotificationConfigTypeEnum = NotificationConfigTypeEnum;
  NotificationTriggerEnum = NotificationTriggerEnum;

  public get produtos(): Array<ProjectModel> {
    return this.parent.produtos;
  }

  public get ambientes(): Array<AmbienteModel> {
    return this.parent.initialData.environments;
  }

  public get healthCheckers(): Array<HealthCheckerModel> {
    return this.parent.initialData.healthcheckers;
  }

  public get spaces(): Array<GoogleSpaceModel> {
    return this.parent.initialData.spaces;
  }

  public get databases(): Array<RDSModel> {
    return this.parent.initialData.rds;
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
    if (!this.notificationForm) {
      this.notificationForm = this._formBuilder.group({
        configs: this._formBuilder.array([])
      });

    }
    this.notificationModel = new NotificationConfigModel();
    if (this.model && this.model.id > 0 && this.model.configType === SystemConfigModelEnum.NOTIFICATION) {
      this.notificationModel = this.model.config ? this.model.config as NotificationConfigModel : new NotificationConfigModel();
      if (this.notificationModel.configs && UtilFunctions.isValidStringOrArray(this.notificationModel.configs) === true) {
        this.getConfigs().clear();
        for (const x of this.notificationModel.configs) {
          this.addConfig(x);
        }

      }
    }
    this.notificationForm.patchValue(this.notificationModel);
    // Ensure multiple mat-select controls remain arrays after patchValue
    this.getConfigs().controls.forEach(c => {
      if (!Array.isArray(c.get('references').value)) {
        c.get('references').setValue([]);
      }
    });
  }

  salvar() {
    this.model.configType = SystemConfigModelEnum.NOTIFICATION;
    this.notificationModel = this.notificationForm.value;
    this.model.config = this.notificationModel;
    console.log(this.model);
    this.parent.service.save(this.model)
      .then(value => {
        this.model = value;
        this._messageService.open('Configuração de Sistema para autenticação Notification salva com sucesso', 'SUCESSO', 'success');
      });
  }

  getConfigs(): FormArray {
    return this.notificationForm.get('configs') as FormArray;
  }

  addTrigger(triggerType: NotificationTriggerEnum) {
    const model: NotificationTriggerConfigModel = new NotificationTriggerConfigModel();
    model.triggerType = triggerType;
    this.addConfig(model);
  }
  addConfig(model: NotificationTriggerConfigModel) {
    const g = this._formBuilder.group({
      triggerType: [model != null ? model.triggerType : null, Validators.required],
      references: this._formBuilder.control([]),
      configs: this._formBuilder.group([])
    });
    this.getConfigs().push(g);
    if (model) {

      if (UtilFunctions.isValidStringOrArray(model.configs)) {
        const keys = Object.keys(model.configs);
        keys.forEach(c => {
          const key = c;

          this.addNotificationKey(g.get('configs') as FormGroup, key, model.configs[key])
        })

      }

      //g.patchValue(model);

    }
  }

  addNotificationKey(fg: FormGroup, type: NotificationConfigTypeEnum | string, model: NotificationBasicConfigModel) {
    const g: FormGroup = this._formBuilder.group({
      enabled: [true],
    });
    if (type === NotificationConfigTypeEnum.EMAIL) {
      g.addControl('destinations', this._formBuilder.array([]));
      if (UtilFunctions.isValidStringOrArray(model.destinations) === true) {
        for (const x of model.destinations) {
          this.addEmailDestination(g);
        }
      }
    } else {
      g.addControl('destinations', this._formBuilder.control([], Validators.required));
    }
    fg.addControl(type, g);
  }
  getConfig(key: NotificationConfigTypeEnum): FormGroup {
    return (this.notificationForm.get('configs') as FormGroup).get(key) as FormGroup;
  }

  getEmailDestinations(fg: FormGroup): FormArray {
    return (fg.get('destinations')) as FormArray;
  }

  addEmailDestination(fg: FormGroup) {
    const g = this._formBuilder.control(null, [Validators.required, Validators.email]);
    (fg.get('destinations') as FormArray).push(g);
  }

  deleteEmailDestination(index, fg: FormGroup) {
    this.getEmailDestinations(fg).removeAt(index);
  }


  sendTestEmail(value: any) {
    this.parent.service.sendEmailTest(value)
      .then(result => {
        this._messageService.open('Email de teste enviado com sucesso. Confira sua caixa de entrada ou spam', 'SUCESSO', 'success');
      })
  }

  removeTrigger(i: number) {
    this.getConfigs().removeAt(i);
  }

  addConfigSender(index: number, sender: NotificationConfigTypeEnum | string) {
    const configs = (this.getConfigs().at(index).get('configs') as FormGroup);
    if (configs.get(sender) as FormGroup) {
      return;
    }
    const g: FormGroup = this._formBuilder.group({
      enabled: [true],
    });
    if (sender === NotificationConfigTypeEnum.EMAIL) {
      g.addControl('destinations', this._formBuilder.array([]))
    } else {
      g.addControl('destinations', this._formBuilder.control([], Validators.required))
    }
    (configs as FormGroup).addControl(sender, g);
  }

  removerConfigSenders(index: number, sender: NotificationConfigTypeEnum | string) {
    (this.getConfigs().at(index).get('configs') as FormGroup).removeControl(sender);
  }

  hasConfigSender(index: number, sender: NotificationConfigTypeEnum | string) {
    const configs = (this.getConfigs().at(index).get('configs') as FormGroup);
    if (configs.get(sender) as FormGroup) {
      return true;
    }
    return false;
  }

  getConfigSender(index: number, sender: NotificationConfigTypeEnum | string): FormGroup {
    return (this.getConfigs().at(index).get('configs') as FormGroup).get(sender) as FormGroup;
  }

  compareFn(o1: any, o2: any) {
    if(o1.id === o2.id || o1.endpoint === o2.endpoint)
      return true;
  }
}
