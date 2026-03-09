import {AfterViewInit, Component, ElementRef, EventEmitter, Inject, Input, Optional, Output, Self} from "@angular/core";
import {FormBuilder, NgControl} from "@angular/forms";
import {MAT_FORM_FIELD, MatFormField, MatFormFieldControl} from "@angular/material/form-field";
import {UtilFunctions} from "../../util/util-functions";
import {FocusMonitor} from "@angular/cdk/a11y";
import {CustomMatComponent} from "../custom-mat-component/custom-mat.component";

@Component({
    selector: 'lookup-input',
    templateUrl: 'lookup-input.component.html',
    styleUrls: ['lookup-input.component.scss'],
    providers: [
        {provide: MatFormFieldControl, useExisting: LookupInputComponent}
    ],
    host: {
        '[id]': 'id'
    },

    standalone: false
})
export class LookupInputComponent extends CustomMatComponent<any> implements AfterViewInit {
    @Input()
    get fastSearchClass(): string {
        return this._fastSearchClass;
    }

    set fastSearchClass(value: string) {
        this._fastSearchClass = value;
    }

    @Input()
    get fastSearchName(): string {
        return this._fastSearchName;
    }

    set fastSearchName(value: string) {
        this._fastSearchName = value;
    }

    @Input()
    get fastSearchMask(): string {
        return this._fastSearchMask;
    }

    set fastSearchMask(value: string) {
        this._fastSearchMask = value;
    }

    @Input()
    get descriptionClass(): string {
        return this._descriptionClass;
    }

    set descriptionClass(value: string) {
        this._descriptionClass = value;
    }

    @Input()
    get descriptionName(): string {
        return this._descriptionName;
    }

    set descriptionName(value: string) {
        this._descriptionName = value;
    }

    private _fastSearchClass = '';
    private _fastSearchName = '';
    private _fastSearchMask = '';
    private _descriptionClass = '';
    private _descriptionName = '';

    @Output()
    onFastKeySearch: EventEmitter<string> = new EventEmitter<string>();

    @Output()
    onSearch: EventEmitter<any> = new EventEmitter<any>();

    controlType = 'lookup-input';
    id = `lookup-input-${CustomMatComponent.nextId++}`;

    private _value: any;

    @Input()
    get value(): any | null {
        return this._value;
    }
    set value(val: any | null) {
        if (UtilFunctions.isValidObject(val) === true) {
            let description = val;
            if (this._descriptionName.includes('.') === true) {
                let myarray = this._descriptionName.split('.');

                for(let i = 0; i < myarray.length; i++)
                {
                    description = description[myarray[i]];
                    //console.log(description);
                }
            } else {
                description = val[this._descriptionName];
            }
            this.fb.setValue({
                fastSearch: val[this._fastSearchName],
                description: description

            });
        }
        this._value = val;
        this.stateChanges.next();
    }

    constructor(
        formBuilder: FormBuilder,
        public focusMonitor: FocusMonitor,
        public elementRef: ElementRef<HTMLElement>,
        @Optional() @Inject(MAT_FORM_FIELD) public _formField: MatFormField,
        @Optional() @Self() public ngControl: NgControl) {

        super(formBuilder, focusMonitor, elementRef, _formField, ngControl);

        this.fb = formBuilder.group({
            fastSearch: [''],
            description: ['']
        }, { validators: [] });

    }
    setDescribedByIds(ids: string[]) {
        const selector = ".lookup-input-container";
        const controlElement = this.elementRef.nativeElement
            .querySelector(selector)!;
        controlElement.setAttribute('aria-describedby', ids.join(' '));
    }

    writeValue(val: any | null): void {
        this.value = val;
    }


    ngAfterViewInit(): void {
        if (this.ngControl != null && this.ngControl.control) {

        }
    }

    setFastKey($event) {
        this._value = null;
        this.fb.get('description').setValue(null);
        this.onFastKeySearch.emit(this.fb.value.fastSearch);
    }

    setSearch() {
        this.onSearch.emit();
    }
}
