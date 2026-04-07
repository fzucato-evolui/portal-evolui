import {NgModule} from '@angular/core';
import {Route, RouterModule} from '@angular/router';
import {RunnerComponent} from "./runner.component";
import {MatButtonModule} from "@angular/material/button";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatIconModule} from "@angular/material/icon";
import {MatInputModule} from "@angular/material/input";
import {MatSelectModule} from "@angular/material/select";
import {MatSidenavModule} from "@angular/material/sidenav";
import {MatTableModule} from "@angular/material/table";
import {MatSortModule} from "@angular/material/sort";
import {RunnerService} from "./runner.service";
import {PerfilUsuarioEnum} from "../../../../shared/models/usuario.model";
import {SharedModule} from "../../../../shared/shared.module";
import {AdminContainerModule} from "../../../../shared/components/admin-container/admin-container.module";
import {RunnerTableComponent} from "./table/runner-table.component";
import {MatTabsModule} from "@angular/material/tabs";
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatDialogModule} from '@angular/material/dialog';
import {MatStepperModule} from '@angular/material/stepper';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatRadioModule} from '@angular/material/radio';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {RunnerInstallerModalComponent} from './modal/runner-installer-modal.component';
import {RunnerRemoveModalComponent} from './modal/runner-remove-modal.component';

const runnerRoutes: Route[] = [
  {
    path     : 'runner',
    component: RunnerComponent,
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
    RunnerComponent,
    RunnerTableComponent,
    RunnerInstallerModalComponent,
    RunnerRemoveModalComponent
  ],
  imports     : [
    RouterModule.forChild(runnerRoutes),
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
    MatDialogModule,
    MatStepperModule,
    MatCheckboxModule,
    MatRadioModule,
    MatProgressSpinnerModule,
    SharedModule,
    AdminContainerModule
  ],
  providers: [
    RunnerService
  ]
})
export class RunnerModule
{
}
