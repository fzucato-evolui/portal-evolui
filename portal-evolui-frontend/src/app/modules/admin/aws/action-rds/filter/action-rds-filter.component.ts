import {Component, EventEmitter, Input, Output, ViewEncapsulation} from '@angular/core';
import {UtilFunctions} from "../../../../../shared/util/util-functions";
import {ClassyLayoutComponent} from "../../../../../layout/layouts/classy/classy.component";
import {VersionConclusionEnum, VersionStatusEnum} from 'app/shared/models/version.model';
import {ActionRdsFilterModel, ActionRDSTypeEnum} from '../../../../../shared/models/action-rds.model';


@Component({
    selector       : 'action-rds-filter',
    templateUrl    : './action-rds-filter.component.html',
    styleUrls      : ['./action-rds-filter.component.scss'],
    encapsulation  : ViewEncapsulation.None,

    standalone: false
})
export class ActionRdsFilterComponent
{
    @Output()
    onFilter: EventEmitter<ActionRdsFilterModel> = new EventEmitter<ActionRdsFilterModel>();

    private _model: ActionRdsFilterModel = new ActionRdsFilterModel();

    ActionRDSTypeEnum = ActionRDSTypeEnum;
    VersionStatusEnum = VersionStatusEnum;
    VersionConclusionEnum = VersionConclusionEnum;

    @Input()
    get model(): ActionRdsFilterModel | null {
        return this._model;
    }
    set model(val: ActionRdsFilterModel | null) {
        if (UtilFunctions.isValidObject(val) === true) {
            this._model = val;
        }
    }

    @Input()
    defaultFilters: ActionRdsFilterModel = null;

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
        this.model = new ActionRdsFilterModel();
    }
}
