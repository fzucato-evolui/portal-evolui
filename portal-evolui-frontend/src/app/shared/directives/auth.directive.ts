import {Directive, Input, TemplateRef, ViewContainerRef} from '@angular/core';
import {PerfilUsuarioEnum, UsuarioModel} from "../models/usuario.model";
import {UtilFunctions} from "../util/util-functions";
import {UserService} from "../services/user/user.service";
import {Subject} from "rxjs";
import {takeUntil} from "rxjs/operators";

/**
 * @whatItDoes Conditionally includes an HTML element if current user has any
 * of the authorities passed as the `expression`.
 *
 * @howToUse
 * ```
 *     <some-element *jhiHasAnyAuthority="'ROLE_ADMIN'">...</some-element>
 *
 *     <some-element *jhiHasAnyAuthority="['ROLE_ADMIN', 'ROLE_USER']">...</some-element>
 * ```
 */
export type AuthModelDirective = { auths: PerfilUsuarioEnum[], biggerThan: PerfilUsuarioEnum, biggerEqualThan: PerfilUsuarioEnum};
@Directive({
  selector: '[userAuthority]',

  standalone: false
})
export class AuthDirective {
  private authorities: AuthModelDirective;
  private user: UsuarioModel;
  protected _onDestroy = new Subject<void>();
  constructor(private _service: UserService, private templateRef: TemplateRef<any>, private viewContainerRef: ViewContainerRef) {

  }

  @Input()
  set userAuthority(value: AuthModelDirective) {
    this.authorities = value;
    this._service.user$
      .pipe((takeUntil(this._onDestroy)))
      .subscribe((user: UsuarioModel) => {
        this.user = user;
        this.updateView();
      });

  }


  private hasAnyAuthority(value: PerfilUsuarioEnum[]): boolean {
    return this._service.hasAnyAuthority(value, this.user);
  }

  private updateView(): void {
    let create = false;
    this.viewContainerRef.clear();
    if (UtilFunctions.isValidObject(this.authorities.biggerThan) === true) {
      if(UtilFunctions.valueToDouble(Object.keys(PerfilUsuarioEnum).indexOf(this.authorities.biggerThan.toString())) > UtilFunctions.valueToDouble(Object.keys(PerfilUsuarioEnum).indexOf(this.user.profile.toString()))) {
        create = true;
      } else {
        return;
      }
    }
    if (UtilFunctions.isValidObject(this.authorities.biggerEqualThan) === true) {
      if(this.authorities.biggerEqualThan.toString() === 'none' ||
        UtilFunctions.valueToDouble(Object.keys(PerfilUsuarioEnum).indexOf(this.authorities.biggerEqualThan.toString())) >= UtilFunctions.valueToDouble(Object.keys(PerfilUsuarioEnum).indexOf(this.user.profile.toString()))) {
        create = true;
      } else {
        return;
      }
    }
    if (UtilFunctions.isValidStringOrArray(this.authorities.auths) === true) {
      create = this.hasAnyAuthority(this.authorities.auths);
    }

    if (create === true) {
      this.viewContainerRef.createEmbeddedView(this.templateRef);
    }
    this._onDestroy.next();
    this._onDestroy.complete();
  }

}

