import {NgModule} from '@angular/core';
import {Route, RouterModule, UrlSegment} from '@angular/router';
import {ClienteComponent} from "./cliente.component";
import {MatButtonModule} from "@angular/material/button";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatIconModule} from "@angular/material/icon";
import {MatInputModule} from "@angular/material/input";
import {MatSidenavModule} from "@angular/material/sidenav";
import {MatTableModule} from "@angular/material/table";
import {MatSortModule} from "@angular/material/sort";
import {PerfilUsuarioEnum} from "../../../shared/models/usuario.model";
import {SharedModule} from "../../../shared/shared.module";
import {AdminContainerModule} from "../../../shared/components/admin-container/admin-container.module";
import {ClienteTableComponent} from "./table/cliente-table.component";
import {ClienteResolver} from './cliente.resolver';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {ClienteModalComponent} from './modal/cliente-modal.component';
import {MatDialogModule} from '@angular/material/dialog';
import {MatChipsModule} from '@angular/material/chips';
import {NgxMaskDirective, NgxMaskPipe} from 'ngx-mask';

export function matcher(url: UrlSegment[]) {
  return url.length === 2 && url[0].path === 'cliente' && /^[a-zA-Z]{1,}$/.test(url[1].path)
    ? { consumed: url }
    : null;
}
const geracaoVersaoRoutes: Route[] = [
  {
    matcher: matcher,
    component: ClienteComponent,
    data: {
      roles: [PerfilUsuarioEnum.ROLE_SUPER, PerfilUsuarioEnum.ROLE_HYPER]
    },
    resolve    : {
      initialData: ClienteResolver
    }

  },

];
@NgModule({
  declarations: [
    ClienteComponent,
    ClienteTableComponent,
    ClienteModalComponent,
  ],
  imports     : [
    RouterModule.forChild(geracaoVersaoRoutes),
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatDialogModule,
    MatSidenavModule,
    MatTableModule,
    MatSortModule,
    MatCheckboxModule,
    MatChipsModule,
    NgxMaskDirective,
    NgxMaskPipe,
    SharedModule,
    AdminContainerModule
  ]
})
export class ClienteModule
{
}
