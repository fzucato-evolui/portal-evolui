import {NgModule} from '@angular/core';
import {Route, RouterModule} from '@angular/router';
import {HealthCheckerComponent} from './health-checker.component';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatSidenavModule} from '@angular/material/sidenav';
import {MatTableModule} from '@angular/material/table';
import {MatSortModule} from '@angular/material/sort';
import {PerfilUsuarioEnum} from '../../../shared/models/usuario.model';
import {SharedModule} from '../../../shared/shared.module';
import {AdminContainerModule} from '../../../shared/components/admin-container/admin-container.module';
import {HealthCheckerTableComponent} from './table/health-checker-table.component';
import {HealthCheckerModalComponent} from './modal/health-checker-modal.component';
import {MatDialogModule} from '@angular/material/dialog';
import {NgxMaskDirective, NgxMaskPipe} from 'ngx-mask';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {MatCardModule} from '@angular/material/card';
import {MatStepperModule} from '@angular/material/stepper';
import {MatSelectModule} from '@angular/material/select';
import {MatRadioModule} from '@angular/material/radio';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatSliderModule} from '@angular/material/slider';
import {MatTooltipModule} from '@angular/material/tooltip';
import {FileDropModule} from '../../../shared/components/file-drop/file-drop.module';
import {HealthCheckerMonitorModalComponent} from './modal/health-checker-monitor-modal.component';
import {NgApexchartsModule} from 'ng-apexcharts';
import {MatDividerModule} from '@angular/material/divider';
import {MatListModule} from '@angular/material/list';
import {MatExpansionModule} from '@angular/material/expansion';
import {HealthCheckerResolver} from './health-checker.resolver';
import {HardwareInfoComponent} from './modal/hardware/info/hardware-info.component';
import {HardwareMemoryComponent} from './modal/hardware/memory/hardware-memory.component';
import {HardwareProcessorComponent} from './modal/hardware/processor/hardware-processor.component';
import {HardwareNetworkComponent} from './modal/hardware/network/hardware-network.component';
import {HardwareDiskstoreComponent} from './modal/hardware/diskstore/hardware-diskstore.component';
import {SoftwareInfoComponent} from './modal/software/info/software-info.component';
import {SoftwareVersionComponent} from './modal/software/version/software-version.component';
import {SoftwareFileSystemComponent} from './modal/software/file-system/software-file-system.component';
import {SoftwareNetworkComponent} from './modal/software/network/software-network.component';
import {SoftwareProcessComponent} from './modal/software/process/software-process.component';
import {SoftwareServiceComponent} from './modal/software/service/software-service.component';
import {SoftwareProtocolStatsComponent} from './modal/software/protocol-stats/software-protocol-stats.component';
import {SoftwareSessionComponent} from './modal/software/session/software-session.component';
import {MatPaginatorModule} from "@angular/material/paginator";

const healthCheckerRoutes: Route[] = [
  {
    path: 'health-checker',
    component: HealthCheckerComponent,
    data: {
      roles: [PerfilUsuarioEnum.ROLE_SUPER, PerfilUsuarioEnum.ROLE_HYPER]
    }
  },
  {
    path: 'health-checker/:id',
    component: HealthCheckerComponent,
    data: {
      roles: [PerfilUsuarioEnum.ROLE_SUPER, PerfilUsuarioEnum.ROLE_HYPER]
    },
    resolve    : {
      initialData: HealthCheckerResolver
    }


  },

];
@NgModule({
  declarations: [
    HealthCheckerComponent,
    HealthCheckerTableComponent,
    HealthCheckerModalComponent,
    HealthCheckerMonitorModalComponent,
    HardwareInfoComponent,
    HardwareProcessorComponent,
    HardwareMemoryComponent,
    HardwareDiskstoreComponent,
    HardwareNetworkComponent,
    SoftwareInfoComponent,
    SoftwareVersionComponent,
    SoftwareFileSystemComponent,
    SoftwareNetworkComponent,
    SoftwareProcessComponent,
    SoftwareServiceComponent,
    SoftwareProtocolStatsComponent,
    SoftwareSessionComponent
  ],
  imports: [
    RouterModule.forChild(healthCheckerRoutes),
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatDialogModule,
    MatSidenavModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    NgxMaskDirective,
    NgxMaskPipe,
    MatSlideToggleModule,
    MatSliderModule,
    MatSelectModule,
    MatRadioModule,
    MatCheckboxModule,
    MatTooltipModule,
    MatStepperModule,
    FileDropModule,
    SharedModule,
    AdminContainerModule,
    MatCardModule,
    NgApexchartsModule,
    MatDividerModule,
    MatListModule,
    MatExpansionModule

  ]
})
export class HealthCheckerModule
{
}
