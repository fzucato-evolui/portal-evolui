import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {MatDialog} from "@angular/material/dialog";
import {WorkspaceModel} from 'app/shared/models/workspace.model';


@Injectable({
    providedIn: 'root'
})
export class WorkspaceService
{

    /**
     * Constructor
     */
    constructor(private _httpClient: HttpClient, private _matDialog: MatDialog)
    {

    }

    getAll(): Promise<{[key: string]: Array<WorkspaceModel>}>
    {
        return this._httpClient.get<{[key: string]: Array<WorkspaceModel>}>('api/admin/aws/workspace').toPromise();
    }

    start(model: WorkspaceModel): Promise<any> {
        return this._httpClient.post('api/admin/aws/workspace/start', model).toPromise();
    }

    stop(model: WorkspaceModel): Promise<any> {
      return this._httpClient.post('api/admin/aws/workspace/stop', model).toPromise();
    }

    reboot(model: WorkspaceModel) {
      return this._httpClient.post('api/admin/aws/workspace/reboot', model).toPromise();
    }
}
