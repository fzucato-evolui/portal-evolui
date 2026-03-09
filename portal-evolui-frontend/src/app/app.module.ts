import {LOCALE_ID, NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';

import {LayoutModule} from "./layout/layout.module";
import {AuthModule} from "./modules/auth/auth.module";
import {RootModule} from "./shared/services/root/root.module";
import {MAT_DATE_LOCALE, MATERIAL_SANITY_CHECKS} from "@angular/material/core";
import {HTTP_INTERCEPTORS} from "@angular/common/http";
import {LoaderInterceptor} from "./shared/interceptors/loader.interceptor";
import {MAT_FORM_FIELD_DEFAULT_OPTIONS} from "@angular/material/form-field";
import localePt from '@angular/common/locales/pt';
import {registerLocaleData} from "@angular/common";
import {MatPaginatorIntl} from '@angular/material/paginator';
import {getPortuguesePaginatorIntl} from './shared/material/mat-paginator-intl-pt';
import {provideTransloco} from '@jsverse/transloco';
import {TranslocoHttpLoader} from './shared/services/transloco/transloco.http-loader';

registerLocaleData(localePt);

@NgModule({
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    RootModule.forRoot(),
    LayoutModule,
    AuthModule
  ],
  declarations: [AppComponent],
  bootstrap: [AppComponent],
  providers: [
    { provide: MAT_DATE_LOCALE, useValue: 'pt-br' },
    {provide: HTTP_INTERCEPTORS, useClass: LoaderInterceptor, multi: true},
    {
      provide: LOCALE_ID,
      useValue: 'pt-BR'
    },
    {
      // Disable 'theme' sanity check
      provide : MATERIAL_SANITY_CHECKS,
      useValue: {
        doctype: true,
        theme  : false,
        version: true
      }
    },
    {
      // Use the 'fill' appearance on Angular Material form fields by default
      provide : MAT_FORM_FIELD_DEFAULT_OPTIONS,
      useValue: {
        appearance: 'fill'
      }
    },
    {  provide: MatPaginatorIntl, useValue: getPortuguesePaginatorIntl() },
    provideTransloco({
      config: {
        reRenderOnLangChange: true,
        availableLangs: ['pt-br'],
        defaultLang: 'pt-br',
        fallbackLang: 'pt-br'
      },
      loader: TranslocoHttpLoader
    })
  ]
})
export class AppModule { }
