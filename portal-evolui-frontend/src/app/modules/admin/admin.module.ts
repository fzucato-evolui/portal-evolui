import {NgModule} from "@angular/core";
import {SharedModule} from "../../shared/shared.module";
import {UsersListModule} from "./users/list/users-list.module";
import {UsersEditModule} from "./users/edit/users-edit.module";
import {ConfigSystemModule} from "./config-system/config-system.module";
import {GeracaoVersaoModule} from './geracao-versao/geracao-versao.module';
import {ClienteModule} from './clients/cliente.module';
import {MetadadosModule} from './metadados/metadados.module';
import {AmbienteModule} from './ambiente/ambiente.module';
import {AtualizacaoVersaoModule} from './atualizacao-versao/atualizacao-versao.module';
import {VersaoModule} from './versao/versao.module';
import {ProjectModule} from './project/project.module';
import {HealthCheckerModule} from './health-checker/health-checker.module';
import {CICDModule} from './cicd/cicd.module';
import {AwsModule} from './aws/aws.module';
import {GithubModule} from './gihub/github.module';

@NgModule({

  imports     : [
    AwsModule,
    GithubModule,
    UsersListModule,
    UsersEditModule,
    ConfigSystemModule,
    ProjectModule,
    GeracaoVersaoModule,
    ClienteModule,
    MetadadosModule,
    AmbienteModule,
    AtualizacaoVersaoModule,
    VersaoModule,
    HealthCheckerModule,
    CICDModule,
    SharedModule
  ],
  providers: [

  ]
})
export class AdminModule
{
}
