import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { ToastOutletComponent } from './shared/components/toast/toast-outlet.component';

@Component({
  selector: 'finflow-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, ToastOutletComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent {
  private readonly router = inject(Router);

  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  );

  protected readonly showNavbar = computed(() => {
    const url = this.currentUrl();
    return !(url.startsWith('/login') || url.startsWith('/signup'));
  });
}
