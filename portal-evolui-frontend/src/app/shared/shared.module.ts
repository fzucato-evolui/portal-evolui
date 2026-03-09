import {NgModule} from '@angular/core';
import {CommonModule, KeyValuePipe} from '@angular/common';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {
  EnumToArrayPipe,
  FilterJsonPipe,
  FormatFileSize,
  FormatNumberPipe,
  FormatTotalFolderFiles,
  FormatTotalFolderSize,
  IsValidStringOrArrayPipe
} from "./pipes/util-functions.pipe";
//import {AuthDirective} from "./directives/auth.directive";
import {
  DateFormatDirective,
  DayMonthFormatDirective,
  MonthYearFormatDirective
} from "./directives/date-format.directive";

import {TranslocoModule} from '@jsverse/transloco';
import {AuthDirective} from "./directives/auth.directive";
import {ConfirmationDialogModule} from "./components/dialog/dialog.module";
import {LoaderInterceptor} from "./interceptors/loader.interceptor";
import {SplashScreenService} from "./services/splash/splash-screen.service";
import {IconsModule} from "./icons/icons.module";


@NgModule({
  declarations: [
    IsValidStringOrArrayPipe,
    EnumToArrayPipe,
    FormatNumberPipe,
    FormatTotalFolderFiles,
    FormatTotalFolderSize,
    FormatFileSize,
    FilterJsonPipe,
    DayMonthFormatDirective,
    DateFormatDirective,
    MonthYearFormatDirective,
    AuthDirective
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    TranslocoModule,
    ConfirmationDialogModule,
    IconsModule,
    KeyValuePipe
  ],
  exports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    TranslocoModule,
    IsValidStringOrArrayPipe,
    EnumToArrayPipe,
    FormatNumberPipe,
    FilterJsonPipe,
    FormatTotalFolderFiles,
    FormatTotalFolderSize,
    FormatFileSize,
    DayMonthFormatDirective,
    DateFormatDirective,
    MonthYearFormatDirective,
    AuthDirective,
    ConfirmationDialogModule,
    IconsModule,
    KeyValuePipe,

  ],
  providers : [
    SplashScreenService,
    LoaderInterceptor
  ]
})
export class SharedModule
{
}
