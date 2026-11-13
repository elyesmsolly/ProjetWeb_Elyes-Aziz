import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastService } from './core/toast.service';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet],
  template: `
    <router-outlet />
    @if (toast.message(); as m) {
      <div class="toast" [class.error]="m.erreur" role="status" aria-live="polite">{{ m.texte }}</div>
    }`,
})
export class App {
  protected readonly toast = inject(ToastService);
}
