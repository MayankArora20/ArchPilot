import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Theme } from '../../services/theme';

@Component({
  selector: 'app-menu',
  imports: [RouterLink],
  templateUrl: './menu.html',
  styleUrl: './menu.scss',
})
export class Menu {
  constructor(public themeService: Theme) {}

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }
}
