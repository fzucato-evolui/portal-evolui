import {Component, Input, OnDestroy, OnInit, ViewChild, ViewEncapsulation} from "@angular/core";
import {Subject} from "rxjs";
import {MatDrawer, MatDrawerMode} from "@angular/material/sidenav";
import {ClassyLayoutComponent} from "../../../layout/layouts/classy/classy.component";

@Component({
    selector       : 'admin-container',
    templateUrl    : './admin-container.component.html',
    encapsulation  : ViewEncapsulation.None,

    standalone: false
})
export class AdminContainerComponent implements OnInit, OnDestroy{
    @ViewChild('matDrawerRight', {static: false}) sidenavRight: MatDrawer;
    @Input()
    title: string;
    @Input()
    showRightMenu: boolean =  true;
    @Input()
    rightMenuMode: MatDrawerMode;
    @Input()
    rightMenuOpened: boolean = false;
    @Input()
    rightMenuClass: string = '';
    @Input()
    rightMenuButtonIcon: string = 'heroicons_outline:menu';
    @Input()
    rightMenuButtonClass: string = '';

    isScreenSmall: boolean = false;
    private _unsubscribeAll: Subject<any> = new Subject<any>();

    constructor(private _parent: ClassyLayoutComponent) {

    }

    ngOnInit(): void {
        // Subscribe to media changes

    }

    ngOnDestroy(): void {
        // Unsubscribe from all subscriptions
        this._unsubscribeAll.next(undefined);
        this._unsubscribeAll.complete();
    }

    toggleMatRight() {
        this.sidenavRight.toggle();
    }

    openedMatdrawer($event: boolean) {

    }
}
