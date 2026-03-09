import {NgModule} from '@angular/core';
import {Route, RouterModule} from '@angular/router';
import {PerfilUsuarioEnum} from "../../../../shared/models/usuario.model";
import {AdminContainerModule} from "../../../../shared/components/admin-container/admin-container.module";
import {UsersListComponent} from "./users-list.component";
import {SharedModule} from "../../../../shared/shared.module";
import {UsersListTableComponent} from "./table/users-list-table.component";
import {MatButtonModule} from "@angular/material/button";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatIconModule} from "@angular/material/icon";
import {MatInputModule} from "@angular/material/input";
import {MatRadioModule} from "@angular/material/radio";
import {MatCheckboxModule} from "@angular/material/checkbox";
import {MatSelectModule} from "@angular/material/select";
import {MatSidenavModule} from "@angular/material/sidenav";
import {MatSlideToggleModule} from "@angular/material/slide-toggle";
import {MatTableModule} from "@angular/material/table";
import {MatSortModule} from "@angular/material/sort";
import {MatDialogModule} from "@angular/material/dialog";
import {NgxMaskDirective, NgxMaskPipe, provideNgxMask} from "ngx-mask";
import {UsersListFilterComponent} from "./filter/users-list-filter.component";
import {UsersListModalComponent} from "./modal/users-list-modal.component";
import {UsuarioListService} from "./usuario-list.service";

const usersRoutes: Route[] = [
  {
    path     : 'users',
    component: UsersListComponent,
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
    UsersListComponent,
    UsersListTableComponent,
    UsersListFilterComponent,
    UsersListModalComponent
  ],
  imports     : [
    RouterModule.forChild(usersRoutes),
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatRadioModule,
    MatCheckboxModule,
    MatSelectModule,
    MatSidenavModule,
    MatSlideToggleModule,
    MatTableModule,
    MatSortModule,
    MatDialogModule,
    NgxMaskDirective,
    NgxMaskPipe,
    SharedModule,
    AdminContainerModule
  ],
  providers: [
    UsuarioListService,
    provideNgxMask()
  ]
})
export class UsersListModule
{
}
