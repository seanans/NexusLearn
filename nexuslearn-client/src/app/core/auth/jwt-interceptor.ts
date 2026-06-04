import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TokenStorageService } from './token-storage.service';
import { AuthService } from '../../features/authentication/services/auth.service';
import { catchError, switchMap, throwError, BehaviorSubject, filter, take } from 'rxjs';
import { TokenRefreshResponse } from '../../features/authentication/models/auth.models';

let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenStorage = inject(TokenStorageService);
  const authService = inject(AuthService);
  const http = inject(HttpClient);

  const token = tokenStorage.getAccessToken();
  let authReq = req;

  if (token && !req.url.includes('/api/auth/refresh')) {
    authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/api/auth/login') && !req.url.includes('/api/auth/refresh')) {
        if (!isRefreshing) {
          isRefreshing = true;
          refreshTokenSubject.next(null);

          const refreshToken = tokenStorage.getRefreshToken();
          if (refreshToken) {
            return http.post<TokenRefreshResponse>('/api/auth/refresh', { refreshToken }).pipe(
              switchMap((response) => {
                isRefreshing = false;
                tokenStorage.saveTokens(response.accessToken, response.refreshToken);
                refreshTokenSubject.next(response.accessToken);

                return next(req.clone({
                  setHeaders: { Authorization: `Bearer ${response.accessToken}` }
                }));
              }),
              catchError((err) => {
                isRefreshing = false;
                authService.logout();
                return throwError(() => err);
              })
            );
          } else {
            isRefreshing = false;
            authService.logout();
            return throwError(() => error);
          }
        } else {
          return refreshTokenSubject.pipe(
            filter(newToken => newToken !== null),
            take(1),
            switchMap(newToken => {
              return next(req.clone({
                setHeaders: { Authorization: `Bearer ${newToken}` }
              }));
            })
          );
        }
      }
      return throwError(() => error);
    })
  );
};
