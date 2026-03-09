import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {MatDialog} from "@angular/material/dialog";
import {MemberGithubModel} from '../../../../shared/models/github.model';


@Injectable({
    providedIn: 'root'
})
export class MemberService
{

    /**
     * Constructor
     */
    constructor(private _httpClient: HttpClient, private _matDialog: MatDialog)
    {

    }

    getAll(): Promise<Array<MemberGithubModel>>
    {
        return this._httpClient.get<Array<MemberGithubModel>>('api/admin/github/member').toPromise();
    }
}
