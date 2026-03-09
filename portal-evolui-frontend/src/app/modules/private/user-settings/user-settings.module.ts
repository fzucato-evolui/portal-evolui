import {NgModule} from '@angular/core';
import {Route, RouterModule} from '@angular/router';
import {SharedModule} from "../../../shared/shared.module";
import {AdminContainerModule} from "../../../shared/components/admin-container/admin-container.module";
import {UserSettingsComponent} from "./user-settings.component";
import {MatIconModule} from "@angular/material/icon";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatSelectModule} from "@angular/material/select";
import {MatButtonModule} from "@angular/material/button";
import {MatInputModule} from "@angular/material/input";
import {MatTooltipModule} from "@angular/material/tooltip";

const userSettings: Route[] = [
  {
    path     : 'user-settings',
    component: UserSettingsComponent,
    data: {
      roles: []
    }

  },

];
@NgModule({
  declarations: [
    UserSettingsComponent
  ],
  imports     : [
    RouterModule.forChild(userSettings),
    AdminContainerModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTooltipModule,
    SharedModule

  ],
  providers: [

  ]
})
export class UserSettingsModule
{
}
