import {NgModule} from '@angular/core';
import {Route, RouterModule} from '@angular/router';
import {WorkspaceComponent} from "./workspace.component";
import {MatButtonModule} from "@angular/material/button";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatIconModule} from "@angular/material/icon";
import {MatInputModule} from "@angular/material/input";
import {MatSelectModule} from "@angular/material/select";
import {MatSidenavModule} from "@angular/material/sidenav";
import {MatTableModule} from "@angular/material/table";
import {MatSortModule} from "@angular/material/sort";
import {WorkspaceService} from "./workspace.service";
import {PerfilUsuarioEnum} from "../../../../shared/models/usuario.model";
import {SharedModule} from "../../../../shared/shared.module";
import {AdminContainerModule} from "../../../../shared/components/admin-container/admin-container.module";
import {WorkspaceTableComponent} from "./table/workspace-table.component";
import {MatTabsModule} from "@angular/material/tabs";
import {MatTooltipModule} from '@angular/material/tooltip';

const workspaceRoutes: Route[] = [
  {
    path     : 'workspace',
    component: WorkspaceComponent,
    data: {
      roles: [PerfilUsuarioEnum.ROLE_SUPER, PerfilUsuarioEnum.ROLE_HYPER]
    }/*,
    resolve    : {
      initialData: AreaUsuarioResolver
    },*/

  },

];
@NgModule({
  declarations: [
    WorkspaceComponent,
    WorkspaceTableComponent
  ],
  imports     : [
    RouterModule.forChild(workspaceRoutes),
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSidenavModule,
    MatTableModule,
    MatTooltipModule,
    MatTabsModule,
    MatSortModule,
    SharedModule,
    AdminContainerModule
  ],
  providers: [
    WorkspaceService
  ]
})
export class WorkspaceModule
{
}
