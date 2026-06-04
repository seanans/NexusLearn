import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { TokenStorageService } from '../../../core/auth/token-storage.service';
import { LoginRequest, JwtResponse } from '../models/auth.models';
import {Router} from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly router = inject(Router);

  readonly isAuthenticated = signal<boolean>(!!this.tokenStorage.getAccessToken());

  login(credentials: LoginRequest): Observable<JwtResponse> {
    return this.http.post<JwtResponse>('/api/auth/login', credentials).pipe(
      tap((response) => {
        this.tokenStorage.saveTokens(response.accessToken, response.refreshToken);
        this.isAuthenticated.set(true);
      })
    );
  }

  logout(): void {
    this.tokenStorage.clearTokens();
    this.isAuthenticated.set(false);
    this.router.navigate(['/login']);
  }

  register(data: { firstName: string; lastName: string; email: string; password: string }): Observable<any> {
    return this.http.post('/api/auth/register', data);
  }
}
