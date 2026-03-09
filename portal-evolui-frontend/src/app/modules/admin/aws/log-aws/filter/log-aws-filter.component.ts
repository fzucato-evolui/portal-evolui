import {Component, EventEmitter, Input, Output, ViewEncapsulation} from '@angular/core';
import {UtilFunctions} from "../../../../../shared/util/util-functions";
import {ClassyLayoutComponent} from "../../../../../layout/layouts/classy/classy.component";
import {LogAWSActionFilterModel, LogAWSActionTypeEnum} from "../../../../../shared/models/log.aws.action.model";


@Component({
    selector       : 'log-aws-filter',
    templateUrl    : './log-aws-filter.component.html',
    styleUrls      : ['./log-aws-filter.component.scss'],
    encapsulation  : ViewEncapsulation.None,

    standalone: false
})
export class LogAwsFilterComponent
{
    @Output()
    onFilter: EventEmitter<LogAWSActionFilterModel> = new EventEmitter<LogAWSActionFilterModel>();

    private _model: LogAWSActionFilterModel = new LogAWSActionFilterModel();

    LogAWSActionTypeEnum = LogAWSActionTypeEnum;

    @Input()
    get model(): LogAWSActionFilterModel | null {
        return this._model;
    }
    set model(val: LogAWSActionFilterModel | null) {
        if (UtilFunctions.isValidObject(val) === true) {
            this._model = val;
        }
    }

    @Input()
    defaultFilters: LogAWSActionFilterModel = null;

    /**
     * Constructor
     */
    constructor(public parent: ClassyLayoutComponent)
    {
    }

    filtrar() {
        this.onFilter.emit(this._model);
    }

    limpar() {
        this.model = new LogAWSActionFilterModel();
    }
}
