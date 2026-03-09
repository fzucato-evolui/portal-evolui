import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {MatDialog} from "@angular/material/dialog";
import {LogAWSActionFilterModel, LogAWSActionModel} from "../../../../shared/models/log.aws.action.model";

@Injectable({
  providedIn: 'root'
})
export class LogAwsService
{

  /**
   * Constructor
   */
  constructor(private _httpClient: HttpClient, private _matDialog: MatDialog)
  {

  }

  getAll(): Promise<Array<LogAWSActionModel>>
  {
    return this._httpClient.get<Array<LogAWSActionModel>>('api/admin/aws/log').toPromise();
  }

  filtrar(model: LogAWSActionFilterModel): Promise<Array<LogAWSActionModel>>
  {
    return this._httpClient.post<Array<LogAWSActionModel>>('api/admin/aws/log/filter', model).toPromise();
  }


}
