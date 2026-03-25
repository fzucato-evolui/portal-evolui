import {NgModule} from '@angular/core';
import {Route, RouterModule, UrlSegment} from '@angular/router';
import {AtualizacaoVersaoComponent} from "./atualizacao-versao.component";
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
import {AtualizacaoVersaoTableComponent} from "./table/atualizacao-versao-table.component";
import {AtualizacaoVersaoFilterComponent} from "./filter/atualizacao-versao-filter.component";
import {MatDatepickerModule} from "@angular/material/datepicker";
import {MatTooltipModule} from '@angular/material/tooltip';
import {AtualizacaoVersaoResolver} from './atualizacao-versao.resolver';
import {AtualizacaoVersaoModalComponent} from './modal/atualizacao-versao-modal.component';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {MatDialogModule} from '@angular/material/dialog';
import {MatCardModule} from '@angular/material/card';
import {
  NgxMatDatepickerActions,
  NgxMatDatepickerApply,
  NgxMatDatepickerCancel,
  NgxMatDatepickerClear,
  NgxMatDatepickerInput,
  NgxMatDatetimepicker,
} from '@ngxmc/datetime-picker';
import {MatNativeDateModule} from '@angular/material/core';


export function matcher(url: UrlSegment[]) {
  return url.length === 2 && url[0].path === 'atualizacao-versao' && /^[a-zA-Z]{1,}$/.test(url[1].path)
    ? { consumed: url }
    : null;
}
const geracaoVersaoRoutes: Route[] = [
  {
    matcher: matcher,
    component: AtualizacaoVersaoComponent,
    data: {
      roles: [PerfilUsuarioEnum.ROLE_ADMIN, PerfilUsuarioEnum.ROLE_SUPER, PerfilUsuarioEnum.ROLE_HYPER]
    },
    resolve    : {
      initialData: AtualizacaoVersaoResolver
    }

  },

];
@NgModule({
  declarations: [
    AtualizacaoVersaoComponent,
    AtualizacaoVersaoTableComponent,
    AtualizacaoVersaoFilterComponent,
    AtualizacaoVersaoModalComponent
  ],
  imports     : [
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
    NgxMatDatepickerActions,
    NgxMatDatepickerActions,
    NgxMatDatepickerApply,
    NgxMatDatepickerCancel,
    NgxMatDatepickerClear,
    NgxMatDatepickerInput,
    NgxMatDatetimepicker,
    MatNativeDateModule,
    MatSortModule,
    MatCardModule,
    SharedModule,
    AdminContainerModule
  ]
})
export class AtualizacaoVersaoModule
{
}
