import {ChangeDetectorRef, Component, OnDestroy, OnInit, ViewChild, ViewEncapsulation} from "@angular/core";
import {Subject, Subscription} from 'rxjs';
import {HealthCheckerService} from '../health-checker.service';
import {AbstractControl, FormArray, FormBuilder, FormGroup, Validators} from '@angular/forms';
import {MessageDialogService} from '../../../../shared/services/message/message-dialog-service';
import {MatDialogRef} from '@angular/material/dialog';
import {UserService} from '../../../../shared/services/user/user.service';
import {RxStomp, RxStompConfig, RxStompState} from '@stomp/rx-stomp';
import {takeUntil} from "rxjs/operators";
import {UsuarioModel} from '../../../../shared/models/usuario.model';
import {MatStep, MatStepper} from "@angular/material/stepper";
import {StepperSelectionEvent} from '@angular/cdk/stepper';
import {
  HealthCheckerAlertTypeEnum,
  HealthCheckerConfigModel,
  HealthCheckerMessageTopicConstants,
  HealthCheckerModuleConfigModel,
  HealthCheckerModuleModel,
  HealthCheckerModuleTypeEnum,
  HealthCheckerSimpleSystemInfoModel,
  HealthCheckerSystemInfoModel
} from '../../../../shared/models/health-checker.model';
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {WebsocketMessageModel} from '../../../../shared/models/websocket-message.model';
import {CertificateModel, CertificateTypeEnum} from '../../../../shared/models/certificate.model';
import {SplashScreenService} from '../../../../shared/services/splash/splash-screen.service';

@Component({
  selector       : 'health-checker-modal',
  styleUrls      : ['./health-checker-modal.component.scss'],
  templateUrl    : './health-checker-modal.component.html',
  encapsulation  : ViewEncapsulation.None,

  standalone: false
})
export class HealthCheckerModalComponent implements OnInit, OnDestroy
{
  formConfig: FormGroup;
  @ViewChild("horizontalStepper", {static: false})
  stepper: MatStepper;
  private _unsubscribeAll: Subject<any> = new Subject<any>();

  public model: HealthCheckerConfigModel;
  title: string = "Health Checker";
  service: HealthCheckerService;
  myRxStompConfig: RxStompConfig;
  rxStomp: RxStomp;

  tokenInit: string;
  public customPatterns = { 'I': { pattern: new RegExp('\[a-zA-Z0-9\-\]')} };

  watchers: Array<Subscription>;
  healthCheckerSystemInfo: HealthCheckerSystemInfoModel;
  destination: string;
  connectionStatus = "Desconectado";
  HealthCheckerAlertTypeEnum = HealthCheckerAlertTypeEnum;
  HealthCheckerModuleTypeEnum = HealthCheckerModuleTypeEnum;
  socketWaiting = false;
  possibleUsers: Array<UsuarioModel>;
  lastStatus: RxStompState = RxStompState.CLOSED;
  online: boolean;

  /** Impede autocomplete do browser até o usuário focar o campo (trick + autocomplete=new-password). */
  loginPasswordReadonly = true;
  certPasswordReadonly = true;

  constructor(private _formBuilder: FormBuilder,
              private _userService: UserService,
              public dialogRef: MatDialogRef<HealthCheckerModalComponent>,
              private _changeDetectorRef: ChangeDetectorRef,
              private _progressBar: SplashScreenService,
              private _messageService: MessageDialogService)
  {
    this.myRxStompConfig = {
      brokerURL: 'wss://ec2-52-205-189-137.compute-1.amazonaws.com:8089/portalEvoluiWebSocket?Authorization='+this._userService.accessToken,
      debug: (msg: string): void => {
        console.log(new Date(), msg);
      },
      connectHeaders: {Identifier: this._userService.accessToken},

    }
  }

  ngOnInit(): void {

    this.formConfig = this._formBuilder.group({
      id: [null],
      host: ['', Validators.required],
      identifier: ['', Validators.required],
      description: ['', Validators.required],
      healthCheckInterval: [2, Validators.required],
      modules: this._formBuilder.array([]),
      login: this._formBuilder.group({
        login: ['', [Validators.required]],
        password: ['', Validators.required]
      }),
      alerts: this._formBuilder.group([]),
      systemInfo: this._formBuilder.group({
        memory: ['', Validators.required],
        machine: ['', Validators.required],
        software: ['', Validators.required],
        disks: ['', Validators.required],
        processor: ['', Validators.required]
      })
    });

    const keys = Object.keys(HealthCheckerAlertTypeEnum);
    keys.forEach((keyName, index) => {
      (this.formConfig.get('alerts') as FormGroup).addControl(
        keyName, this._formBuilder.group({
          maxPercentual: [0, [Validators.min(0), Validators.max(100)]],
          sendNotification: [true, Validators.required]
        }))
    });
    if (UtilFunctions.isValidStringOrArray(this.model.modules)) {
      for (const m of this.model.modules) {
        this.addModule();
      }
    }
    this.formConfig.patchValue(this.model);

  }

  onLoginPasswordFocus(): void {
    this.loginPasswordReadonly = false;
  }

  onCertPasswordFocus(): void {
    this.certPasswordReadonly = false;
  }

  copyInitTokenToClipboard(): void {
    if (!this.tokenInit) {
      return;
    }
    const done = (): void => {
      this._messageService.open('Token copiado para a área de transferência.', 'Sucesso', 'success');
      this._changeDetectorRef.markForCheck();
    };
    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(this.tokenInit).then(done).catch(() => this.copyInitTokenFallback(done));
    } else {
      this.copyInitTokenFallback(done);
    }
  }

  private copyInitTokenFallback(onDone: () => void): void {
    const ta = document.createElement('textarea');
    ta.value = this.tokenInit;
    ta.style.position = 'fixed';
    ta.style.left = '-9999px';
    document.body.appendChild(ta);
    ta.focus();
    ta.select();
    try {
      if (document.execCommand('copy')) {
        onDone();
      } else {
        this._messageService.open(
          'Não foi possível copiar automaticamente. Selecione o token e use Ctrl+C.',
          'Aviso',
          'warning'
        );
      }
    } catch {
      this._messageService.open(
        'Não foi possível copiar automaticamente. Selecione o token e use Ctrl+C.',
        'Aviso',
        'warning'
      );
    } finally {
      document.body.removeChild(ta);
    }
  }

  ngOnDestroy(): void {
    if (this.rxStomp) {
      this.rxStomp.deactivate();
    }
    this._unsubscribeAll.next(undefined);
    this._unsubscribeAll.complete();
  }


  canSave(): boolean {
    if (this.formConfig) {
      const can = !this.formConfig.invalid &&
        UtilFunctions.isValidStringOrArray(this.destination) &&
        this.rxStomp != null && this.rxStomp.active;
      if (can === true) {
        const incompleted = this.stepper.steps.filter(x => x.completed === false);
        if (incompleted && incompleted.length > 0) {
          return false;
        }
      }
      return can;
    }
    return false;
  }

  doSaving() {
    this.model = this.formConfig.value;
    this.service.save(this.model).then(value => {
      this.formConfig.patchValue(value);
      this.sendMessage(HealthCheckerMessageTopicConstants.SAVE_CONFIG_REQUEST, value, 10000);
    })
  }

  generateToken() {
    const login: UsuarioModel = new UsuarioModel();
    login.login = this.formConfig.get("login").get("login").value;
    login.password = this.formConfig.get("login").get("password").value;
    login.name = this._userService.accessToken;
    login.newPassword = this.model.identifier;

    this.service.generateToken(login).then(value => {
      this.connectionStatus = "Conectando WebSocket...";
      const url = new URL(value.endpoint);
      this.tokenInit = value.token;
      this.formConfig.get('host').setValue(value.endpoint);
      this.online = value.online;
      this.myRxStompConfig.brokerURL = (url.protocol === 'https:' ? 'wss:' : 'ws:') + url.host  + '/portalEvoluiWebSocket?Authorization='+this._userService.accessToken
      this.connectWebsocket();
      this.stepper.next();
    });
  }

  connectWebsocket() {
    this.rxStomp = new RxStomp();
    this.rxStomp.connectionState$.pipe((takeUntil(this._unsubscribeAll)))
      .subscribe((value: RxStompState) => {
        if (value === RxStompState.OPEN) {

          this.connectionStatus = 'Aguardando HealthChecker...';
          if (UtilFunctions.isValidStringOrArray(this.model.identifier)) {
            if (this.online === true) {
              this.destination = this.model.identifier;
              const me = this;
              setTimeout(() => {
                me.sendMessage(HealthCheckerMessageTopicConstants.START_REQUEST, null, 10000);
              }, 100)
            }

          }
        }
        this.lastStatus = value;
      });
    this.rxStomp.webSocketErrors$.pipe((takeUntil(this._unsubscribeAll)))
      .subscribe((value: Event) => {
        const c: WebSocket = value.target as WebSocket;
        if (c != null && c.readyState === RxStompState.CLOSED) {
          this.connectionStatus = 'Desconectado';
          this.destination = null;
          const me = this;
          setTimeout(() => {
            me.stepper.selectedIndex = 0;
            me._messageService.open('WebSocket foi desconectado. Cheque o backend da aplicação', 'Erro', 'error');
          }, 300);
        }
      });

    this.rxStomp.configure(this.myRxStompConfig);
    this.rxStomp.activate();
    this.addWatchers();

  }

  stepperChanged(step: StepperSelectionEvent) {

    if (step.selectedIndex === 0 && this.rxStomp && this.rxStomp.active) {
      this.rxStomp.deactivate();
      this.tokenInit = null;
      this.destination = null;
      this.formConfig.get('systemInfo').patchValue({});
      this.formConfig.get('identifier').setValue(null);
    }
  }

  addWatchers() {
    const me = this;

    this.watchers = [];
    {
      const subscription = this.rxStomp
        .watch({destination: `/topic/${HealthCheckerMessageTopicConstants.CLIENT_DISCONECTION}`})
        .subscribe((message) => {
          const m = JSON.parse(message.body);
          if (m && m.client === this.destination) {
            me.destination = null;
            setTimeout(() => {
              me.stepper.selectedIndex = 0;
              me._messageService.open('Health Checket foi desconectado', 'Erro', 'error');
            }, 300);
          }

        });
      this.watchers.push(subscription);
    }
    {
      const subscription = this.rxStomp
        .watch({destination: `/queue/${HealthCheckerMessageTopicConstants.HEY}/${this._userService.accessToken}`})
        .subscribe((message) => {
          const m: WebsocketMessageModel = JSON.parse(message.body);
          this.destination = m.from;
          this.sendMessage(HealthCheckerMessageTopicConstants.START_REQUEST, null, 10000);
        });
      this.watchers.push(subscription);
    }
    {
      const subscription = this.rxStomp
        .watch({destination: `/queue/${HealthCheckerMessageTopicConstants.START_RESPONSE}/${this._userService.accessToken}`})
        .subscribe((message) => {
          // Não exigir socketWaiting: evita descartar resposta válida em corrida (timeout 10s já zerou a flag) ou
          // quando há dois START_REQUEST (ex.: OPEN com online=true + HEY) — o segundo start-response era ignorado.
          me.socketWaiting = false;
          me._progressBar.hide();
          const m: WebsocketMessageModel = JSON.parse(message.body);
          if (UtilFunctions.isValidStringOrArray(m.error)) {
            setTimeout(() => {
              me._messageService.open(m.error, 'ERRO', 'error');
            }, 100);
            return;
          }
          this.healthCheckerSystemInfo = m.message;
          this.connectionStatus = "Health Checker Online";
          try {
            const si: HealthCheckerSimpleSystemInfoModel = HealthCheckerSimpleSystemInfoModel.parseFromSystemInfo(this.healthCheckerSystemInfo);
            this.formConfig.get('systemInfo').patchValue(si);
          } catch (e) {
            console.error('parseFromSystemInfo', e);
          }
          this.formConfig.get('identifier').setValue(this.destination);

        });
      this.watchers.push(subscription);
    }
    {
      const subscription = this.rxStomp
        .watch({destination: `/queue/${HealthCheckerMessageTopicConstants.TEST_CONFIG_RESPONSE}/${this._userService.accessToken}`})
        .subscribe((message) => {
          if (me.socketWaiting === false) {
            return;
          }
          me.socketWaiting = false;
          me._progressBar.hide();
          const m: WebsocketMessageModel = JSON.parse(message.body);
          if (UtilFunctions.isValidStringOrArray(m.error)) {
            setTimeout(() => {
              me._messageService.open(m.error, 'ERRO', 'error');
            }, 100);
            return;
          }
          const resp: HealthCheckerModuleModel = m.message;
          if (resp.health === false) {
            setTimeout(() => {
              me._messageService.open(resp.error, 'ERRO', 'error');
            }, 100);

          } else {
            setTimeout(() => {
              me._messageService.open('Sistema está saudável', 'SUCESSO', 'success');
            }, 100);
          }
        });
      this.watchers.push(subscription);
    }
    {
      const subscription = this.rxStomp
        .watch({destination: `/queue/${HealthCheckerMessageTopicConstants.SAVE_CONFIG_RESPONSE}/${this._userService.accessToken}`})
        .subscribe((message) => {
          if (me.socketWaiting === false) {
            return;
          }
          me.socketWaiting = false;
          me._progressBar.hide();
          const m: WebsocketMessageModel = JSON.parse(message.body);
          if (UtilFunctions.isValidStringOrArray(m.error)) {
            setTimeout(() => {
              me._messageService.open(m.error, 'ERRO', 'error');
            }, 100);

          } else {
            setTimeout(() => {
              me._messageService.open('Configurações salvas com sucesso!', 'SUCESSO', 'success');
            }, 100);
          }
        });
      this.watchers.push(subscription);
    }
    // @ts-ignore

  }


  sendMessage(topic: string, message: any, waitTime?: number) {
    const model = new WebsocketMessageModel();
    model.from = this._userService.accessToken;
    model.to = this.destination;
    model.message = message;
    this.rxStomp.publish({
      destination: `/app/${topic}`,
      body: JSON.stringify(model)
    });

    if (waitTime && waitTime > 0) {
      const me = this;
      this.socketWaiting = true;
      this._progressBar.show();
      setTimeout(() => {
        if (me.socketWaiting) {
          me._progressBar.hide();
          me.socketWaiting = false;
          me._messageService.open('Tempo de espera expirado', 'TIMEOUT', 'error');
        }
      }, waitTime)
    }
  }

  backToUser() {
    if (UtilFunctions.isValidStringOrArray(this.destination)) {
      this._messageService.open(
        'Se voltar ao passo anterior, o websocket será desconectado, e deverá refazer os procedimentos na máquina. Deseja voltar mesmo assim?',
        'CONFIRMAÇÃO', 'confirm').subscribe((result) => {
        if (result === 'confirmed') {
          const me = this;
          this.destination = null;
          setTimeout(() => {
            me.stepper.previous();
          }, 100)

        }
      });
    } else {
      this.stepper.previous();
    }

  }


  getAlerts(): FormGroup {
    return this.formConfig.get('alerts') as FormGroup;
  }

  /** Valor atual do slider de alerta (evita #slider no *ngFor, que não reflete o MatSlider MDC). */
  getAlertMaxPercent(alertKey: string): number {
    const raw = this.getAlerts()?.get(alertKey)?.get('maxPercentual')?.value;
    const n = Number(raw);
    return Number.isFinite(n) ? Math.round(n) : 0;
  }

  getControlName(c: AbstractControl): string | null {
    const formGroup = c.parent.controls;
    return Object.keys(formGroup).find(name => c === formGroup[name]) || null;
  }

  formatLabel(value: number): string {
    return `${value}%`;
  }

  buildModule(): FormGroup {

    const c = this._formBuilder.group({
      id: [null],
      moduleType: [HealthCheckerModuleTypeEnum.WEB, Validators.required],
      identifier: ['', Validators.required],
      description: ['', Validators.required],
      commandAddress: ['', Validators.required],
      acceptableResponsePattern: ['', Validators.required],
      bypassCertificate: [true, Validators.required],
      sendNotification: [true, Validators.required],
      clientCertificate: this._formBuilder.group({
        certificateType: [null],
        file: [null],
        fileName: [null],
        password: ['']
      })

    });

    return c;
  }

  addModule() {
    (this.formConfig.get('modules') as FormArray).push(this.buildModule());
  }

  getModules(): FormArray {
    return this.formConfig.get('modules') as FormArray;
  }

  removeModule(index: number) {
    if (index >= 0) {
      (this.formConfig.get('modules') as FormArray).removeAt(index);
    }
  }


  testModule(index: number) {
    if (index >= 0) {
      const module: FormGroup = (this.formConfig.get('modules') as FormArray).at(index) as FormGroup;
      const model: HealthCheckerModuleConfigModel = module.value;
      this.sendMessage(HealthCheckerMessageTopicConstants.TEST_CONFIG_REQUEST, model, 30000);
    }
  }
  getModuleTitle(i: number, c: AbstractControl): string {
    let text = 'Módulo ' + i;

    if (UtilFunctions.isValidStringOrArray(c.get('identifier').value as string)) {
      text = c.get('identifier').value as string;
    }
    return text;
  }
  getModuleDescription(i: number, c: AbstractControl): string {
    let text = '';

    if (UtilFunctions.isValidStringOrArray(c.get('description').value as string)) {
      text = c.get('description').value as string;
    }
    return text;
  }

  public droppedCertificate(files: File[], module: FormGroup) {
    const certificate: FormGroup = module.get('clientCertificate') as FormGroup;
    certificate.reset();
    const me = this;
    files.forEach(file => {
      const ext = UtilFunctions.getFileExtension(file.name);
      if (ext !== '.jks' && ext !== '.pfx' && ext !== '.p12') {
        this._messageService.open('Extensão não permitida', 'ERRO', 'error');
        return;
      }

      file.arrayBuffer().then(buffer => {
        certificate.get('file').setValue(UtilFunctions.arrayBufferToByteArray(buffer));
        certificate.get('fileName').setValue(file.name);
        if (ext === '.jks') {
          certificate.get('certificateType').setValue(CertificateTypeEnum.JKS);
        } else {
          certificate.get('certificateType').setValue(CertificateTypeEnum.PKCS12);
        }
        me._changeDetectorRef.markForCheck();

        setTimeout(function () {
          me._changeDetectorRef.markForCheck();
        }, 200);

      }).catch(reason => {
        me._messageService.open(reason, 'ERRO', 'error');
      });
    });
  }

  clearCerificate(module: FormGroup) {
    const certificate: FormGroup = module.get('clientCertificate') as FormGroup;
    certificate.reset();
    this._changeDetectorRef.markForCheck();
  }

  testCertificate(module: FormGroup) {
    const certificate: FormGroup = module.get('clientCertificate') as FormGroup;
    const model: CertificateModel = certificate.value as CertificateModel;
    this.service.testCertificate(model).then(result => {
      this._messageService.open(`<h1>Certificado Validado</h1><pre>${JSON.stringify(result, null, 2)}</pre>`, 'Sucesso', 'success');
    })
  }

  setCompleted(step: MatStep) {
    step.completed = true;
    this._changeDetectorRef.markForCheck();
  }
}
