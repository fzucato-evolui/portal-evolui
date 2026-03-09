import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from "@angular/core";
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
import {FunctionsUsingCSI, NgTerminal} from 'ng-terminal';
import {MatSlideToggleChange} from "@angular/material/slide-toggle";
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
  styleUrls      : ['/health-checker-monitor-modal.component.scss'],
  templateUrl    : './health-checker-monitor-modal.component.html',
  encapsulation  : ViewEncapsulation.None,

  standalone: false
})
export class HealthCheckerMonitorModalComponent implements OnInit, OnDestroy, AfterViewInit
{
  @ViewChild('term', {static: false}) terminalConsole: NgTerminal;
  readonly ANSI_RESET = "\x1B[0m";
  readonly ANSI_YELLOW = "\x1B[33m";
  readonly ANSI_CYAN = "\x1B[36m";
  readonly ANSI_RED = "\x1B[31m";
  hystoryTerminal = [];
  currentTerminalCommand = '';
  readonly promptStringTerminal = '$ ';
  readonly prompt = '\n' + FunctionsUsingCSI.cursorColumn(1) + this.promptStringTerminal;
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
  realTime: boolean;
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
  resizeTimeout: any;
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
        console.log(new Date(), msg);
      },
      connectHeaders: {Identifier: this._userService.accessToken},

    }

  }

  ngOnInit(): void {
    this.loadCharts();

  }

  ngAfterViewInit(){
    this.terminalConsole.write(this.promptStringTerminal);

    this.terminalConsole.underlying.onResize(arg => {
      const me = this;
      if (this.resizeTimeout) {
        clearTimeout(me.resizeTimeout);
      }
      this.resizeTimeout = setTimeout(() => {
        if (UtilFunctions.isValidStringOrArray(me.currentTerminalCommand) === false) {
          return;
        }
        const initialXCursor = me.terminalConsole.underlying.buffer.active.cursorX;
        const initialYCursor = me.terminalConsole.underlying.buffer.active.cursorY;
        const columnsSize = me.terminalConsole.underlying.cols;
        let currentCursor = initialYCursor;
        let currentLine = me.terminalConsole.underlying.buffer.active.getLine(currentCursor);
        let write = [];
        let count = 0;
        while (currentLine.isWrapped === true) {
          count++;
          currentLine = me.terminalConsole.underlying.buffer.active.getLine(currentCursor - count);
        }
        if (count > 0) {
          write.push(FunctionsUsingCSI.cursorPrecedingLine(count));
        }
        write.push(FunctionsUsingCSI.cursorColumn(0));
        const line = me.promptStringTerminal + me.currentTerminalCommand;
        write.push(line);
        const lines = Math.ceil(line.length / columnsSize); // Arredonda para cima
        //console.log('resize timeout', write.join(''));
        me.terminalConsole.write(write.join(''));
        write = [];
        const lastLineString = line.substring((lines-1) * columnsSize);
        const finalCursoX = lastLineString.length;
        currentCursor = finalCursoX;
        const trashRow = columnsSize - currentCursor;
        if (trashRow > 0) {
          write.push(FunctionsUsingCSI.eraseCharacters(trashRow));
        }
        currentCursor = initialYCursor - count + lines;
        currentLine = me.terminalConsole.underlying.buffer.active.getLine(currentCursor);
        count = 0;

        while (currentLine && currentLine.translateToString(true, 0, currentLine.length).trim().length > 0) {
          count ++;
          currentLine = me.terminalConsole.underlying.buffer.active.getLine(currentCursor + count);
        }
        if (count > 0) {
          write.push(FunctionsUsingCSI.cursorNextLine(1));
          write.push(FunctionsUsingCSI.deleteLines(count));
          write.push(FunctionsUsingCSI.cursorPrecedingLine(1));
          write.push(FunctionsUsingCSI.cursorColumn(finalCursoX + 1));
        }
        //console.log('resize timeout', lines, currentCursor, currentLine, trashRow, count, write.join(''), finalCursoX, lastLineString);
        me.terminalConsole.write(write.join(''));
      }, 500)

    })
    this.terminalConsole.onData().subscribe((input) => {
      return;
      //console.log(input);
      let currentLine = this.terminalConsole.underlying.buffer.active.getLine(this.terminalConsole.underlying.buffer.active.cursorY);
      let currentLineText = currentLine.translateToString(true, 0, currentLine.length).trim();
      let nextLine = this.terminalConsole.underlying.buffer.active.getLine(this.terminalConsole.underlying.buffer.active.cursorY + 1);
      let nextLineText = nextLine.translateToString(true, 0, nextLine.length).trim();
      let previousLine = this.terminalConsole.underlying.buffer.active.cursorY > 0 ? this.terminalConsole.underlying.buffer.active.getLine(this.terminalConsole.underlying.buffer.active.cursorY + 1) : null;
      let previousLineText = nextLine ? nextLine.translateToString(true, 0, nextLine.length).trim() : null;
      if (input === '\r') { // Carriage Return (When Enter is pressed)
        this.terminalConsole.write(this.prompt);
      } else if (input === '\u007f') { // Delete (When Backspace is pressed)
        if (currentLine.isWrapped === false) {
          if (this.terminalConsole.underlying.buffer.active.cursorX > 2) {
            if (this.terminalConsole.underlying.buffer.active.cursorX >= currentLineText.length) {
              this.terminalConsole.write('\b \b');
            } else {
              const init = currentLineText.slice(0, this.terminalConsole.underlying.buffer.active.cursorX - 1);
              const end = currentLineText.slice(this.terminalConsole.underlying.buffer.active.cursorX, currentLineText.length);
              this.terminalConsole.write(FunctionsUsingCSI.cursorColumn(0));
              this.terminalConsole.write(init + end);
              this.terminalConsole.write(FunctionsUsingCSI.cursorColumn(init.length + 1));
            }
          }
        } else {
          if (this.terminalConsole.underlying.buffer.active.cursorX === 0) {
            this.terminalConsole.write(FunctionsUsingCSI.cursorPrecedingLine(1));
            currentLine = this.terminalConsole.underlying.buffer.active.getLine(this.terminalConsole.underlying.buffer.active.cursorY)
            this.terminalConsole.write(FunctionsUsingCSI.cursorColumn(currentLine.length));
            this.terminalConsole.write(FunctionsUsingCSI.deleteCharacter(currentLine.length));
          } else {
            if (this.terminalConsole.underlying.buffer.active.cursorX >= currentLineText.length) {
              this.terminalConsole.write('\b \b');
            } else {
              const init = currentLineText.slice(0, this.terminalConsole.underlying.buffer.active.cursorX - 1);
              const end = currentLineText.slice(this.terminalConsole.underlying.buffer.active.cursorX, currentLineText.length);
              this.terminalConsole.write(FunctionsUsingCSI.cursorColumn(0));
              this.terminalConsole.write(init + end);
              this.terminalConsole.write(FunctionsUsingCSI.cursorColumn(init.length + 1));
            }
          }
        }
      } else if (input === '\u0003') { // End of Text (When Ctrl and C are pressed)
        this.terminalConsole.write('^C');
        this.terminalConsole.write(this.prompt);
      } else if (input === '\u001b[A' || input === '\u001b[B') { // Arrow up and down
        return;
      } else if (input === '\u001b[C') { // Arrow Right
        if (this.terminalConsole.underlying.buffer.active.cursorX >= currentLineText.length) {
          if (currentLineText.length < currentLine.length) {
            return;
          } else if (nextLine.isWrapped && nextLineText.length > 0) {
            this.terminalConsole.write(FunctionsUsingCSI.cursorNextLine(1));
            this.terminalConsole.write(FunctionsUsingCSI.cursorColumn(0));
          }
        } else {
          this.terminalConsole.write(input);
        }
      } else if (input === '\u001b[D') { // Arrow Left
        if (currentLine.isWrapped === false) {
          if (this.terminalConsole.underlying.buffer.active.cursorX > 2) {
            this.terminalConsole.write(input);
          } else {
            return;
          }
        } else {
          if (this.terminalConsole.underlying.buffer.active.cursorX === 0) {
            this.terminalConsole.write(FunctionsUsingCSI.cursorPrecedingLine(1));
            this.terminalConsole.write(FunctionsUsingCSI.cursorColumn(previousLine.length));
          } else {
            this.terminalConsole.write(input);
          }
        }

      } else if (input === '\u001b[3~') { //Delete
        return;
      } else {
        if (this.terminalConsole.underlying.buffer.active.cursorX >= currentLineText.length) {
          this.terminalConsole.write(input);
        } else {
          const init = currentLineText.slice(0, this.terminalConsole.underlying.buffer.active.cursorX);
          const end = currentLineText.slice(this.terminalConsole.underlying.buffer.active.cursorX, currentLineText.length);
          this.terminalConsole.write(FunctionsUsingCSI.cursorColumn(0));
          this.terminalConsole.write(init + input + end);
          this.terminalConsole.write(FunctionsUsingCSI.cursorColumn(init.length + input.length + 1));
        }


      }
      //this.child.write('Hello from \x1B[1;3;31mxterm.js\x1B[0m $ ');
    });

  }

  ngOnDestroy(): void {
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
            me.terminalConsole.write(me.prompt);
          } else {
            const consoleModel: ConsoleResponseMessageModel = m.message;
            const messages = me.syncConsoleMessages.addMessage(consoleModel);
            if (UtilFunctions.isValidStringOrArray(messages)) {
              messages.forEach(value => {
                if (UtilFunctions.isValidStringOrArray(value.outputError) === true) {
                  let s = value.outputError;
                  s = s.replace(/\r?\n/g, "\r\n")
                  this.terminalConsole.write(`\x1B[1;3;31m${s}\x1B[0m`);
                } else if (UtilFunctions.isValidStringOrArray(value.output) === true) {
                  let s = value.output;
                  s = s.replace(/\r?\n/g, "\r\n")
                  this.terminalConsole.write(s);
                }
                if (value.finished) {
                  this.waitTerminalFinish = false;
                  this.terminalConsole.write(this.prompt);
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
    this.rxStomp.publish({
      destination: `/app/${topic}`,
      body: JSON.stringify(model)
    });

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
      }, waitTime)
    }
  }

  changeOption(option: MonitorOptionType) {
    this.monitorOption = option;
  }

  changeRealTime(event: MatSlideToggleChange) {
    if (UtilFunctions.isValidStringOrArray(this.destination)) {
      if (event.checked === true) {
        if (this.monitorOption === 'prompt') {
          this.monitorOption = 'dashboard';
        }
        this.cpuLineDataSeries.splice(0, this.cpuLineDataSeries.length - 1);
        this.memoryLineDataSeries.splice(0, this.memoryLineDataSeries.length - 1);
        this.sendMessage(HealthCheckerMessageTopicConstants.SYSTEM_INFO_REQUEST_START, true, 30000);
      } else {
        this.sendMessage(HealthCheckerMessageTopicConstants.SYSTEM_INFO_REQUEST_STOP, null, 30000);
      }
    }
  }

  getTotalRunningOfUserProcess(): number {
    if (this.healthCheckerSystemInfo.operatingSystem) {
      const userId = this.healthCheckerSystemInfo.operatingSystem.currentProcess.userID;
      return this.healthCheckerSystemInfo.operatingSystem.processes.filter(x => x.userID === userId).length;

    }
    return 0;
  }

  getTotalRunningOfUserThreads(): number {
    if (this.healthCheckerSystemInfo.operatingSystem) {
      const userId = this.healthCheckerSystemInfo.operatingSystem.currentProcess.userID;
      const totalUser = this.healthCheckerSystemInfo.operatingSystem.processes.filter(x => x.userID === userId);
      if (UtilFunctions.isValidStringOrArray(totalUser) === true) {
        return totalUser
          .map(item => item.threadCount).reduce((prev, next) => prev + next);
      }
    }
    return 0;
  }

  getTotalRunningServices(): number {

    if (this.healthCheckerSystemInfo.operatingSystem) {
      return this.healthCheckerSystemInfo.operatingSystem.services
        .map(item => {
          if (item.state === 'RUNNING') {
            return 1;
          }
          return null;
        }).reduce((prev, next) => {
          if (next) {
            return prev + next
          } else {
            return prev;
          }
        });
    }
    return 0;
  }

  getTotalConnectedPorts(): number {
    if (this.healthCheckerSystemInfo.operatingSystem) {
      return this.healthCheckerSystemInfo.operatingSystem.internetProtocolStats.connections
        .map(item => {
          if (item.state === 'ESTABLISHED') {
            return 1;
          }
          return null;
        }).reduce((prev, next) => {
          if (next) {
            return prev + next
          } else {
            return prev;
          }
        });
    }
    return 0;

  }

  loadCharts() {
    this.memoryPieOptions = {
      chart      : {
        animations: {
          speed           : 400,
          animateGradually: {
            enabled: false,
          },
        },
        fontFamily: 'inherit',
        foreColor : 'inherit',
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
        theme          : 'dark',
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
      chart      : {
        animations: {
          speed           : 400,
          animateGradually: {
            enabled: false,
          },
        },
        fontFamily: 'inherit',
        foreColor : 'inherit',
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
        theme          : 'dark',
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
      chart      : {
        animations: {
          speed           : 400,
          animateGradually: {
            enabled: false,
          },
        },
        fontFamily: 'inherit',
        foreColor : 'inherit',
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
        theme          : 'dark',
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
        foreColor : 'inherit',
        height    : '100%',
        type: 'bar',
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
        theme          : 'dark',
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
        animations: {
          enabled: true,
          dynamicAnimation: {
            speed: 1000
          }
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
    };
    this.memoryLineOptions = {
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
        animations: {
          enabled: true,
          dynamicAnimation: {
            speed: 1000
          }
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
    };
  }

  updateCharts() {
    if (this.monitorOption !== 'dashboard') {
      return;
    }
    const now = new Date(this.healthCheckerSystemInfo.lastUpdate);
    const memoryUsed = this.healthCheckerSystemInfo.hardware.memory.total - this.healthCheckerSystemInfo.hardware.memory.available;
    const memoryAvailable = this.healthCheckerSystemInfo.hardware.memory.available;
    const memoryPercent = UtilFunctions.roundNumber(memoryUsed/ this.healthCheckerSystemInfo.hardware.memory.total * 100)
    const cpuLoad = this.healthCheckerSystemInfo.hardware.processor.cpuLoad;

    //memoryPie
    {
      this.memoryPieOptions.series = [ UtilFunctions.roundNumber(memoryUsed), UtilFunctions.roundNumber(memoryAvailable)];
      this.memoryPieChart.updateSeries(this.memoryPieOptions.series);
    }
    //cpuPie
    {
      this.cpuPieOptions.series = [ cpuLoad, 100 - cpuLoad];
      this.cpuPieChart.updateSeries(this.cpuPieOptions.series);
    }
    //openedFilesPie
    {
      this.openedFilesPieOptions.series = [ this.healthCheckerSystemInfo.operatingSystem.fileSystem.openFileDescriptors,
        this.healthCheckerSystemInfo.operatingSystem.fileSystem.maxFileDescriptors - this.healthCheckerSystemInfo.operatingSystem.fileSystem.openFileDescriptors];
      this.openedFilesPieChart.updateSeries(this.openedFilesPieOptions.series);
    }
    //diskUsageColumn
    {
      const dataLabel = HealthCheckerSimpleSystemInfoModel.getDiskDataLabel(this.healthCheckerSystemInfo);
      console.log('dataLabel', dataLabel);
      this.diskUsageColumnOptions.series = [{
        data: dataLabel.map(x => UtilFunctions.roundNumber((x.totalSize - x.availableSize)/x.totalSize * 100))
      }];
      this.diskUsageColumnOptions.xaxis.categories = dataLabel.map(x => x.label);
      this.diskUsageColumnChart.updateOptions({
        xaxis: this.diskUsageColumnChart.xaxis,
        series: this.diskUsageColumnOptions.series,
      })
    }
    //cpuLine
    {
      this.cpuLineDataSeries.push({x: now, y: UtilFunctions.roundNumber(cpuLoad)});
      const old = this.cpuLineDataSeries.filter(x => {
        const diff = now.getTime() - x.x.getTime();
        return diff > this.cpuLineOptions.xaxis.range * 2;

      });
      if (old && old.length > 0) {
        this.cpuLineDataSeries.splice(0, old.length - 1);
      }
      this.cpuLineChart.updateSeries(this.cpuLineOptions.series, false);
    }
    //memoryLine
    {

      this.memoryLineDataSeries.push({x: now, y: memoryPercent});
      const old = this.memoryLineDataSeries.filter(x => {
        const diff = now.getTime() - x.x.getTime();
        return diff > this.memoryLineOptions.xaxis.range * 2;

      });
      if (old && old.length > 0) {
        this.memoryLineDataSeries.splice(0, old.length - 1);
      }
      this.memoryLineChart.updateSeries(this.memoryLineOptions.series, false);
    }
  }

  updateInfo() {
    if (UtilFunctions.isValidStringOrArray(this.destination)) {
      this.cpuLineDataSeries.splice(0, this.cpuLineDataSeries.length - 1);
      this.memoryLineDataSeries.splice(0, this.memoryLineDataSeries.length - 1);
      this.sendMessage(HealthCheckerMessageTopicConstants.SYSTEM_INFO_REQUEST_START, false, 30000);
    }
  }

  fillConsole(m: ConsoleResponseMessageModel) {
    //this.child.
  }

  onTerminalKeyEvent(e: { key: string; domEvent: KeyboardEvent; }) {

    //console.log('keyevent', e);
    let currentY = this.terminalConsole.underlying.buffer.active.cursorY;
    let currentX = this.terminalConsole.underlying.buffer.active.cursorX;
    let currentLine = this.terminalConsole.underlying.buffer.active.getLine(currentY);
    const columnsSize = currentLine.length;
    let isPrintableKey = (e.domEvent.key.length === 1 || e.domEvent.key === 'Unidentified') && e.domEvent.ctrlKey === false && e.domEvent.altKey === false;
    if (isPrintableKey === true) {
      if (this.waitTerminalFinish === false) {
        const line = this.promptStringTerminal + this.currentTerminalCommand;

        let offsetY = 0;
        while (currentLine.isWrapped !== false || currentLine.translateToString(true, 0, columnsSize).trim().length === 0) {
          offsetY++;
          currentLine = this.terminalConsole.underlying.buffer.active.getLine(currentY - offsetY);
        }
        const zeroY = currentY - offsetY;
        const realIndex = currentX + (offsetY * columnsSize);

        if (realIndex >= line.length) {
          this.currentTerminalCommand = this.currentTerminalCommand + e.key;
          this.terminalConsole.write(e.key);
        } else {
          const init = line.substring(0, realIndex) + e.key;
          const end = line.substring(realIndex);
          this.currentTerminalCommand = init.substring(this.promptStringTerminal.length) + end;
          const realX = init.length % columnsSize;
          const realY = (zeroY) + (init.length < columnsSize ? 0 : (Math.floor(init.length / columnsSize)));
          const lines = Math.ceil((init + end).length / columnsSize);
          this.terminalConsole.write(FunctionsUsingCSI.cursorPosition(zeroY + 1, 1) +
            FunctionsUsingCSI.deleteLines(lines + 1) +
            (init + end) +
            FunctionsUsingCSI.cursorPosition(realY + 1, realX + 1));
        }
      }
      else {
        this.currentTerminalCommand = this.currentTerminalCommand + e.key;
        this.terminalConsole.write(this.ANSI_CYAN + e.key + this.ANSI_RESET);
      }
    }
    else if (e.key === '\u0003') { // End of Text (When Ctrl and C are pressed)
      if (this.waitTerminalFinish === true) {
        this.waitTerminalFinish = true;
        this.syncConsoleMessages.reset();
        this.sendMessage(HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_REQUEST, 'quit', 30000);
      }
    }
    else if (e.key === '\r') { // Carriage Return (When Enter is pressed)
      const cmd = this.currentTerminalCommand;
      this.terminalConsole.write('\n' + FunctionsUsingCSI.cursorColumn(1));
      this.currentTerminalCommand = '';
      if (this.waitTerminalFinish === false) {
        if (cmd === 'clear') {
          this.terminalConsole.underlying.reset();
          this.terminalConsole.write(this.promptStringTerminal);
        } else {
          this.waitTerminalFinish = true;
          this.syncConsoleMessages.reset();
          this.sendMessage(HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_REQUEST, cmd, 30000);
        }
      } else {
        this.sendMessage(HealthCheckerMessageTopicConstants.EXECUTE_COMMAND_REQUEST, cmd, 0);
      }

    } else if (e.domEvent.key === 'Backspace') {
      if (this.waitTerminalFinish === false) {
        const line = this.promptStringTerminal + this.currentTerminalCommand;
        if (currentLine.isWrapped === false && currentX == this.promptStringTerminal.length) {
          return;

        }
        let offsetY = 0;
        while (currentLine.isWrapped !== false || currentLine.translateToString(true, 0, columnsSize).trim().length === 0) {
          offsetY++;
          currentLine = this.terminalConsole.underlying.buffer.active.getLine(currentY - offsetY);
        }
        const zeroY = currentY - offsetY;
        const realIndex = currentX - 1 + (offsetY * columnsSize);
        const init = line.substring(0, realIndex);
        const end = line.substring(init.length + 1, line.length);
        this.currentTerminalCommand = init.substring(this.promptStringTerminal.length) + end;

        const realX = init.length % columnsSize;
        const realY = (zeroY) + (init.length < columnsSize ? 0 : (Math.floor(init.length / columnsSize)));
        const lines = Math.ceil((init + end).length / columnsSize);
        this.terminalConsole.write(FunctionsUsingCSI.cursorPosition(zeroY + 1, 1) +
          FunctionsUsingCSI.deleteLines(lines + 1) +
          (init + end) +
          FunctionsUsingCSI.cursorPosition(realY + 1, realX + 1));
      }
      else {

        if (currentX > 0) {
          this.currentTerminalCommand = this.currentTerminalCommand.substring(0, this.currentTerminalCommand.length - 1);
          this.terminalConsole.write('\b \b');
        }
        else if (UtilFunctions.isValidStringOrArray(this.currentTerminalCommand) === true) {
          this.currentTerminalCommand = this.currentTerminalCommand.substring(0, this.currentTerminalCommand.length - 1);
          this.terminalConsole.write(FunctionsUsingCSI.cursorPrecedingLine(1) +
            FunctionsUsingCSI.cursorColumn(columnsSize) + FunctionsUsingCSI.deleteCharacter(1));
        }
      }

    } else if (e.domEvent.key === 'ArrowUp' || (e.domEvent.key === 'ArrowDown')) {
      //this.terminalConsole.write(e.key);
    } else if (e.domEvent.key === 'ArrowLeft') {
      if (this.waitTerminalFinish === true) {
        return;
      }
      if (currentLine.isWrapped === false) {
        if (currentX === this.promptStringTerminal.length) {
          return;
        } else {
          this.terminalConsole.write(e.key);
        }
      } else {
        if (currentX === 0) {
          this.terminalConsole.write(FunctionsUsingCSI.cursorPrecedingLine(1) +
            FunctionsUsingCSI.cursorColumn(columnsSize));
        } else {
          this.terminalConsole.write(e.key);
        }
      }
    } else if (e.domEvent.key === 'ArrowRight') {
      if (this.waitTerminalFinish === true) {
        return;
      }
      const line = this.promptStringTerminal + this.currentTerminalCommand;
      let offsetY = 0;
      while (currentLine.isWrapped !== false || currentLine.translateToString(true, 0, columnsSize).trim().length === 0) {
        offsetY++;
        currentLine = this.terminalConsole.underlying.buffer.active.getLine(currentY - offsetY);
      }
      const realIndex = currentX + (offsetY * columnsSize);
      if (realIndex >= line.length) {
        return;
      } else if (currentX + 1 === columnsSize) {
        this.terminalConsole.write(FunctionsUsingCSI.cursorNextLine(1) +
          FunctionsUsingCSI.cursorColumn(0));
      } else {
        this.terminalConsole.write(e.key);
      }
    } else if (e.domEvent.key === 'Escape') {
      if (this.waitTerminalFinish === true) {
        return;
      }
      if (this.currentTerminalCommand.length === 0) {
        return;
      }
      let offsetY = 0;
      while (currentLine.isWrapped !== false || currentLine.translateToString(true, 0, columnsSize).trim().length === 0) {
        offsetY++;
        currentLine = this.terminalConsole.underlying.buffer.active.getLine(currentY - offsetY);
      }
      const zeroY = currentY - offsetY;
      const line = this.promptStringTerminal + this.currentTerminalCommand;
      this.currentTerminalCommand = '';
      const lines = Math.ceil(line.length / columnsSize);
      this.terminalConsole.write(FunctionsUsingCSI.cursorPosition(zeroY + 1, 1) +
        FunctionsUsingCSI.deleteLines(lines + 1) +
        this.promptStringTerminal);
    }
  }

  onTerminalBeforeInput(e: InputEvent) {

    if (!e.inputType.toLowerCase().includes('paste')) {
      return;
    }
    //console.log('before Input ', e);
    if (e.data.includes('\r') || e.data.includes('\n')) {
      this._messageService.open('Não cole textos que incluam quebras de linhas', 'ALERTA', 'warning');
      return;
    }
    const data = UtilFunctions.removeNonPrintable(e.data);
    if (UtilFunctions.isValidStringOrArray(data) === false) {
      return;
    }

    if (this.waitTerminalFinish === false) {
      let currentY = this.terminalConsole.underlying.buffer.active.cursorY;
      let currentX = this.terminalConsole.underlying.buffer.active.cursorX;
      let currentLine = this.terminalConsole.underlying.buffer.active.getLine(currentY);
      const columnsSize = currentLine.length;
      const line = this.promptStringTerminal + this.currentTerminalCommand;
      let offsetY = 0;
      while (currentLine.isWrapped !== false || currentLine.translateToString(true, 0, columnsSize).trim().length === 0) {
        offsetY++;
        currentLine = this.terminalConsole.underlying.buffer.active.getLine(currentY - offsetY);
      }
      const zeroY = currentY - offsetY;
      const realIndex = currentX + (offsetY * columnsSize);
      if (realIndex >= line.length) {
        this.currentTerminalCommand = this.currentTerminalCommand + data;
        this.terminalConsole.write(data);
      } else {
        const init = line.substring(0, realIndex) + data;
        const end = line.substring(realIndex)
        this.currentTerminalCommand = init.substring(this.promptStringTerminal.length) + end;
        const realX = init.length % columnsSize;
        const realY = (zeroY) + (init.length < columnsSize ? 0 : (Math.floor(init.length / columnsSize)));
        const lines = Math.ceil((init + end).length / columnsSize);
        this.terminalConsole.write(FunctionsUsingCSI.cursorPosition(zeroY + 1, 1) +
          FunctionsUsingCSI.deleteLines(lines + 1) +
          (init + end) +
          FunctionsUsingCSI.cursorPosition(realY + 1, realX + 1));
      }
    }
    else {
      this.currentTerminalCommand = this.currentTerminalCommand + data;
      this.terminalConsole.write(this.ANSI_CYAN + data + this.ANSI_RESET);
    }
  }
}
