import {NgModule} from "@angular/core";
import {MatIconModule} from "@angular/material/icon";
import {ConfirmationDialogComponent} from "./dialog.component";
import {MatDialogModule} from "@angular/material/dialog";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {CommonModule} from "@angular/common";
import {TranslocoModule} from "@jsverse/transloco";

@NgModule({
  declarations: [
    ConfirmationDialogComponent
  ],
  exports     : [
    ConfirmationDialogComponent
  ],
  imports : [
    MatIconModule,
    MatDialogModule,
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    TranslocoModule,
  ]
})
export class ConfirmationDialogModule {

}
