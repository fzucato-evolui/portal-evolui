import {Injectable} from '@angular/core';
import {UserConfigModel} from '../../models/usuario.model';

const STORAGE_KEY = 'userThemeConfig';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {

  private _mediaQuery: MediaQueryList;
  private _mediaListener: (e: MediaQueryListEvent) => void;

  constructor() {
    this._mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
  }

  applyTheme(theme: string): void {
    const body = document.body;
    const classes = Array.from(body.classList).filter(c => c.startsWith('theme-'));
    classes.forEach(c => body.classList.remove(c));
    if (theme && theme !== 'default') {
      body.classList.add(`theme-${theme}`);
    }
  }

  applyScheme(scheme: string): void {
    const body = document.body;
    const html = document.documentElement;

    // Remove listener anterior (auto mode)
    if (this._mediaListener) {
      this._mediaQuery.removeEventListener('change', this._mediaListener);
      this._mediaListener = null;
    }

    body.classList.remove('dark', 'light');
    html.classList.remove('dark', 'light');

    if (scheme === 'auto') {
      this._mediaListener = (e: MediaQueryListEvent) => {
        body.classList.remove('dark', 'light');
        html.classList.remove('dark', 'light');
        const s = e.matches ? 'dark' : 'light';
        body.classList.add(s);
        html.classList.add(s);
      };
      this._mediaQuery.addEventListener('change', this._mediaListener);
      const resolved = this._mediaQuery.matches ? 'dark' : 'light';
      body.classList.add(resolved);
      html.classList.add(resolved);
    } else {
      const s = scheme === 'dark' ? 'dark' : 'light';
      body.classList.add(s);
      html.classList.add(s);
    }
  }

  apply(config: UserConfigModel): void {
    if (!config) {
      config = {scheme: 'light', theme: 'default'};
    }
    this.applyScheme(config.scheme || 'light');
    this.applyTheme(config.theme || 'default');
    this.saveToStorage(config);
  }

  loadFromStorage(): void {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const config: UserConfigModel = JSON.parse(raw);
        this.applyScheme(config.scheme || 'light');
        this.applyTheme(config.theme || 'default');
      } else {
        this.applyScheme('light');
      }
    } catch {
      this.applyScheme('light');
    }
  }

  private saveToStorage(config: UserConfigModel): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(config));
  }

  clearStorage(): void {
    localStorage.removeItem(STORAGE_KEY);
  }
}
