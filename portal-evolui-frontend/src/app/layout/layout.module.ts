import {NgModule} from '@angular/core';
import {LayoutComponent} from "./layout.component";
import {EmptyLayoutModule} from "./layouts/empty/empty.module";
import {SharedModule} from "../shared/shared.module";
import {ClassyLayoutModule} from "./layouts/classy/classy.module";


const layoutModules = [
  // Empty
  EmptyLayoutModule,
  ClassyLayoutModule
];

@NgModule({
  declarations: [
    LayoutComponent
  ],
  imports     : [
    SharedModule,
    ...layoutModules
  ],
  exports     : [
    LayoutComponent,
    ...layoutModules
  ]
})
export class LayoutModule
{
}
