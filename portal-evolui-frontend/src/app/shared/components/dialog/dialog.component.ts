import {Component, Inject, OnInit, ViewEncapsulation} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {DialogConfirmationConfig} from "../../services/message/message-dialog-service";

@Component({
    selector     : 'confirmation-dialog',
    templateUrl  : './dialog.component.html',
    styles       : [
        /* language=SCSS */
        `
            .fuse-confirmation-dialog-panel {
                max-width: 70vw !important;
                max-height: 95vh !important;

                .mat-dialog-container {
                    padding: 0 !important;
                }
            }
        `
    ],
    encapsulation: ViewEncapsulation.None,

    standalone: false
})
export class ConfirmationDialogComponent implements OnInit
{
    /**
     * Constructor
     */
    constructor(
        @Inject(MAT_DIALOG_DATA) public data: DialogConfirmationConfig,
        public matDialogRef: MatDialogRef<ConfirmationDialogComponent>
    )
    {

    }

    // -----------------------------------------------------------------------------------------------------
    // @ Lifecycle hooks
    // -----------------------------------------------------------------------------------------------------

    /**
     * On init
     */
    ngOnInit(): void
    {
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Public methods
    // -----------------------------------------------------------------------------------------------------

}
