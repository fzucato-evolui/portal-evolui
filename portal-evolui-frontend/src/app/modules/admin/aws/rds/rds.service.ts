import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {MatDialog} from "@angular/material/dialog";
import {RDSModel} from "../../../../shared/models/rds.model";

@Injectable({
    providedIn: 'root'
})
export class RdsService
{

    /**
     * Constructor
     */
    constructor(private _httpClient: HttpClient, private _matDialog: MatDialog)
    {

    }

    getAll(): Promise<{DEV: Array<RDSModel>, PROD: Array<RDSModel>, ROOT: Array<RDSModel>}>
    {
        return this._httpClient.get<{DEV: Array<RDSModel>, PROD: Array<RDSModel>, ROOT: Array<RDSModel>}>('api/admin/aws/rds').toPromise();
    }

    start(model: RDSModel): Promise<any> {
        return this._httpClient.post('api/admin/aws/rds/start', model).toPromise();
    }

    stop(model: RDSModel): Promise<any> {
      return this._httpClient.post('api/admin/aws/rds/stop', model).toPromise();
    }



}
