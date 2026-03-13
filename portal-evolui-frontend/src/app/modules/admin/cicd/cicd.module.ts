import {NgModule} from '@angular/core';
import {Route, RouterModule, UrlSegment} from '@angular/router';
import {CICDComponent} from "./cicd.component";
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
import {CICDTableComponent} from "./table/cicd-table.component";
import {CICDFilterComponent} from "./filter/cicd-filter.component";
import {MatDatepickerModule} from "@angular/material/datepicker";
import {MatTooltipModule} from '@angular/material/tooltip';
import {CICDResolver} from './cicd.resolver';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {MatDialogModule} from '@angular/material/dialog';
import {MatCardModule} from '@angular/material/card';
import {NgxMatDatetimePickerInputV2, NgxMatDatetimePickerV2, NgxMatTimepickerComponent} from '@ngxmc/datetime-picker';
import {MatNativeDateModule} from '@angular/material/core';
import {CICDReportModalComponent} from './modal/cicd-report-modal.component';
import {CICDModalComponent} from './modal/cicd-modal.component';
import {NgxMaskDirective, NgxMaskPipe} from 'ngx-mask';
import {BuildReportComponent} from './build-report/build-report.component';
import {MatAutocomplete, MatAutocompleteTrigger} from "@angular/material/autocomplete";


export function matcher(url: UrlSegment[]) {
  return url.length === 2 && url[0].path === 'cicd' && /^[a-zA-Z]{1,}$/.test(url[1].path)
    ? { consumed: url }
    : null;
}
const cicdRoutes: Route[] = [
  {
    matcher: matcher,
    component: CICDComponent,
    data: {
      roles: [PerfilUsuarioEnum.ROLE_SUPER, PerfilUsuarioEnum.ROLE_HYPER]
    },
    resolve    : {
      initialData: CICDResolver
    }

  },

];
@NgModule({
  declarations: [
    CICDComponent,
    CICDTableComponent,
    CICDFilterComponent,
    CICDReportModalComponent,
    CICDModalComponent,
    BuildReportComponent

  ],
  imports: [
    RouterModule.forChild(cicdRoutes),
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatDialogModule,
    MatSidenavModule,
    MatTableModule,
    MatDatepickerModule,
    NgxMatDatetimePickerV2,
    NgxMatDatetimePickerInputV2,
    NgxMatTimepickerComponent,
    MatNativeDateModule,
    MatSortModule,
    MatCardModule,
    SharedModule,
    AdminContainerModule,
    NgxMaskDirective,
    NgxMaskPipe,
    MatAutocomplete,
    MatAutocompleteTrigger
  ]
})
export class CICDModule
{
}
