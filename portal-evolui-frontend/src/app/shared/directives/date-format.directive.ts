import {MAT_DATE_FORMATS} from '@angular/material/core';
import {Directive} from '@angular/core';

export const FORMAT = {
    parse: {
        dateInput: 'DD/MM/YYYY',
    },
    display: {
        dateInput: 'DD/MM/YYYY',
        monthYearLabel: 'MMM YYYY',
        dateA11yLabel: 'LL',
        monthYearA11yLabel: 'MMMM YYYY',
    },
};
@Directive({
    selector: '[appDateFormat]',
    providers: [
        { provide: MAT_DATE_FORMATS, useValue: FORMAT },
    ],

    standalone: false
})
export class DateFormatDirective {
}

export const FORMATMONTHYEAR = {
    parse: {
        dateInput: 'MM/YYYY',
    },
    display: {
        dateInput: 'MM/YYYY',
        monthYearLabel: 'MMM YYYY',
        dateA11yLabel: 'LL',
        monthYearA11yLabel: 'MMMM YYYY',
    },
};
@Directive({
    selector: '[appMonthYearFormat]',
    providers: [
        { provide: MAT_DATE_FORMATS, useValue: FORMATMONTHYEAR },
    ],

    standalone: false
})
export class MonthYearFormatDirective {
}

export const FORMATDAYMONTH = {
    parse: {
        dateInput: 'DD/MM',
    },
    display: {
        dateInput: 'DD MMM',
        monthYearLabel: 'DD MMM',
        dateA11yLabel: 'LL',
        monthYearA11yLabel: 'DD MMMM',
    },
};
@Directive({
    selector: '[appDayMonthFormat]',
    providers: [
        { provide: MAT_DATE_FORMATS, useValue: FORMATDAYMONTH },
    ],

    standalone: false
})
export class DayMonthFormatDirective {
}