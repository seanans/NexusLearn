import { Injectable, inject, effect, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RxStomp } from '@stomp/rx-stomp';
import { AuthService } from '../../../features/authentication/services/auth.service';
import { TokenStorageService } from '../../auth/token-storage.service';
import { myRxStompConfig } from './chat-stomp.config';
import { take } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ChatConnectionService implements OnDestroy {
  private rxStomp: RxStomp = new RxStomp();

  private readonly authService = inject(AuthService);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly http = inject(HttpClient);

  constructor() {
    effect(() => {
      if (this.authService.isAuthenticated()) {
        this.initializeConnection();
      } else {
        this.disconnect();
      }
    });
  }

  private initializeConnection() {
    const token = this.tokenStorage.getAccessToken();
    if (!token) return;

    this.rxStomp.configure(myRxStompConfig(token));

    this.rxStomp.stompErrors$.subscribe(errorFrame => {
      if (errorFrame.headers['message']?.includes('Access Denied') ||
        errorFrame.headers['message']?.includes('Expired')) {

        this.rxStomp.deactivate();

        this.http.post<{accessToken: string, refreshToken: string}>('/api/auth/refresh', {
          refreshToken: this.tokenStorage.getRefreshToken()
        }).pipe(take(1)).subscribe({
          next: (tokens) => {
            this.tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken);
            this.rxStomp.configure(myRxStompConfig(tokens.accessToken));
            this.rxStomp.activate();
          },
          error: () => this.authService.logout()
        });
      }
    });

    this.rxStomp.activate();
  }

  public watchChannel(channelId: string) {
    return this.rxStomp.watch(`/topic/channels/${channelId}`);
  }

  public sendMessage(channelId: string, payload: any) {
    this.rxStomp.publish({
      destination: `/app/chat/channels/${channelId}`,
      body: JSON.stringify(payload)
    });
  }

  public markAsRead(channelId: string) {
    this.http.post(`/api/chat/channels/${channelId}/read`, {}).subscribe({
      error: err => console.error('Failed to mark channel as read', err)
    });
  }

  public disconnect() {
    this.rxStomp.deactivate();
  }

  ngOnDestroy() {
    this.disconnect();
  }
}
