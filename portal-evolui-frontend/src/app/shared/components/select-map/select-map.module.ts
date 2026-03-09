import {NgModule} from "@angular/core";
import {SharedModule} from "../../shared.module";
import {SelectMapComponent} from "./select-map.component";

@NgModule({
    declarations: [
        SelectMapComponent
    ],
    exports     : [
        SelectMapComponent
    ],
    imports : [
        SharedModule
    ]
})
export class SelectMapModule {

}
