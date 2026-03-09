import {Component, OnDestroy, OnInit, ViewEncapsulation} from "@angular/core";
import {ProjectModel,} from '../../../../shared/models/project.model';
import {Subject} from 'rxjs';
import {ProjectService} from '../project.service';
import {MessageDialogService} from '../../../../shared/services/message/message-dialog-service';
import {MatDialogRef} from '@angular/material/dialog';
import {MatTableDataSource} from '@angular/material/table';
import {DetailedRepositoryGithubModel} from '../../../../shared/models/github.model';
import {RepositoryService} from '../../gihub/repository/repository.service';

@Component({
  selector       : 'project-repository-modal',
  styleUrls      : ['/project-repository-modal.component.scss'],
  templateUrl    : './project-repository-modal.component.html',
  encapsulation  : ViewEncapsulation.None,

  standalone: false
})
export class ProjectRepositoryModalComponent implements OnInit, OnDestroy
{
  get dataSource(): MatTableDataSource<DetailedRepositoryGithubModel> {
    return this._dataSource;
  }

  set dataSource(value: MatTableDataSource<DetailedRepositoryGithubModel>) {
    this._dataSource = value;
  }

  private _workflow: boolean = true;

  get workflow(): boolean {
    return this._workflow;
  }

  set workflow(value: boolean) {
    this._workflow = value;
    if (!value) {
      this.title = "Repositório de Fontes do Módulo do Projeto";
    }
  }

  private _unsubscribeAll: Subject<any> = new Subject<any>();

  private _dataSource = new MatTableDataSource<DetailedRepositoryGithubModel>();

  public model: ProjectModel;
  title: string = "Repositórios de Workflow dos Projetos";
  service: ProjectService;

  constructor(private _githubService: RepositoryService,
              private _messageService: MessageDialogService,
              public dialogRef: MatDialogRef<ProjectRepositoryModalComponent>)
  {
  }

  ngOnInit(): void {
    if (this.workflow) {
      this._githubService.getWorkflowRepositories().then(resp => {
        this.dataSource.data = resp;
      });
    }
    else {
      this._githubService.getAll().then(resp => {
        this.dataSource.data = resp;
      });
    }
  }

  ngOnDestroy(): void {
    this._unsubscribeAll.next(undefined);
    this._unsubscribeAll.complete();
  }

  selectedRepository(model: DetailedRepositoryGithubModel) {
    if (this.workflow) {
      this.service.getRepoStructure(model.name).then(result => {
        if (!result) {
          this._messageService.open("Repositório não está preparado para pipelines de construção!", "ERRO", "error");
          return;
        } else {
          result.repository = model.name;
          this.dialogRef.close(result);
        }
      });
    }
    else {
      this.dialogRef.close(model);
    }
  }

  filterRepositoriesWithWorkflow(
    repos: DetailedRepositoryGithubModel[]
  ): DetailedRepositoryGithubModel[] {

    // 1. pega todos os workflow-xxx
    const workflowBases = new Set(
      repos
      .filter(r => r.name.startsWith('workflow-'))
      .map(r => r.name.replace(/^workflow-/, ''))
    );

    // 2. mantém apenas os repositórios base que possuem workflow correspondente
    return repos.filter(r => workflowBases.has(r.name));
  }
}
