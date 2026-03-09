import {NgModule} from '@angular/core';
import {Route, RouterModule, UrlSegment} from '@angular/router';
import {MetadadosComponent} from "./metadados.component";
import {MatButtonModule} from "@angular/material/button";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatIconModule} from "@angular/material/icon";
import {MatInputModule} from "@angular/material/input";
import {MatSelectModule} from "@angular/material/select";
import {MatSidenavModule} from "@angular/material/sidenav";
import {MatTableModule} from "@angular/material/table";
import {MatSortModule} from "@angular/material/sort";
import {PerfilUsuarioEnum} from "../../../shared/models/usuario.model";
import {SharedModule} from "../../../shared/shared.module";
import {AdminContainerModule} from "../../../shared/components/admin-container/admin-container.module";
import {MetadadosTableComponent} from "./table/metadados-table.component";
import {MetadadosResolver} from './metadados.resolver';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MetadadosModalComponent} from './modal/metadados-modal.component';
import {MatDialogModule} from '@angular/material/dialog';
import {MatChipsModule} from '@angular/material/chips';
import {NgxMaskDirective, NgxMaskPipe} from 'ngx-mask';
import {MatCardModule} from '@angular/material/card';
import {MatTooltipModule} from '@angular/material/tooltip';

export function matcher(url: UrlSegment[]) {
  return url.length === 2 && url[0].path === 'metadados' && /^[a-zA-Z]{1,}$/.test(url[1].path)
    ? { consumed: url }
    : null;
}
const geracaoVersaoRoutes: Route[] = [
  {
    matcher: matcher,
    component: MetadadosComponent,
    data: {
      roles: [PerfilUsuarioEnum.ROLE_SUPER, PerfilUsuarioEnum.ROLE_HYPER]
    },
    resolve    : {
      initialData: MetadadosResolver
    }

  },

];
@NgModule({
  declarations: [
    MetadadosComponent,
    MetadadosTableComponent,
    MetadadosModalComponent,
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
    MatSelectModule,
    MatSortModule,
    MatCheckboxModule,
    MatTooltipModule,
    MatCardModule,
    MatChipsModule,
    NgxMaskDirective,
    NgxMaskPipe,
    SharedModule,
    AdminContainerModule
  ]
})
export class MetadadosModule
{
}
