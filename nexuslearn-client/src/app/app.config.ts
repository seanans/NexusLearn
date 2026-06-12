import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { IMAGE_CONFIG } from '@angular/common';
import { routes } from './app.routes';
import { jwtInterceptor } from './core/auth/jwt-interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    {
      provide: IMAGE_CONFIG,
      useValue: {
        domains: ['localhost', '127.0.0.1']
      }
    }
  ]
};
