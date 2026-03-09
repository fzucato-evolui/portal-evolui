import {NgModule} from '@angular/core';
import {Route, RouterModule, UrlSegment} from '@angular/router';
import {VersaoComponent} from "./versao.component";
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
import {VersaoTableComponent} from "./table/versao-table.component";
import {VersaoResolver} from './versao.resolver';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {VersaoModalComponent} from './modal/versao-modal.component';
import {MatDialogModule} from '@angular/material/dialog';
import {NgxMaskDirective, NgxMaskPipe} from 'ngx-mask';
import {MatCardModule} from '@angular/material/card';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {MatOptionModule} from '@angular/material/core';
import {MatSelectModule} from '@angular/material/select';

export function matcher(url: UrlSegment[]) {
  return url.length === 2 && url[0].path === 'versao' && /^[a-zA-Z]{1,}$/.test(url[1].path)
    ? { consumed: url }
    : null;
}
const geracaoVersaoRoutes: Route[] = [
  {
    matcher: matcher,
    component: VersaoComponent,
    data: {
      roles: [PerfilUsuarioEnum.ROLE_ADMIN, PerfilUsuarioEnum.ROLE_SUPER, PerfilUsuarioEnum.ROLE_HYPER]
    },
    resolve    : {
      initialData: VersaoResolver
    }

  },

];
@NgModule({
  declarations: [
    VersaoComponent,
    VersaoTableComponent,
    VersaoModalComponent,
  ],
    imports: [
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
        MatCardModule,
        MatSlideToggleModule,
        NgxMaskDirective,
        NgxMaskPipe,
        SharedModule,
        AdminContainerModule,
        MatOptionModule,
        MatSelectModule
    ]
})
export class VersaoModule
{
}
