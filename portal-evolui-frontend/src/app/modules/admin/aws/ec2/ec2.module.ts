import {NgModule} from '@angular/core';
import {Route, RouterModule} from '@angular/router';
import {Ec2Component} from "./ec2.component";
import {MatButtonModule} from "@angular/material/button";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatIconModule} from "@angular/material/icon";
import {MatInputModule} from "@angular/material/input";
import {MatSelectModule} from "@angular/material/select";
import {MatSidenavModule} from "@angular/material/sidenav";
import {MatTableModule} from "@angular/material/table";
import {MatSortModule} from "@angular/material/sort";
import {Ec2Service} from "./ec2.service";
import {PerfilUsuarioEnum} from "../../../../shared/models/usuario.model";
import {SharedModule} from "../../../../shared/shared.module";
import {AdminContainerModule} from "../../../../shared/components/admin-container/admin-container.module";
import {Ec2TableComponent} from "./table/ec2-table.component";
import {MatTabsModule} from "@angular/material/tabs";

const usersRoutes: Route[] = [
  {
    path     : 'ec2',
    component: Ec2Component,
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
    Ec2Component,
    Ec2TableComponent
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
    MatTabsModule,
    MatSortModule,
    SharedModule,
    AdminContainerModule
  ],
  providers: [
    Ec2Service
  ]
})
export class Ec2Module
{
}
