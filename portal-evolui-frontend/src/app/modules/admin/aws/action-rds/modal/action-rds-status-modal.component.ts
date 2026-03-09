import {
  AfterViewChecked,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from "@angular/core";
import {ReplaySubject, Subject} from 'rxjs';
import {MatDialogRef} from '@angular/material/dialog';
import {
  BackupRestoreRDSStatusLevelEnum,
  BackupRestoreRDSStatusModel
} from '../../../../../shared/models/action-rds.model';
import {ActionRdsService} from '../action-rds.service';
import {ActionRdsModalComponent} from './action-rds-modal.component';
import {UtilFunctions} from '../../../../../shared/util/util-functions';
import {CdkVirtualScrollViewport} from "@angular/cdk/scrolling";
import {ActionRdsComponent} from "../action-rds.component";
import {MatDrawer} from '@angular/material/sidenav';
import {NgModel} from "@angular/forms";

export class FilterStatusModel {
  sTimeStampFrom: string = null;
  timeStampFrom: Date = null;
  sTimeStampTo: string = null;
  timeStampTo: Date = null;
  tempLevel: Array<BackupRestoreRDSStatusLevelEnum> = [];
  level: Array<BackupRestoreRDSStatusLevelEnum> = [];
  tempMessage: string = null;
  message: string = null;
}
@Component({
  selector       : 'action-rds-modal',
  styleUrls      : ['/action-rds-status-modal.component.scss'],
  templateUrl    : './action-rds-status-modal.component.html',
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,


  standalone: false
})
export class ActionRdsStatusModalComponent implements OnInit, OnDestroy, AfterViewChecked
{
  @ViewChild(CdkVirtualScrollViewport) viewPort!: CdkVirtualScrollViewport;
  @ViewChild('matDrawerRight', {static: false}) sidenavRight: MatDrawer;
  errorMessage: { from?: string; to?: string } = {};

  // Constante para limitar o tamanho máximo dos logs
  private readonly MAX_LOGS = 1000;

  // Variável para controlar atualizações em lote
  private updateScheduled = false;

  // Cache para filtragem de logs
  private filterCache = new Map<string, boolean>();

  // Contador para gerar IDs sintéticos
  private logIdCounter = 0;

  get drawerMode(): 'over' | 'side' {
    return this.caller.parent.mobileQuery.matches ? 'over' : 'side';
  };
  get drawerOpened(): boolean {
    return this.caller.parent.mobileQuery.matches === false;
  };
  service: ActionRdsService;
  id: number;
  title: string;
  caller: ActionRdsComponent;
  logs: Array<BackupRestoreRDSStatusModel> = [];
  filteredLogs: Array<BackupRestoreRDSStatusModel> = [];
  filteredObjects: ReplaySubject<Array<BackupRestoreRDSStatusModel>> = new ReplaySubject<Array<BackupRestoreRDSStatusModel>>(1);
  BackupRestoreRDSStatusLevelEnum = BackupRestoreRDSStatusLevelEnum;
  private _unsubscribeAll: Subject<any> = new Subject<any>();
  filter: FilterStatusModel = new FilterStatusModel();
  private eventSource: EventSource;
  private userScrolled = false;
  private resizeObserver: ResizeObserver | null = null;
  private pendingUpdates: BackupRestoreRDSStatusModel[] = [];

  constructor(private _changeDetectorRef: ChangeDetectorRef,
              public dialogRef: MatDialogRef<ActionRdsModalComponent>)
  {
  }

  ngOnInit(): void {
    this.userScrolled = false;

    // Importante: Inicializar a lista de logs filtrados para evitar array vazio
    this.filteredObjects.next(this.filteredLogs);

    // Inscrever-se no observable de logs filtrados para atualizar a view
    this.filteredObjects
      .subscribe(() => {
        // Forçar detecção de mudanças após atualização dos logs
        this._changeDetectorRef.detectChanges();

        // Forçar o virtual scroll a atualizar seu layout
        setTimeout(() => {
          if (this.viewPort) {
            this.viewPort.checkViewportSize();
            this.viewPort.scrollToOffset(this.viewPort.measureScrollOffset());
          }

          if (!this.userScrolled) {
            requestAnimationFrame(() => {
              this.scrollToBottom();
            });
          }
        });
      });
    
    // Forçar uma verificação inicial após um pequeno delay para garantir que o viewport esteja pronto
    setTimeout(() => {
      this._changeDetectorRef.detectChanges();
      if (this.viewPort) {
        this.viewPort.checkViewportSize();
      }
    }, 50);

    this.eventSource = this.service.status(this.id);

    // Variável para controlar se é a primeira mensagem
    let isFirstMessage = true;
    
    this.eventSource.onmessage = (event) => {
      const data: BackupRestoreRDSStatusModel = JSON.parse(event.data);

      // Ignorar mensagens de heartbeat
      if (UtilFunctions.parseBoolean(data.heartbeat) === true) {
        return;
      }

      // Adicionar um ID sintético ao log
      (data as any).syntheticId = ++this.logIdCounter;

      // Adicionar ao array de atualizações pendentes
      this.pendingUpdates.push(data);
      
      // Se for a primeira mensagem, processar imediatamente para garantir que a tela seja atualizada
      if (isFirstMessage) {
        isFirstMessage = false;
        this.processPendingUpdates();
      } else {
        // Processar em lotes para melhorar a performance
        if (!this.updateScheduled) {
          this.updateScheduled = true;
          setTimeout(() => {
            this.processPendingUpdates();
          }, 100);
        }
      }

      // Tratar finalização do processamento
      if (data.finished) {
        setTimeout(() => {
          if (!this.userScrolled) {
            this.scrollToBottom();
          }
          this.eventSource?.close();
          this.eventSource = null;
          this._changeDetectorRef.detectChanges();
        }, 2000);
      }
    };

    this.eventSource.onerror = (error) => {
      console.warn('SSE desconectado. Tentando reconectar...', error);
      this.eventSource?.close();
      this.eventSource = null;
      this._changeDetectorRef.detectChanges();
    };

    // Configurar observer de redimensionamento
    this.resizeObserver = new ResizeObserver(() => {
      if (!this.userScrolled) {
        requestAnimationFrame(() => this.scrollToBottom());
      }
    });

    setTimeout(() => {
      if (this.viewPort) {
        const element = this.viewPort.elementRef.nativeElement;
        this.resizeObserver?.observe(element);
      }
    }, 100);
  }

  ngOnDestroy(): void {
    this._unsubscribeAll.next(null);
    this._unsubscribeAll.complete();
    if (this.resizeObserver) {
      this.resizeObserver.disconnect();
      this.resizeObserver = null;
    }

    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }

    // Limpar o cache ao destruir
    this.filterCache.clear();
  }

  ngAfterViewChecked() {
    if (!this.viewPort) return;

    // Verificar o tamanho do viewport após cada verificação
    this.viewPort.checkViewportSize();

    // Rolar para o fundo se o usuário não tiver rolado manualmente
    if (!this.userScrolled && this.filteredLogs.length > 0) {
      // Usar setTimeout para evitar ExpressionChangedAfterItHasBeenCheckedError
      setTimeout(() => {
        this.scrollToBottom();
      });
    }
  }

  private processPendingUpdates(): void {
    // Processamento em lote
    this.logs.push(...this.pendingUpdates);

    // Controlar o tamanho máximo de logs em memória
    if (this.logs.length > this.MAX_LOGS) {
      this.logs = this.logs.slice(-this.MAX_LOGS);
    }

    // Filtrar apenas os logs que atendem aos critérios
    const newFilteredLogs = this.pendingUpdates.filter(log => this.filterData(log));

    if (newFilteredLogs.length > 0) {
      this.filteredLogs.push(...newFilteredLogs);

      // Controlar o tamanho máximo de logs filtrados
      if (this.filteredLogs.length > this.MAX_LOGS) {
        this.filteredLogs = this.filteredLogs.slice(-this.MAX_LOGS);
      }

      // Emitir um novo array (importante para OnPush) para forçar a atualização
      this.filteredObjects.next([...this.filteredLogs]);
      
      // Forçar detecção de mudanças imediatamente
      this._changeDetectorRef.detectChanges();
      
      // Forçar atualização do viewport
      setTimeout(() => {
        if (this.viewPort) {
          this.viewPort.checkViewportSize();
        }
      }, 0);
    }

    // Limpar atualizações pendentes e desmarcar flag de agendamento
    this.pendingUpdates.length = 0;
    this.updateScheduled = false;
  }

  filterLogs() {
    this.convertToDate('from', this.filter.sTimeStampFrom, null);
    this.convertToDate('to', this.filter.sTimeStampTo, null);
    this.filter.level = this.filter.tempLevel;
    this.filter.message = this.filter.tempMessage;

    // Limpar cache quando os filtros mudarem
    this.filterCache.clear();

    this.filteredLogs = this.logs.filter((log: BackupRestoreRDSStatusModel) => {
      return this.filterData(log);
    });

    // Limitar o tamanho dos logs filtrados
    if (this.filteredLogs.length > this.MAX_LOGS) {
      this.filteredLogs = this.filteredLogs.slice(-this.MAX_LOGS);
    }
    this.filteredObjects.next(this.filteredLogs);
    this._changeDetectorRef.detectChanges();

    // Scroll para o início dos resultados filtrados
    if (this.filteredLogs.length > 0) {
      this.scrollToTop();
    }
  }

  filterData(data: BackupRestoreRDSStatusModel): boolean {
    // Criar uma chave de cache única baseada nos parâmetros de filtro e dados
    const cacheKey = JSON.stringify({
      id: (data as any).syntheticId,
      filters: {
        level: this.filter.level,
        from: this.filter.timeStampFrom?.getTime(),
        to: this.filter.timeStampTo?.getTime(),
        msg: this.filter.message
      }
    });

    // Verificar se o resultado já está em cache
    if (this.filterCache.has(cacheKey)) {
      return this.filterCache.get(cacheKey);
    }

    const timestamp = new Date(data.timeStamp);
    const result = (this.filter.level.length === 0 || this.filter.level.includes(data.logLevel)) &&
      (this.filter.timeStampFrom === null || timestamp >= this.filter.timeStampFrom) &&
      (this.filter.timeStampTo === null || timestamp <= this.filter.timeStampTo) &&
      (this.filter.message === null || UtilFunctions.removeAccents(data.message).toLowerCase()
        .includes(UtilFunctions.removeAccents(this.filter.message).toLowerCase()));

    // Limitar o tamanho do cache
    if (this.filterCache.size > 1000) {
      this.filterCache.clear();
    }

    this.filterCache.set(cacheKey, result);
    return result;
  }

  limparLogs() {
    this.logs = [];
    this.filteredLogs = [];
    this.filterCache.clear();
    this.filteredObjects.next(this.filteredLogs);
    this._changeDetectorRef.detectChanges();
  }

  scrollToBottom(): void {
    if (!this.viewPort) return;

    try {
      // Rolar para o último item na lista virtual
      if (this.filteredLogs.length > 0) {
        const lastIndex = this.filteredLogs.length - 1;
        this.viewPort.scrollToIndex(lastIndex);
      }
    } catch (err) {
      console.error('Erro ao tentar rolar para o fundo:', err);
    }
  }

  scrollToTop(): void {
    if (!this.viewPort) return;

    try {
      // Rolar para o primeiro item
      this.viewPort.scrollToIndex(0);
    } catch (err) {
      console.error('Erro ao tentar rolar para o topo:', err);
    }
  }

  onUserScroll(): void {
    if (!this.viewPort) return;

    // Obter informações sobre a posição de scroll atual
    const element = this.viewPort.elementRef.nativeElement;
    const scrollTop = element.scrollTop;
    const scrollHeight = element.scrollHeight;
    const clientHeight = element.clientHeight;

    // Calcular a distância do fundo em pixels
    const distanceFromBottom = scrollHeight - scrollTop - clientHeight;

    // Se estiver a mais de 100px do fundo, consideramos como scroll manual
    if (distanceFromBottom > 100) {
      this.userScrolled = true;
    }
    // Se o usuário rolou até próximo do fundo (menos de 100px), reativamos o scroll automático
    else {
      this.userScrolled = false;
    }
  }

  toggleMatRight() {
    this.sidenavRight.toggle();
  }

  limpar() {
    this.filter = new FilterStatusModel();
    this.filterCache.clear();
    this.filterLogs();
  }

  convertToDate(field: 'from' | 'to', value: string, ngModelRef: NgModel) {
    if (UtilFunctions.isValidStringOrArray(value) === false) {

      if (ngModelRef) {
        this.errorMessage[field] = null;
        ngModelRef.control.setErrors(null);
      }
      else {
        this.filter[field === 'from' ? 'timeStampFrom' : 'timeStampTo'] = null;
      }
      return;
    }
    const dateParts = value.match(/(\d+)/g); // Extrai números
    if (dateParts && dateParts.length === 7) {
      const [day, month, year, hour, minute, second, millis] = dateParts.map(Number);
      // ✅ Validações de data
      if (
        month < 1 || month > 12 ||  // Mês inválido
        day < 1 || day > 31 ||      // Dia inválido
        hour < 0 || hour > 23 ||    // Hora inválida
        minute < 0 || minute > 59 ||// Minuto inválido
        second < 0 || second > 59 ||// Segundo inválido
        millis < 0 || millis > 999  // Milissegundo inválido
      ) {
        if (ngModelRef) {
          this.errorMessage[field] = "Data/Hora inválida!";
          ngModelRef.control.setErrors({invalidDate: true});
        }
        else {
          this.setInvalidDate(field);
        }
        return;
      }

      // ✅ Criar data se válida
      const date = new Date(year, month - 1, day, hour, minute, second, millis);
      if (ngModelRef) {
        this.errorMessage[field] = ""; // Limpa erro
        ngModelRef.control.setErrors(null);
      }
      else {
        if (field === 'from') {
          this.filter.timeStampFrom = date;
        } else {
          this.filter.timeStampTo = date;
        }
      }

    } else {
      if (ngModelRef) {
        this.errorMessage[field] = "Formato inválido!";
        ngModelRef.control.setErrors({invalidDate: true});
      }
      else {
        this.setInvalidDate(field);
      }
    }
  }

  setInvalidDate(field: 'from' | 'to') {
    if (field === 'from') {
      this.filter.timeStampFrom = null;
    } else {
      this.filter.timeStampTo = null;
    }
  }
}
