import {Directive, ElementRef, NgZone, OnDestroy, OnInit} from '@angular/core';

/**
 * Enables horizontal drag-to-scroll on any overflow container using the mouse.
 * Useful for tables that overflow horizontally on narrow desktop screens
 * where there is no touch scroll and the scrollbar is far at the bottom.
 *
 * Usage: <div dragScroll class="overflow-x-auto"> ... </div>
 */
@Directive({
  selector: '[dragScroll]',
  standalone: false
})
export class DragScrollDirective implements OnInit, OnDestroy {
  private _el: HTMLElement;
  private _isDown = false;
  private _startX = 0;
  private _scrollLeft = 0;
  private _moved = false;

  private _onMouseDown = (e: MouseEvent) => this.onMouseDown(e);
  private _onMouseMove = (e: MouseEvent) => this.onMouseMove(e);
  private _onMouseUp = () => this.onMouseUp();
  private _onMouseLeave = () => this.onMouseUp();

  constructor(private _elementRef: ElementRef<HTMLElement>, private _ngZone: NgZone) {
    this._el = this._elementRef.nativeElement;
  }

  ngOnInit(): void {
    // Run outside Angular zone to avoid unnecessary change detection
    this._ngZone.runOutsideAngular(() => {
      this._el.addEventListener('mousedown', this._onMouseDown);
      this._el.addEventListener('mousemove', this._onMouseMove);
      this._el.addEventListener('mouseup', this._onMouseUp);
      this._el.addEventListener('mouseleave', this._onMouseLeave);
    });
  }

  ngOnDestroy(): void {
    this._el.removeEventListener('mousedown', this._onMouseDown);
    this._el.removeEventListener('mousemove', this._onMouseMove);
    this._el.removeEventListener('mouseup', this._onMouseUp);
    this._el.removeEventListener('mouseleave', this._onMouseLeave);
  }

  private onMouseDown(e: MouseEvent): void {
    // Only activate if there's actually horizontal overflow
    if (this._el.scrollWidth <= this._el.clientWidth) return;

    this._isDown = true;
    this._moved = false;
    this._startX = e.pageX - this._el.offsetLeft;
    this._scrollLeft = this._el.scrollLeft;
    this._el.style.cursor = 'grab';
  }

  private onMouseMove(e: MouseEvent): void {
    if (!this._isDown) return;
    e.preventDefault();
    const x = e.pageX - this._el.offsetLeft;
    const walk = (x - this._startX) * 1.5; // Scroll speed multiplier
    this._el.scrollLeft = this._scrollLeft - walk;
    this._moved = true;
    this._el.style.cursor = 'grabbing';
  }

  private onMouseUp(): void {
    if (!this._isDown) return;
    this._isDown = false;
    this._el.style.cursor = '';
  }
}
