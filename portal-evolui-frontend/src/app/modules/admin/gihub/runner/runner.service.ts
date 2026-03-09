import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {MatDialog} from "@angular/material/dialog";
import {RunnerGithubModel} from '../../../../shared/models/github.model';


@Injectable({
    providedIn: 'root'
})
export class RunnerService
{

    /**
     * Constructor
     */
    constructor(private _httpClient: HttpClient, private _matDialog: MatDialog)
    {

    }

    getAll(): Promise<Array<RunnerGithubModel>>
    {
        return this._httpClient.get<Array<RunnerGithubModel>>('api/admin/github/runner').toPromise();
    }
}
