import {NgModule} from "@angular/core";
import {HomeModule} from "./home/home.module";
import {SharedModule} from "../../shared/shared.module";
import {UserSettingsModule} from "./user-settings/user-settings.module";

@NgModule({

  imports     : [
    HomeModule,
    UserSettingsModule,
    SharedModule
  ],
  providers: [

  ]
})
export class PrivateModule
{
}
