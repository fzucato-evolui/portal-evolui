import {NgModule} from '@angular/core';
import {Route, RouterModule} from '@angular/router';
import {MatButtonModule} from "@angular/material/button";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatIconModule} from "@angular/material/icon";
import {MatInputModule} from "@angular/material/input";
import {MatSlideToggleModule} from "@angular/material/slide-toggle";
import {ConfigSystemComponent} from "./config-system.component";
import {PerfilUsuarioEnum} from "../../../shared/models/usuario.model";
import {SharedModule} from "../../../shared/shared.module";
import {AdminContainerModule} from "../../../shared/components/admin-container/admin-container.module";
import {ConfigSystemResolver} from "./config-system.resolver";
import {ConfigSystemGoogleComponent} from "./google/config-system-google.component";
import {ConfigSystemGithubComponent} from './github/config-system-github.component';
import {ConfigSystemAwsComponent} from './aws/config-system-aws.component';
import {MatTabsModule} from '@angular/material/tabs';
import {MatCardModule} from '@angular/material/card';
import {MatSelectModule} from '@angular/material/select';
import {ConfigSystemSmtpComponent} from './smtp/config-system-smtp.component';
import {ConfigSystemNotificationComponent} from './notification/config-system-notification.component';
import {MatTooltipModule} from '@angular/material/tooltip';
import {ConfigSystemMondayComponent} from './monday/config-system-monday.component';
import {FileDropModule} from '../../../shared/components/file-drop/file-drop.module';
import {ConfigSystemCicdComponent} from './cicd/config-system-cicd.component';
import {NgxMaskDirective, NgxMaskPipe} from 'ngx-mask';
import {ConfigSystemAxComponent} from './ax/config-system-ax.component';
import {ConfigSystemPortalLuthierComponent} from './portal-luthier/config-system-portal-luthier.component';

const usersRoutes: Route[] = [
  {
    path     : 'system-config',
    component: ConfigSystemComponent,
    data: {
      roles: [PerfilUsuarioEnum.ROLE_SUPER, PerfilUsuarioEnum.ROLE_HYPER]
    },
    resolve    : {
      initialData: ConfigSystemResolver
    },

  },

];
@NgModule({
  declarations: [
    ConfigSystemComponent,
    ConfigSystemGoogleComponent,
    ConfigSystemGithubComponent,
    ConfigSystemAwsComponent,
    ConfigSystemSmtpComponent,
    ConfigSystemNotificationComponent,
    ConfigSystemMondayComponent,
    ConfigSystemCicdComponent,
    ConfigSystemAxComponent,
    ConfigSystemPortalLuthierComponent
  ],
  imports     : [
    RouterModule.forChild(usersRoutes),
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSlideToggleModule,
    MatTabsModule,
    MatCardModule,
    MatSelectModule,
    MatTooltipModule,
    SharedModule,
    AdminContainerModule,
    FileDropModule,
    NgxMaskDirective,
    NgxMaskPipe
  ],
  providers: [

  ]
})
export class ConfigSystemModule
{
}
