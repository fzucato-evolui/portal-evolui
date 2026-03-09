import {NgModule} from "@angular/core";
import {SharedModule} from "../../shared.module";
import {MatIconModule} from "@angular/material/icon";
import {NgxMaskDirective, NgxMaskPipe, provideNgxMask} from "ngx-mask";
import {MatInputModule} from "@angular/material/input";
import {LookupInputComponent} from "./lookup-input.component";

@NgModule({
    declarations: [
        LookupInputComponent
    ],
    exports     : [
        LookupInputComponent
    ],
    imports : [
        MatIconModule,
        MatInputModule,
        NgxMaskDirective,
        NgxMaskPipe,
        SharedModule
    ],
    providers: [
        provideNgxMask()
    ]
})
export class LookupInputModule {

}