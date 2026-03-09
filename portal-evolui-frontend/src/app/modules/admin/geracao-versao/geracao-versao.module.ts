import {NgModule} from '@angular/core';
import {Route, RouterModule, UrlSegment} from '@angular/router';
import {GeracaoVersaoComponent} from "./geracao-versao.component";
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
import {GeracaoVersaoTableComponent} from "./table/geracao-versao-table.component";
import {GeracaoVersaoFilterComponent} from "./filter/geracao-versao-filter.component";
import {MatDatepickerModule} from "@angular/material/datepicker";
import {MatTooltipModule} from '@angular/material/tooltip';
import {GeracaoVersaoResolver} from './geracao-versao.resolver';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {GeracaoVersaoModalComponent} from './modal/geracao-versao-modal.component';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {MatDialogModule} from '@angular/material/dialog';
import {GeracaoVersaoDiffModalComponent} from './modal/geracao-versao-diff-modal.component';
import {GeracaoVersaoGitRefModalComponent} from './modal/geracao-versao-git-ref-modal.component';
import {NgxMaskDirective, NgxMaskPipe} from 'ngx-mask';
import {MatTabsModule} from '@angular/material/tabs';

export function matcher(url: UrlSegment[]) {
  return url.length === 2 && url[0].path === 'geracao-versao' && /^[a-zA-Z]{1,}$/.test(url[1].path)
    ? { consumed: url }
    : null;
}
const geracaoVersaoRoutes: Route[] = [
  {
    matcher: matcher,
    component: GeracaoVersaoComponent,
    data: {
      roles: [PerfilUsuarioEnum.ROLE_SUPER, PerfilUsuarioEnum.ROLE_HYPER]
    },
    resolve    : {
      initialData: GeracaoVersaoResolver
    }

  },

];
@NgModule({
  declarations: [
    GeracaoVersaoComponent,
    GeracaoVersaoTableComponent,
    GeracaoVersaoFilterComponent,
    GeracaoVersaoModalComponent,
    GeracaoVersaoDiffModalComponent,
    GeracaoVersaoGitRefModalComponent
  ],
    imports: [
        RouterModule.forChild(geracaoVersaoRoutes),
        MatButtonModule,
        MatIconModule,
        MatTooltipModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatSlideToggleModule,
        MatDialogModule,
        MatSidenavModule,
        MatTableModule,
        MatDatepickerModule,
        MatSortModule,
        MatCheckboxModule,
        SharedModule,
        AdminContainerModule,
        NgxMaskDirective,
        NgxMaskPipe,
        MatTabsModule
    ]
})
export class GeracaoVersaoModule
{
}
