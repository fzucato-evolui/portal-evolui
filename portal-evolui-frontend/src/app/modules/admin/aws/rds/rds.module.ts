import {NgModule} from '@angular/core';
import {Route, RouterModule} from '@angular/router';
import {RdsComponent} from "./rds.component";
import {MatButtonModule} from "@angular/material/button";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatIconModule} from "@angular/material/icon";
import {MatInputModule} from "@angular/material/input";
import {MatSelectModule} from "@angular/material/select";
import {MatSidenavModule} from "@angular/material/sidenav";
import {MatTableModule} from "@angular/material/table";
import {MatSortModule} from "@angular/material/sort";
import {RdsService} from "./rds.service";
import {PerfilUsuarioEnum} from "../../../../shared/models/usuario.model";
import {SharedModule} from "../../../../shared/shared.module";
import {AdminContainerModule} from "../../../../shared/components/admin-container/admin-container.module";
import {RdsTableComponent} from "./table/rds-table.component";
import {MatTabsModule} from "@angular/material/tabs";
import {MatTooltipModule} from '@angular/material/tooltip';

const rdsRoutes: Route[] = [
  {
    path     : 'rds',
    component: RdsComponent,
    data: {
      roles: [PerfilUsuarioEnum.ROLE_ADMIN, PerfilUsuarioEnum.ROLE_SUPER, PerfilUsuarioEnum.ROLE_HYPER]
    }/*,
    resolve    : {
      initialData: AreaUsuarioResolver
    },*/

  },

];
@NgModule({
  declarations: [
    RdsTableComponent,
    RdsComponent
  ],
  imports: [
    RouterModule.forChild(rdsRoutes),
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSidenavModule,
    MatTableModule,
    MatTabsModule,
    MatSortModule,
    SharedModule,
    AdminContainerModule,
    MatTooltipModule
  ],
  providers: [
    RdsService
  ],
  exports: [
    RdsTableComponent
  ]
})
export class RdsModule
{
}
