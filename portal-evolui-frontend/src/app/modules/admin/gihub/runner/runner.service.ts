import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {MatDialog} from "@angular/material/dialog";
import {RunnerGithubModel} from '../../../../shared/models/github.model';
import {
  ActionsRunnerLatestResponse,
  GithubRegistrationTokenResponse
} from '../../../../shared/models/runner-installer.model';


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

    generateInstallerSessionToken(uuid: string): Promise<{ token: string; endpoint: string }> {
      return this._httpClient
        .post<{ token: string; endpoint: string }>(`api/admin/github/runner/generate-token/${uuid}`, {})
        .toPromise();
    }

    getRegistrationToken(): Promise<GithubRegistrationTokenResponse> {
      return this._httpClient
        .post<GithubRegistrationTokenResponse>('api/admin/github/runner/registration-token', {})
        .toPromise();
    }

    getActionsRunnerLatest(os: string): Promise<ActionsRunnerLatestResponse> {
      return this._httpClient
        .get<ActionsRunnerLatestResponse>('api/admin/github/runner/actions-runner-latest', { params: { os } })
        .toPromise();
    }

    isRunnerNameAvailable(name: string): Promise<{ available: boolean }> {
      return this._httpClient
        .get<{ available: boolean }>('api/admin/github/runner/runner-name-available', { params: { name } })
        .toPromise();
    }

    deleteRunner(runnerId: number): Promise<void> {
      return this._httpClient.delete<void>(`api/admin/github/runner/${runnerId}`).toPromise();
    }

    /** Token para `config.sh remove` / `config.cmd remove` na máquina do runner. */
    getRemoveToken(): Promise<GithubRegistrationTokenResponse> {
      return this._httpClient
        .post<GithubRegistrationTokenResponse>('api/admin/github/runner/remove-token', {})
        .toPromise();
    }
}
