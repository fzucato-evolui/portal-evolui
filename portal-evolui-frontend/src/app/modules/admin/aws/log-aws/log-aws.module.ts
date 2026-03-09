import {NgModule} from '@angular/core';
import {Route, RouterModule} from '@angular/router';
import {LogAwsComponent} from "./log-aws.component";
import {MatButtonModule} from "@angular/material/button";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatIconModule} from "@angular/material/icon";
import {MatInputModule} from "@angular/material/input";
import {MatSelectModule} from "@angular/material/select";
import {MatSidenavModule} from "@angular/material/sidenav";
import {MatTableModule} from "@angular/material/table";
import {MatSortModule} from "@angular/material/sort";
import {LogAwsService} from "./log-aws.service";
import {PerfilUsuarioEnum} from "../../../../shared/models/usuario.model";
import {SharedModule} from "../../../../shared/shared.module";
import {AdminContainerModule} from "../../../../shared/components/admin-container/admin-container.module";
import {LogAwsTableComponent} from "./table/log-aws-table.component";
import {LogAwsFilterComponent} from "./filter/log-aws-filter.component";
import {MatDatepickerModule} from "@angular/material/datepicker";

const usersRoutes: Route[] = [
  {
    path     : 'log-aws',
    component: LogAwsComponent,
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
    LogAwsComponent,
    LogAwsTableComponent,
    LogAwsFilterComponent
  ],
  imports     : [
    RouterModule.forChild(usersRoutes),
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSidenavModule,
    MatTableModule,
    MatDatepickerModule,
    MatSortModule,
    SharedModule,
    AdminContainerModule
  ],
  providers: [
    LogAwsService
  ]
})
export class LogAwsModule
{
}
