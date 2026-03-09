import {Component, Inject, OnInit, ViewEncapsulation} from "@angular/core";
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {MatTableDataSource} from '@angular/material/table';
import {BranchesAndTagsDetailModel, GitRefDetailModel} from '../../../../shared/models/github.model';

export interface GitRefSelectionResult {
  type: 'branch' | 'tag';
  name: string;
  sha: string;
}

@Component({
  selector       : 'geracao-versao-git-ref-modal',
  styleUrls      : ['./geracao-versao-git-ref-modal.component.scss'],
  templateUrl    : './geracao-versao-git-ref-modal.component.html',
  encapsulation  : ViewEncapsulation.None,

  standalone: false
})
export class GeracaoVersaoGitRefModalComponent implements OnInit
{
  title: string = 'Selecionar Branch ou Tag';
  displayedColumns: string[] = ['name', 'sha', 'author', 'message', 'date'];

  branchesDataSource = new MatTableDataSource<GitRefDetailModel>();
  tagsDataSource = new MatTableDataSource<GitRefDetailModel>();

  branchFilter: string = '';
  tagFilter: string = '';

  constructor(
    public dialogRef: MatDialogRef<GeracaoVersaoGitRefModalComponent>,
    @Inject(MAT_DIALOG_DATA) public data: BranchesAndTagsDetailModel
  ) {}

  ngOnInit(): void {
    if (this.data) {
      this.branchesDataSource.data = this.data.branches || [];
      this.tagsDataSource.data = this.data.tags || [];
    }
  }

  applyBranchFilter(value: string): void {
    this.branchFilter = value;
    this.branchesDataSource.filter = value.trim().toLowerCase();
  }

  applyTagFilter(value: string): void {
    this.tagFilter = value;
    this.tagsDataSource.filter = value.trim().toLowerCase();
  }

  selectBranch(row: GitRefDetailModel): void {
    const result: GitRefSelectionResult = {
      type: 'branch',
      name: row.name,
      sha: row.sha
    };
    this.dialogRef.close(result);
  }

  selectTag(row: GitRefDetailModel): void {
    const result: GitRefSelectionResult = {
      type: 'tag',
      name: row.name,
      sha: row.sha
    };
    this.dialogRef.close(result);
  }

  truncateSha(sha: string): string {
    return sha ? sha.substring(0, 7) : '';
  }
}
