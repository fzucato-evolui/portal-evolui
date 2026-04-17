import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from "@angular/core";
import {Terminal} from '@xterm/xterm';
import {FitAddon} from '@xterm/addon-fit';
import {Subject, Subscription} from 'rxjs';
import {HealthCheckerService} from '../health-checker.service';
import {FormBuilder} from '@angular/forms';
import {MessageDialogService} from '../../../../shared/services/message/message-dialog-service';
import {MatDialogRef} from '@angular/material/dialog';
import {UserService} from '../../../../shared/services/user/user.service';
import {RxStomp, RxStompConfig, RxStompState} from '@stomp/rx-stomp';
import {takeUntil} from "rxjs/operators";
import {UsuarioModel} from '../../../../shared/models/usuario.model';
import {
  HealthCheckerConfigModel,
  HealthCheckerMessageTopicConstants,
  HealthCheckerSimpleSystemInfoModel,
  HealthCheckerSystemInfoModel
} from '../../../../shared/models/health-checker.model';
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {ConsoleResponseMessageModel, WebsocketMessageModel} from '../../../../shared/models/websocket-message.model';
import {SplashScreenService} from '../../../../shared/services/splash/splash-screen.service';
import {MediaMatcher} from '@angular/cdk/layout';
import {MatSlideToggleChange} from "@angular/material/slide-toggle";
import {SessionCommandHistory} from './session-command-history';
import {ApexOptions, ChartComponent} from 'ng-apexcharts';

export type MonitorOptionType = 'dashboard' |
  'hardware' | 'hardware-info' | 'hardware-processor' | 'hardware-memory' | 'hardware-disks' | 'hardware-logical-volumes' | 'hardware-network' |
  'software' | 'software-info' | 'software-version' | 'software-file-system' | 'software-network' | 'software-services' | 'software-protocol-stats' |
  'software-sessions' | 'software-process' | 'prompt';

export class SyncConsoleMessages {
  private _lastFlushSequence = -1;
  private _consoleMessages: Array<ConsoleResponseMessageModel> = [];

  public reset() {
    this._lastFlushSequence = -1;
    this._consoleMessages = [];
  }
  public addMessage(message: ConsoleResponseMessageModel): Array<ConsoleResponseMessageModel>  {
    this._consoleMessages.push(message);
    return this.flush()
  }

  private flush(): Array<ConsoleResponseMessageModel> {
    const ret: Array<ConsoleResponseMessageModel> = [];
    while(true) {
      const index = this._consoleMessages.findIndex(x => x.sequence === this._lastFlushSequence + 1);
      if (index >= 0) {
        const m = {...this._consoleMessages[index]};
        this._lastFlushSequence = m.sequence;
        ret.push(m);
        this._consoleMessages.splice(index, 1);
      } else {
        break;
      }
    }
    return ret;
  }

}
// @ts-ignore
@Component({
  selector       : 'health-checker-modal',
  styleUrls      : ['./health-checker-monitor-modal.component.scss'],
  templateUrl    : './health-checker-monitor-modal.component.html',
  encapsulation  : ViewEncapsulation.None,

  standalone: false
})
export class HealthCheckerMonitorModalComponent implements OnInit, OnDestroy, AfterViewInit
{
  @ViewChild('xtermHost', {static: false}) xtermHost: ElementRef<HTMLDivElement>;
  readonly ANSI_RESET = "\x1B[0m";
  readonly ANSI_YELLOW = "\x1B[33m";
  readonly ANSI_CYAN = "\x1B[36m";
  readonly ANSI_RED = "\x1B[31m";
  /** Linha em edição no prompt (modo local, antes de Enter). */
  private shellLine = '';
  private shellCursor = 0;
  private readonly shellHistory = new SessionCommandHistory();
  /** Entrada enviada ao processo remoto enquanto `waitTerminalFinish`. */
  private busyStdinLine = '';
  private xterm: Terminal | null = null;
  private fitAddon: FitAddon | null = null;
  private xtermResizeObserver: ResizeObserver | null = null;
  /** cwd do último ConsoleResponseMessage; prioridade sobre system info. */
  private remoteShellCwd: string | null = null;

  /** Prompt estilo cmd: `C:\path>` ou `$ ` até haver cwd. */
  get promptPrefix(): string {
    const fromConsole = this.remoteShellCwd;
    const fromSi =
      this.healthCheckerSystemInfo?.operatingSystem?.currentProcess?.currentWorkingDirectory;
    const path = (fromConsole && fromConsole.length > 0) ? fromConsole : (fromSi || '');
    if (path.length > 0) {
      return path + '>';
    }
    return '$ ';
  }
  drawerMode: 'over' | 'side' = 'side';
  drawerOpened: boolean = true;
  private _unsubscribeAll: Subject<any> = new Subject<any>();
  public model: HealthCheckerConfigModel;
  public healthCheckerSystemInfo: HealthCheckerSystemInfoModel = new HealthCheckerSystemInfoModel(); //si as HealthCheckerSystemInfoModel;
  title: string = "Health Checker Monitor";
  service: HealthCheckerService;
  myRxStompConfig: RxStompConfig;
  rxStomp: RxStomp;
  tokenInit: string;
  watchers: Array<Subscription>;
  destination: string;
  private _socketWaiting = false;
  get socketWaiting(): boolean {
    return  this._socketWaiting;
  }

  set socketWaiting(value: boolean) {
    this._socketWaiting = value
    if (value === false && this.timeoutMessage) {
      clearTimeout(this.timeoutMessage);
    }
  }
  lastStatus: RxStompState = RxStompState.CLOSED;
  online: boolean;
  /** Só atualiza gráficos de linha e aceita stream STOMP rápido quando o operador liga "Tempo Real". */
  realTime = false;
  /** Evita rebuild completo do gráfico de discos quando só os percentuais mudam (modo tempo real). */
  private lastDiskCategoriesKey = '';
  /** Últimos % de uso por volume (string "a|b|c") para decidir se vale redesenhar barras em tempo real. */
  private lastDiskPctKey = '';
  private lastDiskBarRefreshAt = 0;
  private readonly diskRealtimeMinIntervalMs = 45000;
  private readonly diskRealtimeMinDeltaPct = 0.45;
  mobileQuery: MediaQueryList;
  private _mobileQueryListener: () => void;
  monitorOption: MonitorOptionType = 'dashboard';

  memoryPieOptions: ApexOptions;
  @ViewChild("memoryPieChart", { static: false })
  memoryPieChart: ChartComponent;
  cpuPieOptions: ApexOptions;
  @ViewChild("cpuPieChart", { static: false })
  cpuPieChart: ChartComponent;
  openedFilesPieOptions: ApexOptions;
  @ViewChild("openedFilesPieChart", { static: false })
  openedFilesPieChart: ChartComponent;
  diskUsageColumnOptions: ApexOptions;
  @ViewChild("diskUsageColumnChart", { static: false })
  diskUsageColumnChart: ChartComponent;
  cpuLineOptions: Partial<ApexOptions>;
  @ViewChild("cpuLineChart", { static: false })
  cpuLineChart: ChartComponent;
  cpuLineDataSeries: Array<{x: Date, y: number}> = [];
  memoryLineOptions: Partial<ApexOptions>;
  @ViewChild("memoryLineChart", { static: false })
  memoryLineChart: ChartComponent;
  memoryLineDataSeries: Array<{x: Date, y: number}> = [];

  delayedFunction: any;
  waitTerminalFinish: boolean = false;
  syncConsoleMessages: SyncConsoleMessages = new SyncConsoleMessages();
  timeoutMessage: any;

  constructor(private _formBuilder: FormBuilder,
              private _userService: UserService,
              public dialogRef: MatDialogRef<HealthCheckerMonitorModalComponent>,
              private _changeDetectorRef: ChangeDetectorRef,
              private _progressBar: SplashScreenService,
              media: MediaMatcher,
              private _messageService: MessageDialogService)
  {
    this.mobileQuery = media.matchMedia('(max-width: 600px)');
    this._mobileQueryListener = () => _changeDetectorRef.detectChanges();
    this.mobileQuery.addListener(this._mobileQueryListener);
    this.myRxStompConfig = {
      brokerURL: 'wss://ec2-52-205-189-137.compute-1.amazonaws.com:8089/portalEvoluiWebSocket?Authorization='+this._userService.accessToken,
      debug: (msg: string): void => {
        if (UtilFunctions.isValidStringOrArray(msg) === true) {
          // Ruído do @stomp/stompjs (heartbeat / qualquer chunk no socket), não indica resposta do monitor.
          if (msg.includes('<<< PONG') || msg.includes('>>> PING') || msg.includes('Received data')) {
            return;
          }
        }
        //console.log(new Date(), msg);
      },
      connectHeaders: {Identifier: this._userService.accessToken},

    }

  }

  ngOnInit(): void {
    this.loadCharts();

  }

  ngAfterViewInit(): void {
    setTimeout(() => this.initShellTerminal(), 0);
  }

  ngOnDestroy(): void {
    this.disposeShellTerminal();
    if (this.rxStomp) {
      this.rxStomp.deactivate();
    }
    if (this.delayedFunction) {
      clearInterval(this.delayedFunction);
    }
    this._unsubscribeAll.next(undefined);
    this._unsubscribeAll.complete();
  }

  generateToken() {
    const login: UsuarioModel = new UsuarioModel();
    login.login = this.model.login.login;
    login.password = this.model.login.password;
    login.name = this._userService.accessToken;
    login.newPassword = this.model.identifier;

    this.service.generateToken(login).then(value => {
      this.online = value.online;
      if (this.online === false) {
        this._messageService.open('Health Checker não está online', 'ERRO', 'error');
        return;
      }
      const url = new URL(value.endpoint);
      this.tokenInit = value.token;
      this.online = value.online;
      this.myRxStompConfig.brokerURL = (url.protocol === 'https:' ? 'wss:' : 'ws:') + url.host  + '/portalEvoluiWebSocket?Authorization='+this._userService.accessToken
      this.connectWebsocket();
      this.mobileQuery.dispatchEvent(new Event('load', null));
      setTimeout(() => {
        this.fitAddon?.fit();
        if (this.monitorOption === 'prompt') {
          this.xterm?.focus();
        }
      }, 400);
    });
  }

  connectWebsocket() {
    this.rxStomp = new RxStomp();
    this.rxStomp.connectionState$.pipe((takeUntil(this._unsubscribeAll)))
      .subscribe((value: RxStompState) => {
        if (value === RxStompState.OPEN) {

          if (UtilFunctions.isValidStringOrArray(this.model.identifier)) {
            if (this.online === true) {
              this.destination = this.model.identifier;
              const me = this;
              setTimeout(() => {
                me.sendMessage(HealthCheckerMessageTopicConstants.START_REQUEST, null, 30000);
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
          const me = this;
          setTimeout(() => {
            me.closeWebSocket();
            me._messageService.open('WebSocket foi desconectado. Cheque o backend da aplicação', 'Erro', 'error');
          }, 300);
        }
      });

    this.rxStomp.configure(this.myRxStompConfig);
    this.rxStomp.activate();
    this.addWatchers();

  }

  closeWebSocket() {
    if (this.rxStomp) {
      try {
        this.rxStomp.deactivate();
      } catch (e) {

      }
    }
    this.tokenInit = null;
    this.destination = null;
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
            me.closeWebSocket();
            setTimeout(() => {
              me._messageService.open('Health Checket foi desconectado', 'Erro', 'error');
            }, 300);
          }

        });
      this.watchers.push(subscription);
    }
    {
      const subscription = this.rxStomp
        .watch({destination: `/queue/${HealthCheckerMessageTopicConstants.START_RESPONSE}/${this._userService.accessToken}`})
        .subscribe((message) => {
          if (me.socketWaiting === false) {
            return;
          }
          me.socketWaiting = false;
          me._progressBar.hide();
          const m: WebsocketMessageModel = JSON.parse(message.body);
          //console.log('message', m);
          if (UtilFunctions.isValidStringOrArray(m.error)) {
            setTimeout(() => {
              me._messageService.open(m.error, 'ERRO', 'error');
            }, 100);
            return;
          }
          me.healthCheckerSystemInfo = m.message;
          me.updateCharts();
          me._changeDetectorRef.detectChanges();
        });
      this.watchers.push(subscription);
    }
    {
      const subscription = this.rxStomp
        .watch({destination: `/queue/${HealthCheckerMessageTopicConstants.SYSTEM_INFO_RESPONSE}/${this._userService.accessToken}`})
        .subscribe((message) => {
          if (me.socketWaiting === true) {
            me.socketWaiting = false;
            me._progressBar.hide();
          }

          const m: WebsocketMessageModel = JSON.parse(message.body);
          if (UtilFunctions.isValidStringOrArray(m.error)) {
            setTimeout(() => {
              me._messageService.open(m.error, 'ERRO', 'error');
            }, 100);
            return;
          }
          me.healthCheckerSystemInfo = m.message;
          me.updateCharts();
          me._changeDetectorRef.detectChanges();
        });
      this.watchers.push(subscription);
    }
    {
      const subscription = this.rxStomp
        .watch({destination: `/queue/${HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_RESPONSE}/${this._userService.accessToken}`})
        .subscribe((message) => {
          //console.log('body', message.body);
          if (me.waitTerminalFinish === false) {
            return;
          }
          me.socketWaiting = false;
          me._progressBar.hide();
          const m: WebsocketMessageModel = JSON.parse(message.body);
          if (UtilFunctions.isValidStringOrArray(m.error)) {
            setTimeout(() => {
              me._messageService.open(m.error, 'ERRO', 'error');
            }, 100);
            me.waitTerminalFinish = false;
            me.writeShellPromptAfterOutput();
          } else {
            const consoleModel: ConsoleResponseMessageModel = m.message;
            const messages = me.syncConsoleMessages.addMessage(consoleModel);
            if (UtilFunctions.isValidStringOrArray(messages)) {
              messages.forEach(value => {
                if (UtilFunctions.isValidStringOrArray(value.currentDirectory)) {
                  me.remoteShellCwd = value.currentDirectory;
                }
                if (UtilFunctions.isValidStringOrArray(value.outputError) === true) {
                  let s = value.outputError;
                  s = s.replace(/\r?\n/g, "\r\n");
                  this.writeShellStderr(s);
                } else if (UtilFunctions.isValidStringOrArray(value.output) === true) {
                  let s = value.output;
                  s = s.replace(/\r?\n/g, "\r\n");
                  this.writeShellStdout(s);
                }
                if (value.finished) {
                  this.waitTerminalFinish = false;
                  this.writeShellPromptAfterOutput();
                }
              });

            }
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
    // Marcar espera *antes* do publish: a resposta STOMP pode voltar no mesmo tick;
    // se socketWaiting só fosse true depois, o handler de system-info-response descartava a mensagem (wasAwaiting false).
    if (waitTime && waitTime > 0) {
      const me = this;
      this.socketWaiting = true;
      this._progressBar.show();
      this.timeoutMessage = setTimeout(() => {
        if (me.socketWaiting) {
          me._progressBar.hide();
          me.socketWaiting = false;
          me.waitTerminalFinish = false;
          me._messageService.open('Tempo de espera expirado', 'TIMEOUT', 'error');
        }
      }, waitTime);
    }
    this.rxStomp.publish({
      destination: `/app/${topic}`,
      body: JSON.stringify(model)
    });
  }

  changeOption(option: MonitorOptionType) {
    this.monitorOption = option;
    if (option === 'prompt') {
      const siCwd =
        this.healthCheckerSystemInfo?.operatingSystem?.currentProcess?.currentWorkingDirectory;
      if (siCwd && !this.remoteShellCwd) {
        this.remoteShellCwd = siCwd;
      }
      setTimeout(() => {
        this.fitAddon?.fit();
        this.applyShellMinCols();
        this.xterm?.focus();
      }, 80);
    }
  }

  /** Indica qual shell o agente remoto usa (Go: cmd.exe /c no Windows, sh -c no Unix). */
  get remoteShellHint(): string {
    const fam = this.healthCheckerSystemInfo?.operatingSystem?.family?.toLowerCase?.() ?? '';
    if (fam.includes('windows')) {
      return 'Shell remoto: comandos executados como cmd.exe (Prompt de Comando), não PowerShell.';
    }
    if (fam.includes('linux') || fam.includes('unix') || fam.includes('mac') || fam.includes('darwin')) {
      return 'Shell remoto: comandos executados como sh -c (shell Unix).';
    }
    return 'Shell remoto: no Windows o agente usa cmd.exe; no Linux/macOS, sh -c. Atualize o monitor para refletir o SO detectado.';
  }

  changeRealTime(event: MatSlideToggleChange) {
    if (UtilFunctions.isValidStringOrArray(this.destination)) {
      if (event.checked === true) {
        if (this.monitorOption === 'prompt') {
          this.monitorOption = 'dashboard';
        }
        this.lastDiskCategoriesKey = '';
        this.lastDiskPctKey = '';
        this.lastDiskBarRefreshAt = 0;
        this.cpuLineDataSeries.splice(0, this.cpuLineDataSeries.length - 1);
        this.memoryLineDataSeries.splice(0, this.memoryLineDataSeries.length - 1);
        this.sendMessage(HealthCheckerMessageTopicConstants.SYSTEM_INFO_REQUEST_START, true, 30000);
      } else {
        this.lastDiskCategoriesKey = '';
        this.lastDiskPctKey = '';
        this.lastDiskBarRefreshAt = 0;
        this.sendMessage(HealthCheckerMessageTopicConstants.SYSTEM_INFO_REQUEST_STOP, null, 30000);
      }
    }
  }

  getTotalRunningOfUserProcess(): number {
    const os = this.healthCheckerSystemInfo.operatingSystem;
    const current = os?.currentProcess;
    const processes = os?.processes;
    if (!current || !UtilFunctions.isValidStringOrArray(processes)) {
      return 0;
    }
    const userId = current.userID;
    return processes.filter(x => x.userID === userId).length;
  }

  getTotalRunningOfUserThreads(): number {
    const os = this.healthCheckerSystemInfo.operatingSystem;
    const current = os?.currentProcess;
    const processes = os?.processes;
    if (!current || !UtilFunctions.isValidStringOrArray(processes)) {
      return 0;
    }
    const userId = current.userID;
    const totalUser = processes.filter(x => x.userID === userId);
    if (UtilFunctions.isValidStringOrArray(totalUser) === true) {
      return totalUser.map(item => item.threadCount).reduce((prev, next) => prev + next);
    }
    return 0;
  }

  getTotalRunningServices(): number {
    const services = this.healthCheckerSystemInfo.operatingSystem?.services;
    if (!UtilFunctions.isValidStringOrArray(services)) {
      return 0;
    }
    return services
      .map(item => {
        if (item.state === 'RUNNING') {
          return 1;
        }
        return null;
      }).reduce((prev, next) => {
        if (next) {
          return prev + next;
        } else {
          return prev;
        }
      });
  }

  getTotalConnectedPorts(): number {
    const connections = this.healthCheckerSystemInfo.operatingSystem?.internetProtocolStats?.connections;
    if (!UtilFunctions.isValidStringOrArray(connections)) {
      return 0;
    }
    return connections
      .map(item => {
        if (item.state === 'ESTABLISHED') {
          return 1;
        }
        return null;
      }).reduce((prev, next) => {
        if (next) {
          return prev + next;
        } else {
          return prev;
        }
      });
  }

  private isDarkTheme(): boolean {
    if (typeof document === 'undefined') {
      return false;
    }
    return document.body.classList.contains('dark') || document.documentElement.classList.contains('dark');
  }

  loadCharts() {
    const dark = this.isDarkTheme();
    const foreColor = dark ? 'rgba(255,255,255,0.87)' : '#373d3f';
    const tooltipTheme = dark ? 'dark' : 'light';

    this.memoryPieOptions = {
      theme: {
        mode: dark ? 'dark' : 'light',
      },
      chart      : {
        animations: {
          speed           : 400,
          animateGradually: {
            enabled: false,
          },
        },
        fontFamily: 'inherit',
        foreColor,
        background: 'transparent',
        height    : '100%',
        type      : 'donut',
        sparkline : {
          enabled: true,
        },
      },
      colors     : ['#3182CE', '#63B3ED'],
      labels     : ['Usada', 'Disponível'],
      plotOptions: {
        pie: {
          customScale  : 0.9,
          expandOnClick: false,
          donut        : {
            size: '70%',
            labels: {
              show: true,
              total: {
                showAlways: true,
                show: true,
                label: 'Em uso',
                formatter: (w: any) => {
                  const series = w.config.series;
                  const used = series[0];
                  const available = series[1];
                  const total = used + available;
                  return UtilFunctions.roundNumber(used / total * 100)  + '%';
                }
              }
            }
          },
        },
      },
      series     : [],
      states     : {
        hover : {
          filter: {
            type: 'none',
          },
        },
        active: {
          filter: {
            type: 'none',
          },
        },
      },
      tooltip    : {
        enabled        : true,
        fillSeriesColor: false,
        theme          : tooltipTheme,
        custom         : ({
                            seriesIndex,
                            w,
                          }): string => `<div class="flex items-center h-8 min-h-8 max-h-8 px-3 z-50">
                                                    <div class="w-3 h-3 rounded-full" style="background-color: ${w.config.colors[seriesIndex]};"></div>
                                                    <div class="ml-2 text-md leading-none">${w.config.labels[seriesIndex]}:</div>
                                                    <div class="ml-2 text-md font-bold leading-none">
                                                        ${UtilFunctions.roundNumber(w.config.series[seriesIndex]/ this.healthCheckerSystemInfo.hardware.memory.total * 100)}%
                                                        (${UtilFunctions.bytes2Size(w.config.series[seriesIndex])})
                                                    </div>
                                                </div>`,
      },
    };
    this.cpuPieOptions = {
      theme: {
        mode: dark ? 'dark' : 'light',
      },
      chart      : {
        animations: {
          speed           : 400,
          animateGradually: {
            enabled: false,
          },
        },
        fontFamily: 'inherit',
        foreColor,
        background: 'transparent',
        height    : '100%',
        type      : 'donut',
        sparkline : {
          enabled: true,
        },
      },
      colors     : ['#319795', '#4FD1C5'],
      labels     : ['Usado', 'Disponível'],
      plotOptions: {
        pie: {
          customScale  : 0.9,
          expandOnClick: false,
          donut        : {
            size: '70%',
            labels: {
              show: true,
              total: {
                showAlways: true,
                show: true,
                label: 'Em uso',
                formatter: (w: any) => {
                  const series = w.config.series;
                  const used = series[0];
                  return UtilFunctions.roundNumber(used) + '%';
                }
              }
            }
          },
        },
      },
      series     : [],
      states     : {
        hover : {
          filter: {
            type: 'none',
          },
        },
        active: {
          filter: {
            type: 'none',
          },
        },
      },
      tooltip    : {
        enabled        : true,
        fillSeriesColor: false,
        theme          : tooltipTheme,
        custom         : ({
                            seriesIndex,
                            w,
                          }): string => `<div class="flex items-center h-8 min-h-8 max-h-8 px-3 z-50">
                                                    <div class="w-3 h-3 rounded-full" style="background-color: ${w.config.colors[seriesIndex]};"></div>
                                                    <div class="ml-2 text-md leading-none">${w.config.labels[seriesIndex]}:</div>
                                                    <div class="ml-2 text-md font-bold leading-none">
                                                        ${w.config.series[seriesIndex]}%
                                                    </div>
                                                </div>`,
      },
    };
    this.openedFilesPieOptions = {
      theme: {
        mode: dark ? 'dark' : 'light',
      },
      chart      : {
        animations: {
          speed           : 400,
          animateGradually: {
            enabled: false,
          },
        },
        fontFamily: 'inherit',
        foreColor,
        background: 'transparent',
        height    : '100%',
        type      : 'donut',
        sparkline : {
          enabled: true,
        },
      },
      colors     : ['#DD6B20', '#F6AD55'],
      labels     : ['Abertos', 'Disponível'],
      plotOptions: {
        pie: {
          customScale  : 0.9,
          expandOnClick: false,
          donut        : {
            size: '70%',
            labels: {
              show: true,
              total: {
                showAlways: true,
                show: true,
                label: 'Em uso',
                formatter: (w: any) => {
                  const series = w.config.series;
                  const used = series[0];
                  const available = series[1];
                  const total = used + available;
                  return UtilFunctions.roundNumber(used / total * 100)  + '%';
                }
              }
            }
          },
        },
      },
      series     : [],
      states     : {
        hover : {
          filter: {
            type: 'none',
          },
        },
        active: {
          filter: {
            type: 'none',
          },
        },
      },
      tooltip    : {
        enabled        : true,
        fillSeriesColor: false,
        theme          : tooltipTheme,
        custom         : ({
                            seriesIndex,
                            w,
                          }): string => `<div class="flex items-center h-8 min-h-8 max-h-8 px-3 z-50">
                                                    <div class="w-3 h-3 rounded-full" style="background-color: ${w.config.colors[seriesIndex]};"></div>
                                                    <div class="ml-2 text-md leading-none">${w.config.labels[seriesIndex]}:</div>
                                                    <div class="ml-2 text-md font-bold leading-none">
                                                        ${UtilFunctions.roundNumber(w.config.series[seriesIndex]/ this.healthCheckerSystemInfo.operatingSystem.fileSystem.maxFileDescriptors * 100)}%
                                                        (${UtilFunctions.bytes2Size(w.config.series[seriesIndex], false)})
                                                    </div>
                                                </div>`,
      },
    };
    this.diskUsageColumnOptions = {
      grid: {
        show: false
      },
      theme: {
        mode: dark ? 'dark' : 'light',
        monochrome: {
          enabled: true,
          color: "#805AD5"
        }
      },
      series: [{
        data: []
      }],
      chart: {
        toolbar: {
          show: false
        },
        fontFamily: 'inherit',
        foreColor,
        background: 'transparent',
        height    : '100%',
        type: 'bar',
        animations: {
          enabled: false,
          animateGradually: { enabled: false },
          dynamicAnimation: { enabled: false },
        },
        events: {
          click: function(chart, w, e) {
            // console.log(chart, w, e)
          }
        }
      },
      plotOptions: {
        bar: {
          distributed: true,
        }
      },
      dataLabels: {
        enabled: false
      },
      legend: {
        show: false
      },
      yaxis: {
        max: 100,

        axisBorder: {
          show: false
        },
        axisTicks: {
          show: false,
        },
        labels: {
          show: true,
          formatter: function (val) {
            return parseInt(val.toString()) + '';
          }
        }

      },
      xaxis: {
        categories: [],
        axisBorder: {
          show: false
        },
        axisTicks: {
          show: false
        },
      },
      tooltip    : {
        enabled        : true,
        fillSeriesColor: true,
        theme          : tooltipTheme,
        custom         : (options): string => {
            const w = options.w;
            const seriesIndex = options.dataPointIndex;
            return`<div class="flex items-center h-8 min-h-8 max-h-8 px-3 z-50">
                      <div class="w-3 h-3 rounded-full" style="background-color: ${w.globals.colors[seriesIndex]};"></div>
                      <div class="ml-2 text-md leading-none">${w.config.xaxis.categories[seriesIndex]}:</div>
                      <div class="ml-2 text-md font-bold leading-none">
                          ${w.config.series[0].data[seriesIndex]}%
                          (${UtilFunctions.bytes2Size(this.healthCheckerSystemInfo.operatingSystem.fileSystem.fileStores[seriesIndex].usableSpace, true)})
                          </br>
                          Total:
                          (${UtilFunctions.bytes2Size(this.healthCheckerSystemInfo.operatingSystem.fileSystem.fileStores[seriesIndex].totalSpace, true)})
                      </div>
                  </div>`
        }
      },

    };
    this.cpuLineOptions = {
      theme: {
        mode: dark ? 'dark' : 'light',
      },
      series: [{
        name: 'CPU',
        data: this.cpuLineDataSeries,
        color: '#3182CE'
      }],
      chart: {
        id: 'realtime',
        height: '100%',
        width: '100%',
        type: 'line',
        foreColor,
        background: 'transparent',
        animations: {
          enabled: false,
          animateGradually: { enabled: false },
          dynamicAnimation: { enabled: false },
        },
        toolbar: {
          show: false
        },
        zoom: {
          enabled: false
        }
      },
      dataLabels: {
        enabled: false
      },
      stroke: {
        curve: 'smooth'
      },
      markers: {
        size: 0
      },
      xaxis: {
        type: 'datetime',
        range: 300000,
        labels: {
          show: true,
          formatter: function (value, timestamp) {
            return new Date(timestamp).toLocaleTimeString(); // The formatter function overrides format property
          },
        }
      },
      yaxis: {
        max: 100,
        labels: {
          show: true,
          formatter: function (val) {
            return parseInt(val.toString()) + '';
          }
        }
      },
      legend: {
        show: false
      },
      tooltip: {
        theme: tooltipTheme,
      },
    };
    this.memoryLineOptions = {
      theme: {
        mode: dark ? 'dark' : 'light',
      },
      series: [{
        name: 'Memória',
        data: this.memoryLineDataSeries,
        color: '#DD6B20'
      }],
      chart: {
        id: 'realtime',
        height: '100%',
        width: '100%',
        type: 'line',
        foreColor,
        background: 'transparent',
        animations: {
          enabled: false,
          animateGradually: { enabled: false },
          dynamicAnimation: { enabled: false },
        },
        toolbar: {
          show: false
        },
        zoom: {
          enabled: false
        }
      },
      dataLabels: {
        enabled: false
      },
      stroke: {
        curve: 'smooth'
      },
      markers: {
        size: 0
      },
      xaxis: {
        type: 'datetime',
        range: 300000,
        labels: {
          show: true,
          formatter: function (value, timestamp) {
            return new Date(timestamp).toLocaleTimeString(); // The formatter function overrides format property
          },
        }
      },
      yaxis: {
        max: 100,
        labels: {
          show: true,
          formatter: function (val) {
            return parseInt(val.toString()) + '';
          }
        }
      },
      legend: {
        show: false
      },
      tooltip: {
        theme: tooltipTheme,
      },
    };
  }

  updateCharts() {
    if (this.monitorOption !== 'dashboard') {
      return;
    }
    this.updateSnapshotCharts();
    if (this.realTime === true) {
      this.updateRealtimeLineCharts();
    }
  }

  /** Pizza memória/CPU, arquivos abertos e barras de disco — a cada snapshot STOMP. */
  private updateSnapshotCharts() {
    const si = this.healthCheckerSystemInfo;
    const memTotal = si.hardware?.memory?.total;
    const memAvail = si.hardware?.memory?.available;
    if (memTotal != null && memTotal > 0 && memAvail != null && this.memoryPieChart) {
      const memoryUsed = memTotal - memAvail;
      this.memoryPieOptions.series = [UtilFunctions.roundNumber(memoryUsed), UtilFunctions.roundNumber(memAvail)];
      this.memoryPieChart.updateSeries(this.memoryPieOptions.series);
    }

    const cpuLoad = si.hardware?.processor?.cpuLoad;
    if (cpuLoad != null && !isNaN(Number(cpuLoad)) && this.cpuPieChart) {
      const c = Math.min(100, Math.max(0, Number(cpuLoad)));
      this.cpuPieOptions.series = [c, 100 - c];
      this.cpuPieChart.updateSeries(this.cpuPieOptions.series);
    }

    const maxFD = si.operatingSystem?.fileSystem?.maxFileDescriptors;
    const openFD = si.operatingSystem?.fileSystem?.openFileDescriptors;
    if (maxFD != null && maxFD > 0 && openFD != null && this.openedFilesPieChart) {
      this.openedFilesPieOptions.series = [openFD, maxFD - openFD];
      this.openedFilesPieChart.updateSeries(this.openedFilesPieOptions.series);
    }

    const dataLabel = HealthCheckerSimpleSystemInfoModel.getDiskDataLabel(si);
    if (!UtilFunctions.isValidStringOrArray(dataLabel) || !this.diskUsageColumnChart) {
      return;
    }
    const pctData = dataLabel.map(x =>
      UtilFunctions.roundNumber((x.totalSize - x.availableSize) / x.totalSize * 100));
    const categories = dataLabel.map(x => x.label);
    const catKey = categories.join('|');
    const pctKey = pctData.map(x => x.toFixed(2)).join('|');

    if (this.realTime === true && !this.shouldRefreshDiskRealtime(pctData)) {
      return;
    }

    this.lastDiskPctKey = pctKey;
    this.lastDiskBarRefreshAt = Date.now();

    this.diskUsageColumnOptions.series = [{ data: pctData }];
    this.diskUsageColumnOptions.xaxis = {
      ...this.diskUsageColumnOptions.xaxis,
      categories,
    };
    if (this.realTime === true && catKey === this.lastDiskCategoriesKey) {
      this.diskUsageColumnChart.updateSeries([{ data: pctData }], false);
      this.lastDiskCategoriesKey = catKey;
      return;
    }
    this.lastDiskCategoriesKey = catKey;
    this.diskUsageColumnChart.updateOptions({
      xaxis: this.diskUsageColumnOptions.xaxis,
      series: this.diskUsageColumnOptions.series,
    });
  }

  /** Em tempo real: redesenha barras de disco só se uso mudou o suficiente ou passou o intervalo mínimo. */
  private shouldRefreshDiskRealtime(pctData: number[]): boolean {
    if (this.lastDiskPctKey === '') {
      return true;
    }
    const now = Date.now();
    const prev = this.lastDiskPctKey.split('|').map(v => parseFloat(v));
    if (prev.length !== pctData.length || prev.some(n => isNaN(n))) {
      return true;
    }
    let maxDelta = 0;
    for (let i = 0; i < pctData.length; i++) {
      maxDelta = Math.max(maxDelta, Math.abs(pctData[i] - prev[i]));
    }
    if (maxDelta >= this.diskRealtimeMinDeltaPct) {
      return true;
    }
    return now - this.lastDiskBarRefreshAt >= this.diskRealtimeMinIntervalMs;
  }

  /** Séries de CPU/memória em tempo real: só com toggle "Tempo Real" e stream do agente. */
  private updateRealtimeLineCharts() {
    const si = this.healthCheckerSystemInfo;
    const raw = si.lastUpdate;
    const now = raw != null ? new Date(raw) : new Date();
    if (isNaN(now.getTime())) {
      return;
    }
    const memTotal = si.hardware?.memory?.total;
    const memAvail = si.hardware?.memory?.available;
    const cpuLoad = si.hardware?.processor?.cpuLoad;
    if (memTotal == null || memTotal <= 0 || memAvail == null || cpuLoad == null) {
      return;
    }
    const memoryPercent = UtilFunctions.roundNumber((memTotal - memAvail) / memTotal * 100);

    if (this.cpuLineChart) {
      this.cpuLineDataSeries.push({
        x: now,
        y: UtilFunctions.roundNumber(Math.min(100, Math.max(0, Number(cpuLoad)))),
      });
      const range = this.cpuLineOptions.xaxis?.range ?? 300000;
      const old = this.cpuLineDataSeries.filter(x => now.getTime() - x.x.getTime() > range * 2);
      if (old && old.length > 0) {
        this.cpuLineDataSeries.splice(0, old.length - 1);
      }
      this.cpuLineChart.updateSeries(this.cpuLineOptions.series, false);
    }

    if (this.memoryLineChart) {
      this.memoryLineDataSeries.push({x: now, y: memoryPercent});
      const range = this.memoryLineOptions.xaxis?.range ?? 300000;
      const old = this.memoryLineDataSeries.filter(x => now.getTime() - x.x.getTime() > range * 2);
      if (old && old.length > 0) {
        this.memoryLineDataSeries.splice(0, old.length - 1);
      }
      this.memoryLineChart.updateSeries(this.memoryLineOptions.series, false);
    }
  }

  updateInfo() {
    if (UtilFunctions.isValidStringOrArray(this.destination)) {
      this.lastDiskPctKey = '';
      this.lastDiskBarRefreshAt = 0;
      this.cpuLineDataSeries.splice(0, this.cpuLineDataSeries.length - 1);
      this.memoryLineDataSeries.splice(0, this.memoryLineDataSeries.length - 1);
      this.sendMessage(HealthCheckerMessageTopicConstants.SYSTEM_INFO_REQUEST_START, false, 30000);
    }
  }

  fillConsole(m: ConsoleResponseMessageModel) {
    //this.child.
  }

  private initShellTerminal(): void {
    const el = this.xtermHost?.nativeElement;
    if (!el || this.xterm) {
      return;
    }
    this.fitAddon = new FitAddon();
    this.xterm = new Terminal({
      cursorBlink: true,
      fontFamily: 'Consolas, "Cascadia Code", "Courier New", monospace',
      fontSize: 14,
      theme: {
        background: '#1e1e1e',
        foreground: '#d4d4d4',
        cursor: '#c7c7c7',
      },
    });
    this.xterm.loadAddon(this.fitAddon);
    this.xterm.open(el);
    this.fitAddon.fit();
    this.xterm.onData((data: string) => this.onXtermData(data));
    this.applyShellMinCols();
    this.xterm.write(this.promptPrefix);
    this.xtermResizeObserver = new ResizeObserver(() => {
      try {
        this.applyShellMinCols();
      } catch {
        /* ignore */
      }
    });
    this.xtermResizeObserver.observe(el);
  }

  private disposeShellTerminal(): void {
    this.xtermResizeObserver?.disconnect();
    this.xtermResizeObserver = null;
    this.xterm?.dispose();
    this.xterm = null;
    this.fitAddon = null;
  }

  /** Largura mínima (~cmd.exe) para `dir` e tabelas não colarem; após `fit()` no host. */
  private readonly shellMinCols = 120;

  private applyShellMinCols(): void {
    if (!this.xterm || !this.fitAddon) {
      return;
    }
    try {
      this.fitAddon.fit();
      const cols = Math.max(this.shellMinCols, this.xterm.cols);
      this.xterm.resize(cols, this.xterm.rows);
    } catch {
      /* ignore */
    }
  }

  private writeShellStdout(s: string): void {
    if (!this.xterm) {
      return;
    }
    const normalized = s.replace(/\r?\n/g, '\r\n');
    if (normalized.length === 0) {
      this.xterm.write('\r\n');
      return;
    }
    const withNl =
      normalized.endsWith('\r\n') ? normalized : normalized + '\r\n';
    this.xterm.write(withNl);
  }

  private writeShellStderr(s: string): void {
    if (!this.xterm) {
      return;
    }
    const normalized = s.replace(/\r?\n/g, '\r\n');
    const withNl =
      normalized.length === 0
        ? '\r\n'
        : normalized.endsWith('\r\n')
          ? normalized
          : normalized + '\r\n';
    this.xterm.write(`\x1b[1;3;31m${withNl}\x1b[0m`);
  }

  /** Após stdout/stderr ou erro: novo prompt e estado de linha limpo. */
  private writeShellPromptAfterOutput(): void {
    if (!this.xterm) {
      return;
    }
    this.shellLine = '';
    this.shellCursor = 0;
    this.shellHistory.resetBrowse();
    this.busyStdinLine = '';
    this.xterm.write('\r\n' + this.promptPrefix);
  }

  private redrawPromptLine(): void {
    if (!this.xterm || this.waitTerminalFinish) {
      return;
    }
    const p = this.promptPrefix;
    const line = this.shellLine;
    const full = p + line;
    const pos = p.length + this.shellCursor;
    const len = full.length;
    this.xterm.write('\r\x1b[2K' + full);
    const fromEnd = len - pos;
    if (fromEnd > 0) {
      this.xterm.write(`\x1b[${fromEnd}D`);
    }
  }

  private insertPromptText(text: string): void {
    const cleaned =
      text.length <= 1 ? text : UtilFunctions.removeNonPrintable(text);
    if (!UtilFunctions.isValidStringOrArray(cleaned)) {
      return;
    }
    this.shellLine =
      this.shellLine.slice(0, this.shellCursor) + cleaned + this.shellLine.slice(this.shellCursor);
    this.shellCursor += cleaned.length;
    this.redrawPromptLine();
  }

  private backspacePrompt(): void {
    if (this.shellCursor > 0) {
      this.shellLine =
        this.shellLine.slice(0, this.shellCursor - 1) + this.shellLine.slice(this.shellCursor);
      this.shellCursor--;
      this.redrawPromptLine();
    }
  }

  private deleteForwardPrompt(): void {
    if (this.shellCursor < this.shellLine.length) {
      this.shellLine =
        this.shellLine.slice(0, this.shellCursor) + this.shellLine.slice(this.shellCursor + 1);
      this.redrawPromptLine();
    }
  }

  private submitPromptLine(): void {
    if (!this.xterm) {
      return;
    }
    const cmd = this.shellLine;
    this.xterm.write('\r\n');
    this.shellLine = '';
    this.shellCursor = 0;
    this.shellHistory.resetBrowse();
    if (cmd.trim() === 'clear') {
      this.xterm.clear();
      this.xterm.write(this.promptPrefix);
      return;
    }
    if (cmd !== '') {
      this.shellHistory.push(cmd);
    }
    this.waitTerminalFinish = true;
    this.syncConsoleMessages.reset();
    this.sendMessage(HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_REQUEST, cmd, 30000);
  }

  private onXtermData(data: string): void {
    if (!this.xterm) {
      return;
    }
    if (this.waitTerminalFinish) {
      this.onXtermDataBusy(data);
    } else {
      this.onXtermDataPrompt(data);
    }
  }

  private onXtermDataBusy(data: string): void {
    if (!this.xterm) {
      return;
    }
    let i = 0;
    while (i < data.length) {
      const code = data.charCodeAt(i);
      if (code === 3) {
        this.syncConsoleMessages.reset();
        this.sendMessage(HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_REQUEST, 'quit', 30000);
        i++;
        continue;
      }
      const c = data[i];
      if (c === '\r' || c === '\n') {
        const cmd = this.busyStdinLine;
        this.busyStdinLine = '';
        this.xterm.write('\r\n');
        this.sendMessage(HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_REQUEST, cmd, 0);
        i++;
        continue;
      }
      if (c === '\u007f' || c === '\b') {
        if (this.busyStdinLine.length > 0) {
          this.busyStdinLine = this.busyStdinLine.slice(0, -1);
          this.xterm.write('\b \b');
        }
        i++;
        continue;
      }
      if (c === '\u001b') {
        const rest = data.slice(i);
        const m = rest.match(/^\u001b\[[0-9;]*[A-Za-z]/);
        if (m) {
          i += m[0].length;
          continue;
        }
        i++;
        continue;
      }
      if (c === '\t' || c >= ' ') {
        this.busyStdinLine += c;
        this.xterm.write(this.ANSI_CYAN + c + this.ANSI_RESET);
        i++;
        continue;
      }
      i++;
    }
  }

  private onXtermDataPrompt(data: string): void {
    if (data.length > 1 && /[\r\n]/.test(data)) {
      this._messageService.open('Não cole textos que incluam quebras de linhas', 'ALERTA', 'warning');
      data = data.replace(/[\r\n]+/g, '');
    }
    let i = 0;
    while (i < data.length) {
      if (data.substr(i, 3) === '\u001b[A') {
        this.shellLine = this.shellHistory.up(this.shellLine);
        this.shellCursor = this.shellLine.length;
        this.redrawPromptLine();
        i += 3;
        continue;
      }
      if (data.substr(i, 3) === '\u001b[B') {
        this.shellLine = this.shellHistory.down(this.shellLine);
        this.shellCursor = this.shellLine.length;
        this.redrawPromptLine();
        i += 3;
        continue;
      }
      if (data.substr(i, 3) === '\u001b[C') {
        if (this.shellCursor < this.shellLine.length) {
          this.shellCursor++;
          this.redrawPromptLine();
        }
        i += 3;
        continue;
      }
      if (data.substr(i, 3) === '\u001b[D') {
        if (this.shellCursor > 0) {
          this.shellCursor--;
          this.redrawPromptLine();
        }
        i += 3;
        continue;
      }
      if (data.substr(i, 4) === '\u001b[3~') {
        this.deleteForwardPrompt();
        i += 4;
        continue;
      }
      if (data.substr(i, 3) === '\u001b[H' || data.substr(i, 4) === '\u001b[1~') {
        this.shellCursor = 0;
        this.redrawPromptLine();
        i += data.substr(i, 4) === '\u001b[1~' ? 4 : 3;
        continue;
      }
      if (data.substr(i, 3) === '\u001b[F' || data.substr(i, 4) === '\u001b[4~') {
        this.shellCursor = this.shellLine.length;
        this.redrawPromptLine();
        i += data.substr(i, 4) === '\u001b[4~' ? 4 : 3;
        continue;
      }
      if (data[i] === '\u001b') {
        const rest = data.slice(i);
        const m = rest.match(/^\u001b\[[0-9;]*[A-Za-z]/);
        if (m) {
          i += m[0].length;
          continue;
        }
        if (rest.startsWith('\u001bO') && rest.length >= 3) {
          i += 3;
          continue;
        }
        if (rest.length === 1) {
          if (this.shellLine.length > 0) {
            this.shellLine = '';
            this.shellCursor = 0;
            this.redrawPromptLine();
          }
          i++;
          continue;
        }
        i++;
        continue;
      }
      const c = data[i];
      if (c === '\r' || c === '\n') {
        this.submitPromptLine();
        i++;
        continue;
      }
      if (c === '\u007f' || c === '\b') {
        this.backspacePrompt();
        i++;
        continue;
      }
      if (c === '\t' || c >= ' ') {
        this.insertPromptText(c);
        i++;
        continue;
      }
      i++;
    }
  }
}
