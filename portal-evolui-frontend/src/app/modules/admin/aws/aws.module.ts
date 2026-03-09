import {NgModule} from "@angular/core";
import {Ec2Module} from './ec2/ec2.module';
import {RdsModule} from './rds/rds.module';
import {LogAwsModule} from './log-aws/log-aws.module';
import {Route, RouterModule} from '@angular/router';
import {WorkspaceModule} from './workspace/workspace.module';
import {ActionRdsModule} from './action-rds/action-rds.module';

const awsRoutes: Route[] = [
  {
    path     : 'aws',
    loadChildren: () => AwsModule,
  },

];
@NgModule({

  imports     : [
    RouterModule.forChild(awsRoutes),
    Ec2Module,
    WorkspaceModule,
    RdsModule,
    LogAwsModule,
    ActionRdsModule
  ],
  providers: [

  ]
})
export class AwsModule
{
}
