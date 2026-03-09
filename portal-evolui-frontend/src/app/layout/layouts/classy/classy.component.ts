import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  ViewEncapsulation
} from '@angular/core';
import {ActivatedRoute, NavigationEnd, Router} from '@angular/router';
import {Observable, Subject} from 'rxjs';
import {filter, takeUntil} from 'rxjs/operators';
import {MediaMatcher} from "@angular/cdk/layout";
import {FormControl} from "@angular/forms";
import {MatDrawerMode} from "@angular/material/sidenav";
import {UsuarioModel} from "../../../shared/models/usuario.model";
import {UserService} from "../../../shared/services/user/user.service";
import {ProjectService} from '../../../modules/admin/project/project.service';
import {ProjectModel} from '../../../shared/models/project.model';

@Component({
  selector     : 'classy-layout',
  templateUrl  : './classy.component.html',
  styleUrls: ['classy.component.scss'],
  encapsulation: ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class ClassyLayoutComponent implements OnInit, OnDestroy
{
  mode = new FormControl('over' as MatDrawerMode);
  link: string;
  isScreenSmall: boolean;
  //navigation: Navigation;
  user: UsuarioModel;
  projects: Array<ProjectModel>;
  mobileQuery: MediaQueryList;
  private _unsubscribeAll: Subject<any> = new Subject<any>();
  private _mobileQueryListener: () => void;
  /**
   * Constructor
   */
  constructor(
    private _activatedRoute: ActivatedRoute,
    public _router: Router,
    private changeDetectorRef: ChangeDetectorRef,
    media: MediaMatcher,
    //private _navigationService: NavigationService,
    public _userService: UserService,
    public projectService: ProjectService
    //private _fuseMediaWatcherService: FuseMediaWatcherService,
    //private _fuseNavigationService: FuseNavigationService
  )
  {
    this.mobileQuery = media.matchMedia('(max-width: 600px)');
    this._mobileQueryListener = () => changeDetectorRef.detectChanges();
    this.mobileQuery.addListener(this._mobileQueryListener);
    /*
    this.link = this._router.routerState.snapshot.url;
    this._router.events.subscribe((s: RouterEvent) => {
      if (s instanceof NavigationEnd) {
        this.link = s.url;
        console.log(this.link);
      }
    });
    */
  }



  // -----------------------------------------------------------------------------------------------------
  // @ Accessors
  // -----------------------------------------------------------------------------------------------------

  /**
   * Getter for current year
   */
  get currentYear(): number
  {
    return new Date().getFullYear();
  }

  // -----------------------------------------------------------------------------------------------------
  // @ Lifecycle hooks
  // -----------------------------------------------------------------------------------------------------

  /**
   * On init
   */
  ngOnInit(): void
  {

    this._router.events.pipe(
      filter(evt => evt instanceof NavigationEnd)
    ).subscribe(() => {
      this.changeDetectorRef.markForCheck();
    });
    this._userService.user$
      .pipe((takeUntil(this._unsubscribeAll)))
      .subscribe((user: UsuarioModel) => {
        this.user = user;
        this.changeDetectorRef.markForCheck();
      });
    this.projectService.model$
      .pipe((takeUntil(this._unsubscribeAll)))
      .subscribe((produtos) => {
        this.projects = produtos;
        this.changeDetectorRef.detectChanges();
      });
    /*
    // Subscribe to navigation data
    this._navigationService.navigation$
        .pipe(takeUntil(this._unsubscribeAll))
        .subscribe((navigation: Navigation) => {
            this.navigation = navigation;
        });

    // Subscribe to the user service
    this._userService.user$
        .pipe((takeUntil(this._unsubscribeAll)))
        .subscribe((user: UsuarioModel) => {
            this.user = user;
        });

    // Subscribe to media changes
    this._fuseMediaWatcherService.onMediaChange$
        .pipe(takeUntil(this._unsubscribeAll))
        .subscribe(({matchingAliases}) => {

            // Check if the screen is small
            this.isScreenSmall = !matchingAliases.includes('md');
        });
        */
  }

  /**
   * On destroy
   */
  ngOnDestroy(): void
  {
    this.mobileQuery.removeListener(this._mobileQueryListener);
    // Unsubscribe from all subscriptions
    this._unsubscribeAll.next(undefined);
    this._unsubscribeAll.complete();
  }

  // -----------------------------------------------------------------------------------------------------
  // @ Public methods
  // -----------------------------------------------------------------------------------------------------

  /**
   * Toggle navigation
   *
   * @param name
   */
  toggleNavigation(name: string): void
  {
    /*
      // Get the navigation
      const navigation = this._fuseNavigationService.getComponent<FuseVerticalNavigationComponent>(name);

      if ( navigation )
      {
          // Toggle the opened status
          navigation.toggle();
      }
      */
  }

  noImage() {
    this.user.image = null;
    this.changeDetectorRef.markForCheck();
  }

  changeMenu(path: string) {
    this.link = path;
    const me = this;
    setTimeout(function() {
      me.changeDetectorRef.detectChanges();
      me.changeDetectorRef.markForCheck();
    } , 500);
  }

  isCurrentPath(path: string) : Observable<boolean> {

    return null;
  }

  isLinkActive(link) {
    const url = this._router.url;
    return url.includes(link);
  }
}
