import {NgModule} from '@angular/core';
import {Route, RouterModule} from '@angular/router';
import {ActionRdsComponent} from "./action-rds.component";
import {MatButtonModule} from "@angular/material/button";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatIconModule} from "@angular/material/icon";
import {MatInputModule} from "@angular/material/input";
import {MatSelectModule} from "@angular/material/select";
import {MatSidenavModule} from "@angular/material/sidenav";
import {MatTableModule} from "@angular/material/table";
import {MatSortModule} from "@angular/material/sort";
import {ActionRdsService} from "./action-rds.service";
import {PerfilUsuarioEnum} from "../../../../shared/models/usuario.model";
import {SharedModule} from "../../../../shared/shared.module";
import {AdminContainerModule} from "../../../../shared/components/admin-container/admin-container.module";
import {ActionRdsTableComponent} from "./table/action-rds-table.component";
import {ActionRdsFilterComponent} from "./filter/action-rds-filter.component";
import {MatDatepickerModule} from "@angular/material/datepicker";
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatStepperModule} from '@angular/material/stepper';
import {MatDialogModule} from '@angular/material/dialog';
import {ActionRdsModalComponent} from './modal/action-rds-modal.component';
import {MatTabsModule} from '@angular/material/tabs';
import {RdsModule} from '../rds/rds.module';
import {MatMenuModule} from '@angular/material/menu';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {
  NgxMatDatepickerActions,
  NgxMatDatepickerApply,
  NgxMatDatepickerCancel,
  NgxMatDatepickerClear,
  NgxMatDatepickerInput,
  NgxMatDatetimepicker,
} from '@ngxmc/datetime-picker';
import {NgxMaskDirective, NgxMaskPipe} from 'ngx-mask';
import {ActionRdsStatusModalComponent} from './modal/action-rds-status-modal.component';
import {ScrollingModule} from '@angular/cdk/scrolling';

const usersRoutes: Route[] = [
  {
    path     : 'action-rds',
    component: ActionRdsComponent,
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
    ActionRdsComponent,
    ActionRdsTableComponent,
    ActionRdsFilterComponent,
    ActionRdsModalComponent,
    ActionRdsStatusModalComponent
  ],
    imports: [
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
        AdminContainerModule,
        MatTooltipModule,
        MatStepperModule,
        MatDialogModule,
        MatTabsModule,
        RdsModule,
        MatMenuModule,
        MatSlideToggleModule,
        NgxMatDatepickerActions,
        NgxMatDatepickerActions,
        NgxMatDatepickerApply,
        NgxMatDatepickerCancel,
        NgxMatDatepickerClear,
        NgxMatDatepickerInput,
        NgxMatDatetimepicker,
        NgxMaskDirective,
        NgxMaskPipe,
        ScrollingModule
    ],
  providers: [
    ActionRdsService
  ]
})
export class ActionRdsModule
{
}
