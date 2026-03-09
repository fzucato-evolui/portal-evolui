import {Component, Inject, OnDestroy, OnInit, ViewEncapsulation} from '@angular/core';
import {DOCUMENT} from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {Subject} from 'rxjs';
import {Layout} from "./layout.types";

@Component({
    selector     : 'layout',
    templateUrl  : './layout.component.html',
    styleUrls    : ['./layout.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class LayoutComponent implements OnInit, OnDestroy
{
    layout: Layout | undefined;
    private _unsubscribeAll: Subject<any> = new Subject<any>();

    /**
     * Constructor
     */
    constructor(
        private _activatedRoute: ActivatedRoute,
        @Inject(DOCUMENT) private _document: any,
        private _router: Router
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
      this._updateLayout();
    }

    /**
     * On destroy
     */
    ngOnDestroy(): void
    {
        // Unsubscribe from all subscriptions
        this._unsubscribeAll.next(undefined);
        this._unsubscribeAll.complete();
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Private methods
    // -----------------------------------------------------------------------------------------------------

    /**
     * Update the selected layout
     */
    private _updateLayout(): void
    {
        // Get the current activated route
        let route = this._activatedRoute;
        while ( route.firstChild )
        {
            route = route.firstChild;
        }

        const layoutFromQueryParam = (route.snapshot.queryParamMap.get('layout') as Layout);
        if ( layoutFromQueryParam )
        {
            this.layout = layoutFromQueryParam;
        }

        const paths = route.pathFromRoot;
        paths.forEach((path) => {

            // Check if there is a 'layout' data
            if ( path.routeConfig && path.routeConfig.data && path.routeConfig.data.layout )
            {
                // Set the layout
                this.layout = path.routeConfig.data.layout;
            }
        });
    }
}
