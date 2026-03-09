import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {MatDialog} from "@angular/material/dialog";
import {DetailedRepositoryGithubModel, GithubContentModel} from '../../../../shared/models/github.model';


@Injectable({
    providedIn: 'root'
})
export class RepositoryService
{

    /**
     * Constructor
     */
    constructor(private _httpClient: HttpClient, private _matDialog: MatDialog)
    {

    }

    getAll(): Promise<Array<DetailedRepositoryGithubModel>>
    {
        return this._httpClient.get<Array<DetailedRepositoryGithubModel>>('api/admin/github/repository').toPromise();
    }

    getWorkflowRepositories(): Promise<Array<DetailedRepositoryGithubModel>>
    {
        return this._httpClient.get<Array<DetailedRepositoryGithubModel>>('api/admin/github/repository/workflow').toPromise();
    }

    getContents(repository: string, path: string = ''): Promise<Array<GithubContentModel>>
    {
        return this._httpClient.get<Array<GithubContentModel>>(
            `api/admin/github/repository/${encodeURIComponent(repository)}/contents`,
            { params: { path } }
        ).toPromise();
    }
}
