import {NgModule} from '@angular/core';
import {Route, RouterModule, UrlSegment} from '@angular/router';
import {AmbienteComponent} from "./ambiente.component";
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
import {AmbienteTableComponent} from "./table/ambiente-table.component";
import {MatTooltipModule} from '@angular/material/tooltip';
import {AmbienteResolver} from './ambiente.resolver';
import {AmbienteModalComponent} from './modal/ambiente-modal.component';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {MatDialogModule} from '@angular/material/dialog';
import {MatTabsModule} from '@angular/material/tabs';
import {NgxMaskDirective, NgxMaskPipe} from 'ngx-mask';
import {MatCardModule} from '@angular/material/card';
import {MatAutocompleteModule} from '@angular/material/autocomplete';

export function matcher(url: UrlSegment[]) {
  return url.length === 2 && url[0].path === 'ambiente' && /^[a-zA-Z]{1,}$/.test(url[1].path)
    ? { consumed: url }
    : null;
}
const geracaoVersaoRoutes: Route[] = [
  {
    matcher: matcher,
    component: AmbienteComponent,
    data: {
      roles: [PerfilUsuarioEnum.ROLE_ADMIN, PerfilUsuarioEnum.ROLE_SUPER, PerfilUsuarioEnum.ROLE_HYPER]
    },
    resolve    : {
      initialData: AmbienteResolver
    }

  },

];
@NgModule({
  declarations: [
    AmbienteComponent,
    AmbienteTableComponent,
    AmbienteModalComponent
  ],
  imports     : [
    RouterModule.forChild(geracaoVersaoRoutes),
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCardModule,
    MatSlideToggleModule,
    MatDialogModule,
    MatSidenavModule,
    MatAutocompleteModule,
    MatTableModule,
    MatTabsModule,
    MatSortModule,
    NgxMaskDirective,
    NgxMaskPipe,
    SharedModule,
    AdminContainerModule
  ]
})
export class AmbienteModule
{
}
