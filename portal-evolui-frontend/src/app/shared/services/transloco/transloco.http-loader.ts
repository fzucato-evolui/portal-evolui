import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Translation, TRANSLOCO_LOADER, TranslocoLoader} from '@jsverse/transloco';

@Injectable({
    providedIn: 'root'
})
export class TranslocoHttpLoader implements TranslocoLoader
{
    /**
     * Constructor
     */
    constructor(
        private _httpClient: HttpClient)
    {
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Public methods
    // -----------------------------------------------------------------------------------------------------

    /**
     * Get translation
     *
     * @param lang
     */
    getTranslation(lang: string): Observable<Translation>
    {
        return this._httpClient.get<Translation>(`./assets/i18n/${lang}.json`);
    }
}
export const translocoLoader = { provide: TRANSLOCO_LOADER, useClass: TranslocoHttpLoader };
