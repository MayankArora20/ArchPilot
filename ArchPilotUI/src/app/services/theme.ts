import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class Theme {
  private isDarkMode = new BehaviorSubject<boolean>(false);
  isDarkMode$ = this.isDarkMode.asObservable();

  toggleTheme(): void {
    const newMode = !this.isDarkMode.value;
    this.isDarkMode.next(newMode);
    document.body.classList.toggle('dark-mode', newMode);
  }

  getCurrentTheme(): boolean {
    return this.isDarkMode.value;
  }
}
