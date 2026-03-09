import {NgModule} from '@angular/core';
import {HttpClientModule} from '@angular/common/http';
import {RouterModule} from '@angular/router';
import {MatButtonModule} from '@angular/material/button';
import {MatDividerModule} from '@angular/material/divider';
import {MatIconModule} from '@angular/material/icon';
import {MatMenuModule} from '@angular/material/menu';
import {SharedModule} from 'app/shared/shared.module';
import {ClassyLayoutComponent} from "./classy.component";
import {MatSidenavModule} from "@angular/material/sidenav";
import {MatListModule} from "@angular/material/list";
import {MatToolbarModule} from "@angular/material/toolbar";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatNativeDateModule} from "@angular/material/core";
import {MatRadioModule} from "@angular/material/radio";
import {UserModule} from "../../../shared/components/user/user.module";
import {MatExpansionModule} from '@angular/material/expansion';

@NgModule({
  declarations: [
    ClassyLayoutComponent
  ],
  imports     : [
    HttpClientModule,
    RouterModule,
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    MatMenuModule,
    MatSidenavModule,
    MatListModule,
    MatExpansionModule,
    MatRadioModule,
    MatToolbarModule,
    FormsModule,
    MatNativeDateModule,
    ReactiveFormsModule,
    SharedModule,
    UserModule
  ],
  exports     : [
    ClassyLayoutComponent
  ]
})
export class ClassyLayoutModule
{
}
