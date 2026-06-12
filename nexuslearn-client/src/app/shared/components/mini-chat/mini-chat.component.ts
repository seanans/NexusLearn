import { Component, inject, OnInit, OnDestroy, ViewChild, ElementRef, Input, signal } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { ChatConnectionService } from '../../../core/services/chat-connection/chat-connection.service';
import { TokenStorageService } from '../../../core/auth/token-storage.service';
import { FileStorageService } from '../../../features/courses/services/file-storage.service';
import { Subscription } from 'rxjs';
import { PendingAttachmentDto } from '../../../features/courses/models/course.models';

interface ChatMessageDisplay {
  id: string; channelId: string; senderId: string; senderName: string; createdAt: string; content: string; attachments: any[];
}

@Component({
  selector: 'app-mini-chat',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgOptimizedImage],
  templateUrl: './mini-chat.component.html',
  styleUrls: ['./mini-chat.component.scss']
})
export class MiniChatComponent implements OnInit, OnDestroy {
  @Input({ required: true }) referenceId!: string;
  @Input() courseId?: string;
  @Input() channelType?: string;

  private chatService = inject(ChatConnectionService);
  private fileStorageService = inject(FileStorageService);
  private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private tokenStorage = inject(TokenStorageService);

  @ViewChild('viewport') viewport!: ElementRef<HTMLDivElement>;
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  @ViewChild('messageInput') messageInput!: ElementRef<HTMLTextAreaElement>;

  channelId = '';
  myUserId = this.tokenStorage.getUserId() || '';
  messages = signal<ChatMessageDisplay[]>([]);

  chatForm = this.fb.group({ message: [''] });
  stagedAttachments = signal<PendingAttachmentDto[]>([]);
  isUploading = signal<boolean>(false);

  currentPage = 0;
  isLoadingHistory = false;
  hasMoreHistory = true;
  private chatSub?: Subscription;

  private getSafeIsoDate(dateInput: any): string {
    if (!dateInput) return new Date().toISOString();
    if (Array.isArray(dateInput)) {
      return new Date(dateInput[0], dateInput[1] - 1, dateInput[2], dateInput[3] || 0, dateInput[4] || 0, dateInput[5] || 0).toISOString();
    }
    return new Date(dateInput).toISOString();
  }

  ngOnInit() {
    const params: any = {};
    if (this.courseId) params.courseId = this.courseId;
    if (this.channelType) params.type = this.channelType;

    this.http.get<{channelId: string}>(`/api/chat/channels/reference/${this.referenceId}`, { params }).subscribe({
      next: (res) => {
        this.channelId = res.channelId;
        this.loadHistory();
        this.connectToChannel();
      },
      error: (err) => console.error("Could not find chat channel for this entity", err)
    });
  }

  private connectToChannel() {
    this.chatSub = this.chatService.watchChannel(this.channelId).subscribe(message => {
      const parsedMessage = JSON.parse(message.body) as ChatMessageDisplay;

      parsedMessage.createdAt = this.getSafeIsoDate(parsedMessage.createdAt);
      if ((parsedMessage as any).updatedAt) {
        (parsedMessage as any).updatedAt = this.getSafeIsoDate((parsedMessage as any).updatedAt);
      }

      this.messages.update(msgs => {
        if (msgs.some(m => m.id === parsedMessage.id)) return msgs;
        return [parsedMessage, ...msgs];
      });
      if (this.viewport && Math.abs(this.viewport.nativeElement.scrollTop) < 150) {
        this.scrollToBottom();
      }
      this.chatService.markAsRead(this.channelId);
    });
  }

  loadHistory() {
    if (this.isLoadingHistory || !this.hasMoreHistory || !this.channelId) return;
    this.isLoadingHistory = true;

    this.http.get<any>(`/api/chat/channels/${this.channelId}/history?page=${this.currentPage}&size=30`).subscribe({
      next: (response) => {
        const history = response.content;
        this.messages.update(current => {
          const consolidated = [...current, ...history];
          const uniqueMap = new Map<string, ChatMessageDisplay>();
          consolidated.forEach(msg => uniqueMap.set(msg.id, msg));
          return Array.from(uniqueMap.values());
        });

        this.hasMoreHistory = !response.last;
        this.currentPage++;
        this.isLoadingHistory = false;

        if (this.currentPage === 1) {
          this.scrollToBottom();
          this.chatService.markAsRead(this.channelId);
        }
      },
      error: () => this.isLoadingHistory = false
    });
  }

  onScroll(event: Event) {
    const target = event.target as HTMLElement;
    const distanceToTop = target.scrollHeight - Math.abs(target.scrollTop) - target.clientHeight;
    if (distanceToTop < 150) this.loadHistory();
  }

  autoResize(textarea: HTMLTextAreaElement) {
    textarea.style.height = 'auto';
    textarea.style.height = textarea.scrollHeight + 'px';
  }

  get isSendDisabled(): boolean {
    const text = this.chatForm.value.message?.trim();
    const hasFiles = this.stagedAttachments().length > 0;
    return (!text && !hasFiles) || this.isUploading();
  }

  triggerFileInput() { this.fileInput.nativeElement.click(); }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];
    this.isUploading.set(true);

    this.fileStorageService.uploadPending(file).subscribe({
      next: (dto) => {
        this.stagedAttachments.update(c => [...c, dto]);
        this.isUploading.set(false);
        input.value = '';
      },
      error: () => { this.isUploading.set(false); input.value = ''; }
    });
  }

  removeStagedAttachment(index: number) {
    this.stagedAttachments.update(c => c.filter((_, i) => i !== index));
  }

  onEnter(event: any) {
    if (!event.shiftKey) { event.preventDefault(); this.onSubmit(); }
  }

  onSubmit() {
    const content = this.chatForm.get('message')?.value?.trim() || '';
    const attachments = this.stagedAttachments();
    if (!content && attachments.length === 0) return;

    this.chatService.sendMessage(this.channelId, { content, attachments });
    this.chatForm.reset();
    this.stagedAttachments.set([]);
    if (this.messageInput) this.messageInput.nativeElement.style.height = 'auto';
  }

  private scrollToBottom() {
    setTimeout(() => { if (this.viewport) this.viewport.nativeElement.scrollTop = 0; }, 50);
  }

  ngOnDestroy() {
    this.chatSub?.unsubscribe();
  }
}
