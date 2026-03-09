import {NgModule} from '@angular/core';
import {Route, RouterModule} from '@angular/router';
import {ProjectComponent} from "./project.component";
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
import {ProjectTableComponent} from "./table/project-table.component";
import {ProjectModalComponent} from './modal/project-modal.component';
import {MatDialogModule} from '@angular/material/dialog';
import {NgxMaskDirective, NgxMaskPipe} from 'ngx-mask';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {MatCardModule} from '@angular/material/card';
import {ProjectService} from './project.service';
import {ProjectRepositoryModalComponent} from './modal/project-repository-modal.component';
import {ProjectFolderModalComponent} from './modal/project-folder-modal.component';
import {RepositoryModule} from '../gihub/repository/repository.module';
import {MatTooltipModule} from '@angular/material/tooltip';

const geracaoVersaoRoutes: Route[] = [
  {
    path: 'project',
    component: ProjectComponent,
    data: {
      roles: [PerfilUsuarioEnum.ROLE_SUPER, PerfilUsuarioEnum.ROLE_HYPER]
    }


  },

];
@NgModule({
  declarations: [
    ProjectComponent,
    ProjectTableComponent,
    ProjectModalComponent,
    ProjectRepositoryModalComponent,
    ProjectFolderModalComponent
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
    NgxMaskDirective,
    NgxMaskPipe,
    MatSlideToggleModule,
    SharedModule,
    AdminContainerModule,
    MatCardModule,
    RepositoryModule,
    MatTooltipModule,

  ],
  providers: [ProjectService],
})
export class ProjectModule
{
}
