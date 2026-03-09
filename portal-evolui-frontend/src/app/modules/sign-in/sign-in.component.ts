import {AfterViewInit, Component, ElementRef, NgZone, OnInit, ViewChild, ViewEncapsulation} from '@angular/core';
import {FormBuilder, FormGroup, NgForm, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {UserService} from "../../shared/services/user/user.service";
import {TipoUsuarioEnum, UsuarioModel} from "../../shared/models/usuario.model";
import {UtilFunctions} from "../../shared/util/util-functions";
import {takeUntil} from "rxjs/operators";
import {Subject} from "rxjs";
import {jwtDecode} from "jwt-decode";


@Component({
    selector     : 'auth-sign-in',
    templateUrl  : './sign-in.component.html',
    encapsulation: ViewEncapsulation.None,

    standalone: false
})
export class AuthSignInComponent implements OnInit, AfterViewInit
{
  @ViewChild('signInNgForm') signInNgForm: NgForm;
  @ViewChild('gbutton') gbutton: ElementRef = new ElementRef({});
  private _unsubscribeAll: Subject<any> = new Subject<any>();
    signInForm: FormGroup;
    showAlert: boolean = false;
    enableSocialMedia = false;
    clientID: string;
    /**
     * Constructor
     */
    constructor(
        private _activatedRoute: ActivatedRoute,
        private _formBuilder: FormBuilder,
        private _router: Router,
        private _userService: UserService,
        private _ngZone: NgZone
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
        this._userService.rootService.googleConfig$
          .pipe(takeUntil(this._unsubscribeAll))
          .subscribe(value => {
              this.enableSocialMedia = value && UtilFunctions.isValidStringOrArray(value.clientID);
              this.clientID = value.clientID;
            })
        // Create the form
        this.signInForm = this._formBuilder.group({
            login     : ['', [Validators.required]],
            password  : ['', Validators.required]
        });



    }

    // -----------------------------------------------------------------------------------------------------
    // @ Public methods
    // -----------------------------------------------------------------------------------------------------

    /**
     * Sign in
     */
    signIn(): void
    {
      // Return if the form is invalid
      if ( this.signInForm.invalid )
      {
        return;
      }

      // Disable the form
      this.signInForm.disable();

      const user = new UsuarioModel();
      user.login = this.signInForm.value['login'];
      user.password = this.signInForm.value['password'];
      user.userType = TipoUsuarioEnum.CUSTOM;
      this.doLogin(user);

    }

  doLogin(user: UsuarioModel) {

    this._userService.signIn(user)
      .subscribe(
        (response) => {
          const redirectURL = this._activatedRoute.snapshot.queryParamMap.get('redirectURL');

          // Navigate to the redirect url
          if (UtilFunctions.isValidStringOrArray(redirectURL) === true) {
            this._router.navigateByUrl(redirectURL);
          } else {
            this._router.navigate(['']);
          }
          this.signInForm.enable();

        },
        (response) => {

          // Re-enable the form
          this.signInForm.enable();

        }
      );
  }

  ngAfterViewInit(): void {
      if (this.enableSocialMedia === true) {

        // @ts-ignore
        window.google.accounts.id.initialize({
          client_id: this.clientID,
          itp_support: true,
          callback: this.handleCallback.bind(this),
        });
        // @ts-ignore
        window.google.accounts.id.renderButton(this.gbutton.nativeElement, {
          type: 'standard',
          theme: 'outline',
          size: 'medium',
          width: 50,
          shape: 'pill',
        });
        // @ts-ignore
        google.accounts.id.prompt();
      }
  }
  // @ts-ignore
  handleCallback(response: google.accounts.id.CredentialResponse) {
    const responsePayload = jwtDecode(response.credential);
    const user = new UsuarioModel();
    user.login = responsePayload['sub'];
    user.password = response.credential;
    user.email = responsePayload['email'];
    user.image = responsePayload['picture'];
    user.name = responsePayload['name'];
    user.userType = TipoUsuarioEnum.GOOGLE;
    const me = this;
    setTimeout(function () {
      me._ngZone.run(()=> {
        me.doLogin(user);
      });
    }, 100);
  }


}
