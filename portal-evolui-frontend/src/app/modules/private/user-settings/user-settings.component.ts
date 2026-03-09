import {ChangeDetectorRef, Component, OnDestroy, OnInit, ViewEncapsulation} from '@angular/core';
import {ClassyLayoutComponent} from "../../../layout/layouts/classy/classy.component";
import {ConfigModelEnum} from "../../../shared/models/config.model";
import {Subject} from "rxjs";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {UserConfigModel, UsuarioModel} from "../../../shared/models/usuario.model";
import {cloneDeep} from "lodash-es";
import {MessageDialogService} from "../../../shared/services/message/message-dialog-service";
import {UsersSettingsService} from "./users-settings.service";
import {ThemeService} from "../../../shared/services/theme/theme.service";

export interface ThemeColor {
  name: string;
  label: string;
  bg: string;
}

@Component({
    selector     : 'user-settings',
    templateUrl  : './user-settings.component.html',
    encapsulation: ViewEncapsulation.None,
    styleUrls : ['./user-settings.component.scss'],

    standalone: false
})
export class UserSettingsComponent implements OnInit, OnDestroy
{
  get drawerMode(): 'over' | 'side' {
    return this._parent.mobileQuery.matches ? 'over' : 'side';
  };
  get drawerOpened(): boolean {
    return this._parent.mobileQuery.matches === false;
  };
  panels: any[] = [];
  selectedPanel: 'profile' | 'password' | 'appearance' = 'profile';
  ConfigModelEnum = ConfigModelEnum;
  private _unsubscribeAll: Subject<any> = new Subject<any>();

  accountForm: FormGroup;
  passwordForm: FormGroup;
  model: UsuarioModel;

  // Aparência
  selectedScheme: string = 'light';
  selectedTheme: string = 'default';
  themeColors: ThemeColor[] = [
    {name: 'default', label: 'Indigo', bg: '#4f46e5'},
    {name: 'blue', label: 'Blue', bg: '#2563eb'},
    {name: 'teal', label: 'Teal', bg: '#0d9488'},
    {name: 'green', label: 'Green', bg: '#16a34a'},
    {name: 'purple', label: 'Purple', bg: '#9333ea'},
    {name: 'red', label: 'Red', bg: '#dc2626'},
    {name: 'orange', label: 'Orange', bg: '#ea580c'},
    {name: 'amber', label: 'Amber', bg: '#d97706'},
    {name: 'pink', label: 'Pink', bg: '#db2777'},
    {name: 'wine', label: 'Wine', bg: '#950d13'},
  ];

    constructor(
        private _parent: ClassyLayoutComponent,
        private _formBuilder: FormBuilder,
        private _changeDetectorRef: ChangeDetectorRef,
        private _messageService: MessageDialogService,
        private _service: UsersSettingsService,
        private _themeService: ThemeService
    )
    {
      this.model = cloneDeep(this._parent.user);
    }

  ngOnInit(): void {
    // Setup available panels
    this.panels = [
      {
        id         : 'profile',
        icon       : {fontSet:"fas", fontIcon:"fa-user"},
        title      : 'Perfil'
      },
      {
        id         : 'password',
        icon       : {fontSet:"fas", fontIcon:"fa-lock"},
        title      : 'Senha'
      },
      {
        id         : 'appearance',
        icon       : {fontSet:"fas", fontIcon:"fa-palette"},
        title      : 'Aparência'
      }
    ];

    // Carregar config atual
    if (this.model.config) {
      this.selectedScheme = this.model.config.scheme || 'light';
      this.selectedTheme = this.model.config.theme || 'default';
    }

    this.accountForm = this._formBuilder.group({
      base64Image: [this.model.image],
      id: [this.model.id],
      name: [this.model.name, [Validators.required]],
      login: [this.model.login, [Validators.required]],
      email: [this.model.email, [Validators.email]],
    });

    this.passwordForm = this._formBuilder.group({
      id: [this.model.id],
      password: [, [Validators.required]],
      confirmPassword: [, [Validators.required]],
      newPassword: [, [Validators.required]]

    },
      {validator: this.passwordMatchValidator}
    );

  }

  ngOnDestroy(): void {
    // Unsubscribe from all subscriptions
    this._unsubscribeAll.next(undefined);
    this._unsubscribeAll.complete();
  }

  goToPanel(panel: any): void
  {
    this.selectedPanel = panel;

  }

  /**
   * Get the details of the panel
   *
   * @param id
   */
  getPanelInfo(id: string): any
  {
    return this.panels.find(panel => panel.id === id);
  }

  /**
   * Track by function for ngFor loops
   *
   * @param index
   * @param item
   */
  trackByFn(index: number, item: any): any
  {
    return item.id || index;
  }

  clearImage() {
    this.model.image = null;
  }

  handleUpload(event) {
    const file = event.target.files[0];
    const reader = new FileReader();
    reader.readAsDataURL(file);
    const me = this;
    reader.onload = () => {
      me.model.image = reader.result.toString();
      me.accountForm.get('base64Image').setValue(me.model.image);
      me._changeDetectorRef.markForCheck();
    };
  }

  private showSucess(message) {

    this._messageService.open(message, 'SUCESSO', 'success');

  }

  public canSave(): boolean {
    return this.accountForm.invalid !== true;
  }

  public saveProfile() {
    const model = this.accountForm.value;
    this._service.save(model)
      .then(value => {
        this.model = value.user;
        const updatedUser = cloneDeep(value.user);
        this._parent._userService.user = updatedUser;
        this._parent._userService.rootService.user = updatedUser;
        this._parent._userService.accessToken = value.accessToken;
        this.showSucess("Dados salvos com sucesso");
        this._changeDetectorRef.markForCheck();
      })
  }

  savePassword() {
    const model = this.passwordForm.value;
    this._service.changePassword(model)
      .then(value => {
        this.showSucess("Senha alterada com sucesso");
      })
  }

  passwordMatchValidator(frm: FormGroup) {
    if (frm.controls['newPassword'].value === frm.controls['confirmPassword'].value) {

    } else {
      if (!frm.controls['confirmPassword'].errors) {
        frm.controls['confirmPassword'].setErrors({'mismatch': true});
      }
      return {'mismatch': true};
    }
    return null;
  }

  // ─── Aparência ──────────────────────────────────────────────────────────

  selectScheme(scheme: string): void {
    this.selectedScheme = scheme;
    this._themeService.applyScheme(scheme);
  }

  selectTheme(theme: string): void {
    this.selectedTheme = theme;
    this._themeService.applyTheme(theme);
  }

  saveAppearance(): void {
    const config: UserConfigModel = {
      theme: this.selectedTheme,
      scheme: this.selectedScheme,
      lang: this.model.config?.lang
    };
    this._service.saveConfig(config)
      .then(user => {
        this.model.config = config;
        const updatedUser = cloneDeep(this.model);
        this._parent._userService.user = updatedUser;
        this._parent._userService.rootService.user = updatedUser;
        this._themeService.apply(config);
        this._messageService.open('Aparência salva com sucesso', 'SUCESSO', 'success');
        this._changeDetectorRef.markForCheck();
      });
  }
}
