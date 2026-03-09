import {NgModule} from "@angular/core";
import {Route, RouterModule} from '@angular/router';
import {RunnerModule} from './runner/runner.module';
import {MemberModule} from './member/member.module';
import {RepositoryModule} from './repository/repository.module';

const awsRoutes: Route[] = [
  {
    path     : 'github',
    loadChildren: () => GithubModule,
  },

];
@NgModule({

  imports     : [
    RouterModule.forChild(awsRoutes),
    RunnerModule,
    MemberModule,
    RepositoryModule
  ],
  providers: [

  ]
})
export class GithubModule
{
}
