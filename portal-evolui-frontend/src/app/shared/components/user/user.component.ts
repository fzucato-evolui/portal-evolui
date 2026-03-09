import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnDestroy,
  OnInit,
  ViewEncapsulation
} from '@angular/core';
import {Router} from '@angular/router';
import {BooleanInput} from '@angular/cdk/coercion';
import {Subject} from 'rxjs';
import {MatDialog} from "@angular/material/dialog";
import {UsuarioModel} from "../../models/usuario.model";
import {ClassyLayoutComponent} from "../../../layout/layouts/classy/classy.component";

@Component({
  selector       : 'user',
  templateUrl    : './user.component.html',
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,
  exportAs       : 'user',

  standalone: false
})
export class UserComponent implements OnInit, OnDestroy
{
  /* eslint-disable @typescript-eslint/naming-convention */
  static ngAcceptInputType_showAvatar: BooleanInput;
  /* eslint-enable @typescript-eslint/naming-convention */

  @Input() showAvatar: boolean = true;
  @Input() user: UsuarioModel;

  private _unsubscribeAll: Subject<any> = new Subject<any>();

  /**
   * Constructor
   */
  constructor(
    private _changeDetectorRef: ChangeDetectorRef,
    private _router: Router,
    private _parent: ClassyLayoutComponent,
    private _matDialog: MatDialog
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


  }

  /**
   * On destroy
   */
  ngOnDestroy(): void
  {
    // Unsubscribe from all subscriptions
    this._unsubscribeAll.next(undefined);
    this._unsubscribeAll.complete();
  }

  // -----------------------------------------------------------------------------------------------------
  // @ Public methods
  // -----------------------------------------------------------------------------------------------------

  /**
   * Update the user status
   *
   * @param status
   */
  updateUserStatus(status: string): void
  {
    // Return if user is not available
    if ( !this.user )
    {
      return;
    }

    /*
    // Update the user
    this._userService.update({
        ...this.user,
        status
    }).subscribe();
    */
  }

  /**
   * Sign out
   */
  signOut(): void
  {
    //this._router.navigate(['/sign-out']);
    this._parent._userService.signOut();
    this._router.navigate(['/sign-in']);
    this.user = null;
  }

  navigate(link) {
    this._router.navigateByUrl(link);
  }

  notificacoes() {
    //this._router.navigate(['/private/area-usuario', 'notificacoes']);
  }
  noImage() {
    this.user.image = null;
    this._changeDetectorRef.detectChanges();
  }
}
