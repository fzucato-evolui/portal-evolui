import {NgModule} from '@angular/core';
import {Route, RouterModule} from '@angular/router';
import {HomeComponent} from "./home.component";
import {AdminContainerModule} from "../../../shared/components/admin-container/admin-container.module";
import {HomeResolver} from './home.resolver';
import {MatExpansionModule} from '@angular/material/expansion';
import {NgApexchartsModule} from 'ng-apexcharts';
import {SharedModule} from '../../../shared/shared.module';
import {MatRadioModule} from '@angular/material/radio';

const homeRoutes: Route[] = [
  {
    path     : '',
    component: HomeComponent,
    data: {
      roles: []
    },
    resolve    : {
      initialData: HomeResolver
    }

  },

];
@NgModule({
  declarations: [
    HomeComponent
  ],
  imports     : [
    RouterModule.forChild(homeRoutes),
    AdminContainerModule,
    SharedModule,
    MatExpansionModule,
    MatRadioModule,
    NgApexchartsModule
  ],
  providers: [

  ]
})
export class HomeModule
{
}
