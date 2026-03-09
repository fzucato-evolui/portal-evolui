import {Component, ElementRef, Inject, Input, OnDestroy, Optional, Self} from "@angular/core";
import {AbstractControl, ControlValueAccessor, FormBuilder, FormGroup, NgControl} from "@angular/forms";
import {MAT_FORM_FIELD, MatFormField, MatFormFieldControl} from "@angular/material/form-field";
import {UtilFunctions} from "../../util/util-functions";
import {Subject} from "rxjs";
import {FocusMonitor} from "@angular/cdk/a11y";
import {BooleanInput, coerceBooleanProperty} from "@angular/cdk/coercion";

@Component({
    selector: 'custom-mat',
    templateUrl: 'custom-mat.component.html',
    styleUrls: ['custom-mat.component.scss'],
    providers: [
        {provide: MatFormFieldControl, useExisting: CustomMatComponent}
    ],
    host: {
        '[id]': 'id'
    },

    standalone: false
})
export class CustomMatComponent<T> implements MatFormFieldControl<T>, OnDestroy, ControlValueAccessor {
    static nextId = 0;

    fb: FormGroup;
    stateChanges = new Subject<void>();
    focused = false;
    touched = false;
    controlType = 'custom-mat';
    id = `custom-mat-${CustomMatComponent.nextId++}`;
    onChange = (_: any) => {};
    onTouched = () => {};

    get empty() {

        return UtilFunctions.isValidObject(this.value) === false;
    }

    get shouldLabelFloat() {
        return this.focused || !this.empty;
    }

    @Input('aria-describedby') userAriaDescribedBy: string;

    @Input()
    get placeholder(): string {
        return this._placeholder;
    }
    set placeholder(value: string) {
        this._placeholder = value;
        this.stateChanges.next();
    }
    private _placeholder: string;

    @Input()
    get required(): boolean {
        return this._required;
    }
    set required(value: boolean) {
        this._required = coerceBooleanProperty(value);
        this.stateChanges.next();
    }
    private _required = false;

    @Input()
    get disabled(): boolean {
        return this._disabled;
    }
    set disabled(value: boolean) {
        this._disabled = coerceBooleanProperty(value);
        this._disabled ? this.fb.disable() : this.fb.enable();
        this.stateChanges.next();
    }
    private _disabled = false;

    @Input()
    get value(): T | null {
        if (UtilFunctions.isValidObject(this.fb) === true) {
            return this.fb.value as T;
        }

        return null;
    }
    set value(val: T | null) {
        if (UtilFunctions.isValidObject(val) === true) {
            this.fb.setValue(val);
        }
        this.stateChanges.next();
    }

    get errorState() {
        if (this.ngControl != null && this.ngControl.control) {
            return this.ngControl.control.invalid && this.ngControl.control.touched;
        }

        return this.fb.invalid && this.touched;
    }

    constructor(
        public formBuilder: FormBuilder,
        public focusMonitor: FocusMonitor,
        public elementRef: ElementRef<HTMLElement>,
        @Optional() @Inject(MAT_FORM_FIELD) public _formField: MatFormField,
        @Optional() @Self() public ngControl: NgControl) {

        if (this.ngControl != null) {
            this.ngControl.valueAccessor = this;
        }
    }

    ngOnDestroy() {
        this.stateChanges.complete();
        this.focusMonitor.stopMonitoring(this.elementRef);
    }

    onFocusIn(event: FocusEvent) {
        if (!this.focused) {
            this.focused = true;
            this.stateChanges.next();
        }
    }

    onFocusOut(event: FocusEvent) {
        if (!this.elementRef.nativeElement.contains(event.relatedTarget as Element)) {
            this.touched = true;
            this.focused = false;
            this.onTouched();
            this.stateChanges.next();
        }
    }

    autoFocusNext(control: AbstractControl, nextElement?: HTMLInputElement): void {
        if (!control.errors && nextElement) {
            this.focusMonitor.focusVia(nextElement, 'program');
        }
    }

    autoFocusPrev(control: AbstractControl, prevElement: HTMLInputElement): void {
        if (control.value.length < 1) {
            this.focusMonitor.focusVia(prevElement, 'program');
        }
    }

    setDescribedByIds(ids: string[]) {

    }

    onContainerClick() {

    }

    writeValue(val: T | null): void {
        this.value = val;
    }

    registerOnChange(fn: any): void {
        this.onChange = fn;
    }

    registerOnTouched(fn: any): void {
        this.onTouched = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        this.disabled = isDisabled;
    }

    _handleInput(control: AbstractControl, nextElement?: HTMLInputElement): void {
        this.autoFocusNext(control, nextElement);
        this.onChange(this.value);
    }

    static ngAcceptInputType_disabled: BooleanInput;
    static ngAcceptInputType_required: BooleanInput;

    change($event) {
        this.onChange(this.value);
    }

    readonly autofilled: boolean;

}