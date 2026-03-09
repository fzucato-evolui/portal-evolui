import {NgModule} from "@angular/core";
import {AdminContainerComponent} from "./admin-container.component";
import {MatButtonModule} from "@angular/material/button";
import {MatMenuModule} from "@angular/material/menu";
import {MatIconModule} from "@angular/material/icon";
import {MatTabsModule} from "@angular/material/tabs";
import {MatSidenavModule} from "@angular/material/sidenav";
import {SharedModule} from "../../shared.module";

@NgModule({
  declarations: [
    AdminContainerComponent
  ],

  exports     : [
    AdminContainerComponent
  ],
  imports : [
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatSidenavModule,
    MatTabsModule,
    SharedModule
  ]
})
export class AdminContainerModule {

}
