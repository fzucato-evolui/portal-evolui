import {Component, OnDestroy, OnInit, ViewEncapsulation} from '@angular/core';
import {ClassyLayoutComponent} from "../../../layout/layouts/classy/classy.component";
import {ConfigInitialDataType, ConfigSystemService} from "./config-system.service";
import {SystemConfigModel, SystemConfigModelEnum} from "../../../shared/models/system-config.model";
import {takeUntil} from "rxjs/operators";
import {Subject} from "rxjs";
import {ProjectModel} from '../../../shared/models/project.model';


@Component({
  selector     : 'config-system.component',
  templateUrl  : './config-system.component.html',
  styleUrls      : ['./config-system.component.scss'],
  encapsulation: ViewEncapsulation.None,

  standalone: false
})
export class ConfigSystemComponent implements OnInit, OnDestroy
{
  get drawerMode(): 'over' | 'side' {
    return this._parent.mobileQuery.matches ? 'over' : 'side';
  };
  get drawerOpened(): boolean {
    return this._parent.mobileQuery.matches === false;
  };

  panels: any[] = [];
  selectedPanel: SystemConfigModelEnum = SystemConfigModelEnum.AX;
  SystemConfigModelEnum = SystemConfigModelEnum;
  public model: Array<SystemConfigModel>;
  public initialData: ConfigInitialDataType;
  private _unsubscribeAll: Subject<any> = new Subject<any>();

  public get produtos(): Array<ProjectModel> {
    return  this._parent.projects;
  }
  constructor(
    private _parent: ClassyLayoutComponent,
    public service: ConfigSystemService
  )
  {
  }

  // -----------------------------------------------------------------------------------------------------
  // @ Lifecycle hooks
  // -----------------------------------------------------------------------------------------------------

  /**
   * On init
   */
  ngOnInit(): void
  {
    this.panels = [
      {
        id         : SystemConfigModelEnum.AX,
        icon       : 'custom:ax',
        title      : 'AX',
        description: "Configurações do AX"
      },
      {
        id         : SystemConfigModelEnum.PORTAL_LUTHIER,
        icon       : 'custom:evolui',
        title      : 'Portal Luthier',
        description: "Configurações do Portal Luthier"
      },
      {
        id         : SystemConfigModelEnum.GOOGLE,
        icon       : {fontSet:"fab", fontIcon:"fa-google"},
        title      : 'Google',
        description: "Autenticação Google"
      },
      {
        id         : SystemConfigModelEnum.AWS,
        icon       : {fontSet:"fab", fontIcon:"fa-aws"},
        title      : 'AWS',
        description: "Dados AWS"
      },
      {
        id         : SystemConfigModelEnum.GITHUB,
        icon       : {fontSet:"fab", fontIcon:"fa-github"},
        title      : 'Github',
        description: "Dados Github"
      },
      {
        id         : SystemConfigModelEnum.SMTP,
        icon       : {fontSet:"fas", fontIcon:"fa-at"},
        title      : 'SMTP',
        description: "Servidor SMTP"
      },
      {
        id         : SystemConfigModelEnum.MONDAY,
        icon       : 'custom:monday',
        title      : 'Monday',
        description: "Configurações do Monday"
      },
      {
        id         : SystemConfigModelEnum.NOTIFICATION,
        icon       : {fontSet:"fas", fontIcon:"fa-bell"},
        title      : 'Notificações',
        description: "Configurações das notificações"
      },
      // {
      //   id         : SystemConfigModelEnum.ACTIONS,
      //   icon       : {fontSet:"fas", fontIcon:"fa-bolt"},
      //   title      : 'Ações',
      //   description: "Configurações de ações por gatilho"
      // },
      {
        id         : SystemConfigModelEnum.CICD,
        icon       : {fontSet:"fas", fontIcon:"fa-flask-vial"},
        title      : 'CI/CD',
        description: "Configurações das integrações contínuas dos produtos"
      }
    ];
    this.service.model$
      .pipe((takeUntil(this._unsubscribeAll)))
      .subscribe((config: ConfigInitialDataType) => {
        this.model = config.configs;
        this.initialData = config;
      });


  }
  ngOnDestroy(): void {
    // Unsubscribe from all subscriptions
    this._unsubscribeAll.next(undefined);
    this._unsubscribeAll.complete();
  }

  trackByFn(index: number, item: any): any
  {
    return item.id || index;
  }

  goToPanel(panel: SystemConfigModelEnum): void
  {
    this.selectedPanel = panel;

  }

  getPanelInfo(id: string): any
  {
    return this.panels.find(panel => panel.id === id);
  }

  getConfig(configType: SystemConfigModelEnum) {
    if (this.model) {
      const index = this.model.findIndex(x => x.configType === configType);
      if (index >= 0) {
        return this.model[index];
      }
    }
    return null;
  }

}
