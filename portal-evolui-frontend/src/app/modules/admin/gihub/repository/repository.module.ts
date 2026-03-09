import {NgModule} from '@angular/core';
import {Route, RouterModule} from '@angular/router';
import {RepositoryComponent} from "./repository.component";
import {MatButtonModule} from "@angular/material/button";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatIconModule} from "@angular/material/icon";
import {MatInputModule} from "@angular/material/input";
import {MatSelectModule} from "@angular/material/select";
import {MatSidenavModule} from "@angular/material/sidenav";
import {MatTableModule} from "@angular/material/table";
import {MatSortModule} from "@angular/material/sort";
import {RepositoryService} from "./repository.service";
import {PerfilUsuarioEnum} from "../../../../shared/models/usuario.model";
import {SharedModule} from "../../../../shared/shared.module";
import {AdminContainerModule} from "../../../../shared/components/admin-container/admin-container.module";
import {MatTabsModule} from "@angular/material/tabs";
import {MatTooltipModule} from '@angular/material/tooltip';
import {RepositoryTableComponent} from './table/repository-table.component';

const repositoryRoutes: Route[] = [
  {
    path     : 'repository',
    component: RepositoryComponent,
    data: {
      roles: [PerfilUsuarioEnum.ROLE_SUPER, PerfilUsuarioEnum.ROLE_HYPER, PerfilUsuarioEnum.ROLE_ADMIN]
    }/*,
    resolve    : {
      initialData: AreaUsuarioResolver
    },*/

  },

];
@NgModule({
  declarations: [
    RepositoryComponent,
    RepositoryTableComponent
  ],
  imports: [
    RouterModule.forChild(repositoryRoutes),
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
  exports: [
    RepositoryTableComponent
  ],
  providers: [
    RepositoryService
  ]
})
export class RepositoryModule
{
}
