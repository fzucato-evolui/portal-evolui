import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewEncapsulation
} from '@angular/core';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {Subject} from "rxjs";
import {parseInt} from "lodash-es";
import {Router} from "@angular/router";
import {PerfilUsuarioEnum, UsuarioModel} from "../../../../../shared/models/usuario.model";
import {UsersEditService} from "../users-edit.service";
import {MessageDialogService} from "../../../../../shared/services/message/message-dialog-service";


@Component({
    selector       : 'users-edit-form',
    templateUrl    : './users-edit-form.component.html',
    styleUrls      : ['./users-edit-form.component.scss'],
    encapsulation  : ViewEncapsulation.None,
    changeDetection: ChangeDetectionStrategy.OnPush,

    standalone: false
})
export class UsersEditFormComponent implements OnInit, OnDestroy {

    accountForm: FormGroup;
    @Input()
    model: UsuarioModel;
    @Input()
    showSaveButton: boolean = true;

    @Output()
    onSave: EventEmitter<UsuarioModel> = new EventEmitter<UsuarioModel>();

    PerfilUsuarioEnum = PerfilUsuarioEnum;

    /** Subject that emits when the component has been destroyed. */
    protected _onDestroy = new Subject<void>();
    /**
     * Constructor
     */
    constructor(
        private _formBuilder: FormBuilder,
        private _service: UsersEditService,
        private _changeDetectorRef: ChangeDetectorRef,
        private _messageService: MessageDialogService,
        private _router: Router
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
      if (this.model && this.model.id > 0) {
        // Create the form
        this.accountForm = this._formBuilder.group({
          base64Image: [this.model.image],
          id: [this.model.id],
          name: [this.model.name, [Validators.required]],
          login: [this.model.login, [Validators.required]],
          password: [''],
          email: [this.model.email, [Validators.email]],
          profile: [this.model.profile, [Validators.required]],
          enabled: [this.model.enabled]

        });
      } else {
        this.model = new UsuarioModel();
        // Create the form
        this.accountForm = this._formBuilder.group({
          base64Image: [''],
          id: [this.model.id],
          name: ['', [Validators.required]],
          login: ['', [Validators.required]],
          password: [''],
          email: ['', [Validators.email]],
          profile: ['', [Validators.required]],
          enabled: [true],
        });
      }
      /*
        this._service.model$.subscribe(value => {
            this.model = value;

            if (this.model.id > 0) {
                // Create the form
                this.accountForm = this._formBuilder.group({
                    base64Image: [this.model.image],
                    id: [this.model.id],
                    name: [this.model.name, [Validators.required]],
                    login: [this.model.login, [Validators.required]],
                    password: [''],
                    email: [this.model.email, [Validators.email]],
                    profile: [this.model.profile, [Validators.required]],
                    enabled: [this.model.enabled]

                });
            } else {
                // Create the form
                this.accountForm = this._formBuilder.group({
                    base64Image: [''],
                    id: [this.model.id],
                    name: ['', [Validators.required]],
                    login: ['', [Validators.required]],
                    password: [''],
                    email: ['', [Validators.email]],
                    profile: ['', [Validators.required]],
                    enabled: [true],
                });
            }


        });
        */


    }



    ngOnDestroy(): void {
        this._onDestroy.next();
        this._onDestroy.complete();
    }


    salvar() {
        this._service.save(this.accountForm.value)
            .then(value=> {
                this.accountForm.get('id').setValue(value.id);
                this.showSucess("Usuário salvo com sucesso", value.id);
                this.onSave.emit(value);
            }).catch(error => {
                console.error(error);
        })
    }

    clearImage() {
        this.model.image = 'assets/images/noPicture.png';
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

    private showSucess(message, id) {

        this._messageService.open(message, 'SUCESSO', 'success').subscribe((result) => {
            if (!this.model.id || parseInt(this.model.id.toString()) <= 0) {
                this._router.navigateByUrl('/admin/user/' + id);
            }
        });

    }

    public canSave(): boolean {
      return this.accountForm.invalid !== true;
    }
}
