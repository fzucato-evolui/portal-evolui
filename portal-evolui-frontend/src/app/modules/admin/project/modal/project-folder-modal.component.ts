import {Component, OnDestroy, OnInit, ViewEncapsulation} from '@angular/core';
import {Subject} from 'rxjs';
import {MatDialogRef} from '@angular/material/dialog';
import {RepositoryService} from '../../gihub/repository/repository.service';
import {GithubContentModel} from '../../../../shared/models/github.model';
import {MessageDialogService} from '../../../../shared/services/message/message-dialog-service';

@Component({
  selector      : 'project-folder-modal',
  styleUrls     : ['./project-folder-modal.component.scss'],
  templateUrl   : './project-folder-modal.component.html',
  encapsulation : ViewEncapsulation.None,

  standalone: false
})
export class ProjectFolderModalComponent implements OnInit, OnDestroy {
  private _unsubscribeAll: Subject<any> = new Subject<any>();

  title: string = 'Selecionar Pasta do Repositório';
  repository: string;
  initialPath: string = '';
  currentPath: string = '';
  breadcrumbs: Array<{name: string, path: string}> = [];
  contents: Array<GithubContentModel> = [];
  loading: boolean = false;
  filterText: string = '';

  constructor(
    private _repositoryService: RepositoryService,
    private _messageService: MessageDialogService,
    public dialogRef: MatDialogRef<ProjectFolderModalComponent>
  ) {}

  ngOnInit(): void {
    this.loadContents(this.initialPath || '');
  }

  get filteredContents(): Array<GithubContentModel> {
    if (!this.filterText) return this.contents;
    const filter = this.filterText.toLowerCase();
    return this.contents.filter(item => item.name.toLowerCase().includes(filter));
  }

  ngOnDestroy(): void {
    this._unsubscribeAll.next(undefined);
    this._unsubscribeAll.complete();
  }

  loadContents(path: string): void {
    this.loading = true;
    this.currentPath = path;
    this.filterText = '';
    this.buildBreadcrumbs(path);
    this._repositoryService.getContents(this.repository, path).then(resp => {
      this.contents = resp;
      this.loading = false;
    }).catch(err => {
      this._messageService.open('Erro ao carregar conteúdo do repositório!', 'ERRO', 'error');
      this.loading = false;
    });
  }

  navigateInto(item: GithubContentModel): void {
    if (item.type === 'dir') {
      this.loadContents(item.path);
    }
  }

  navigateToBreadcrumb(breadcrumb: {name: string, path: string}): void {
    this.loadContents(breadcrumb.path);
  }

  selectCurrentFolder(): void {
    this.dialogRef.close(this.currentPath);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  private buildBreadcrumbs(path: string): void {
    this.breadcrumbs = [{name: this.repository, path: ''}];
    if (path && path.length > 0) {
      const segments = path.split('/');
      let accumulator = '';
      for (const segment of segments) {
        accumulator = accumulator ? accumulator + '/' + segment : segment;
        this.breadcrumbs.push({name: segment, path: accumulator});
      }
    }
  }
}
