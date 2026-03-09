import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {LayoutComponent} from "./layout/layout.component";
import {AuthGuard} from "./modules/auth/guards/auth.guard";
import {NoAuthGuard} from "./modules/auth/guards/noAuth.guard";
import {ProjectResolver} from './modules/admin/project/project.resolver';

const routes: Routes = [
  // Auth routes for guests
  {
    path: '',
    canActivate: [AuthGuard],
    canActivateChild: [AuthGuard],
    component: LayoutComponent,
    data: {
      layout: 'classy'
    },
    children: [
      {
        path: '',
        loadChildren: () => import('./modules/private/private.module').then(m => m.PrivateModule),
        resolve    : {
          initialData: ProjectResolver
        }
      },
      {
        path: 'admin',
        loadChildren: () => import('./modules/admin/admin.module').then(m => m.AdminModule),
        resolve    : {
          initialData: ProjectResolver
        }
      },

    ]
  },
  {
    path: '',
    component: LayoutComponent,
    canActivateChild: [NoAuthGuard],
    data: {
      layout: 'empty'
    },
    children: [
      {path: 'sign-in', loadChildren: () => import('./modules/sign-in/sign-in.module').then(m => m.AuthSignInModule)}
    ]
  },

];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
