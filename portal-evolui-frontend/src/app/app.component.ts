import {Component, OnInit} from '@angular/core';
import {ThemeService} from './shared/services/theme/theme.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  standalone: false
})

export class AppComponent implements OnInit {
  title = 'tw-test';

  constructor(private _themeService: ThemeService) {}

  ngOnInit(): void {
    this._themeService.loadFromStorage();
  }
}
