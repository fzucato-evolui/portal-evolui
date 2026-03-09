import {NgModule} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {MatDividerModule} from '@angular/material/divider';
import {MatIconModule} from '@angular/material/icon';
import {MatMenuModule} from '@angular/material/menu';
import {SharedModule} from 'app/shared/shared.module';
import {MatDialogModule} from "@angular/material/dialog";
import {UserComponent} from "./user.component";

@NgModule({
  declarations: [
    UserComponent
  ],
    imports     : [
        MatButtonModule,
        MatDividerModule,
        MatIconModule,
        MatMenuModule,
        MatDialogModule,
        SharedModule
    ],
    exports     : [
        UserComponent
    ]
})
export class UserModule
{
}
