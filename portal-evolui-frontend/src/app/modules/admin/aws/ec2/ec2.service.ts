import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {MatDialog} from "@angular/material/dialog";
import {EC2Model} from "../../../../shared/models/ec2.model";

@Injectable({
    providedIn: 'root'
})
export class Ec2Service
{

    /**
     * Constructor
     */
    constructor(private _httpClient: HttpClient, private _matDialog: MatDialog)
    {

    }

    getAll(): Promise<{DEV: Array<EC2Model>, PROD: Array<EC2Model>, ROOT: Array<EC2Model>}>
    {
        return this._httpClient.get<{DEV: Array<EC2Model>, PROD: Array<EC2Model>, ROOT: Array<EC2Model>}>('api/admin/aws/ec2').toPromise();
    }

    start(model: EC2Model): Promise<any> {
        return this._httpClient.post('api/admin/aws/ec2/start', model).toPromise();
    }

    stop(model: EC2Model): Promise<any> {
      return this._httpClient.post('api/admin/aws/ec2/stop', model).toPromise();
    }

    reboot(model: EC2Model): Promise<any> {
      return this._httpClient.post('api/admin/aws/ec2/reboot', model).toPromise();
    }

}
