import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ChatInboxItem } from './models/chat-inbox.model';

@Component({
  selector: 'app-messages-hub',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './messages-hub.component.html',
  styleUrls: ['./messages-hub.component.scss']
})
export class MessagesHubComponent implements OnInit {
  private readonly http = inject(HttpClient);
  public router = inject(Router);

  inboxItems = signal<ChatInboxItem[]>([]);
  isLoading = signal<boolean>(true);

  get isChatActive(): boolean {
    return this.router.url !== '/messages';
  }

  ngOnInit() {
    this.fetchInbox();
  }

  private fetchInbox() {
    this.http.get<ChatInboxItem[]>('/api/chat/inbox').subscribe({
      next: (items) => {
        this.inboxItems.set(items);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load inbox', err);
        this.isLoading.set(false);
      }
    });
  }
}
