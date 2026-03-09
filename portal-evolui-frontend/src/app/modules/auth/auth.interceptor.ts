import {Injectable} from '@angular/core';
import {HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {Observable, throwError} from 'rxjs';
import {catchError} from 'rxjs/operators';
import {UtilFunctions} from "../../shared/util/util-functions";
import {UserService} from "../../shared/services/user/user.service";
import {DialogConfirmationConfig, MessageDialogService} from "../../shared/services/message/message-dialog-service";

@Injectable()
export class AuthInterceptor implements HttpInterceptor
{
    /**
     * Constructor
     */
    constructor(private _userService: UserService, private _messageService: MessageDialogService)
    {
    }

    /**
     * Intercept
     *
     * @param req
     * @param next
     */
    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>>
    {
        // Clone the request object
        let newReq = req.clone({
          withCredentials: true
        });

        // Request
        //
        // If the access token didn't expire, add the Authorization header.
        // We won't add the Authorization header if the access token expired.
        // This will force the server to return a "401 Unauthorized" response
        // for the protected API routes which our response interceptor will
        // catch and delete the access token from the local storage while logging
        // the user out from the app.
        if ( UtilFunctions.isValidStringOrArray(this._userService.accessToken) === true)
        {
            newReq = req.clone({
                headers: req.headers.set('Authorization', 'Bearer ' + this._userService.accessToken),
                withCredentials: true
            });
        }

        //console.log('interceptor', newReq, newReq.withCredentials);
        // Response
        return next.handle(newReq).pipe(
            catchError((error) => {

                console.log(error);
                // Catch "401 Unauthorized" responses
                if ( error instanceof HttpErrorResponse && error.status === 401 )
                {
                    // Sign out
                    this._userService.signOut();

                    // Reload the app
                    location.replace("/sign-in");
                    return;
                }
                console.error(error);
                this.showWarning(error);

                return throwError(error);
            })
        );
    }

  private showWarning(error) {
    const config: DialogConfirmationConfig = {
      title      : 'ALERTA',
      message    : (UtilFunctions.getHttpErrorMessage(error) as string).replace(new RegExp('\r?\n','g'), '<br />'),
      icon       : {
        show : true,
        name : 'fa-exclamation-circle',
        color: 'warn'
      },
      actions    : {
        confirm: {
          show : true,
          color: 'warn',
          label: 'OK'
        },
        cancel : {
          show : false,
        }
      },
      dismissible: false
    };

    const dialogRef = this._messageService.openDialog(config);

    // Subscribe to afterClosed from the dialog reference
    dialogRef.afterClosed().subscribe((result) => {
      //console.log(result);
    });
  }


}
