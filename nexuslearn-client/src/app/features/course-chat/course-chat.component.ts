import { Component, inject, OnInit, signal, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { ChatConnectionService } from '../../core/services/chat-connection/chat-connection.service';
import { TokenStorageService } from '../../core/auth/token-storage.service';
import { FileStorageService } from '../courses/services/file-storage.service';
import { Subscription } from 'rxjs';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { PendingAttachmentDto } from '../courses/models/course.models';

interface ChatMessageDisplay {
  id: string;
  channelId: string;
  senderId: string;
  senderName: string;
  createdAt: string;
  content: string;
  attachments: any[];
}

@Component({
  selector: 'app-course-chat',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, NgOptimizedImage],
  templateUrl: './course-chat.component.html',
  styleUrls: ['./course-chat.component.scss']
})
export class CourseChatComponent implements OnInit, OnDestroy {
  private chatService = inject(ChatConnectionService);
  private fileStorageService = inject(FileStorageService);
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);
  private tokenStorage = inject(TokenStorageService);
  public router = inject(Router);

  @ViewChild('viewport') viewport!: ElementRef<HTMLDivElement>;
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  @ViewChild('messageInput') messageInput!: ElementRef<HTMLTextAreaElement>;

  messages = signal<ChatMessageDisplay[]>([]);
  channelId = '';
  channelName = '';
  channelType = '';
  courseId = '';
  referenceId = '';
  pinnedLink = '';

  canSend = signal<boolean>(true);

  private chatSub?: Subscription;
  private paramSub?: Subscription;

  stagedAttachments = signal<PendingAttachmentDto[]>([]);
  isUploading = signal<boolean>(false);
  myUserId: string = '';
  chatForm = this.fb.group({ message: [''] });

  currentPage = 0;
  isLoadingHistory = false;
  hasMoreHistory = true;

  private getSafeIsoDate(dateInput: any): string {
    if (!dateInput) return new Date().toISOString();
    if (Array.isArray(dateInput)) {
      return new Date(dateInput[0], dateInput[1] - 1, dateInput[2], dateInput[3] || 0, dateInput[4] || 0, dateInput[5] || 0).toISOString();
    }
    return new Date(dateInput).toISOString();
  }

  ngOnInit() {
    this.myUserId = this.tokenStorage.getUserId() || '';
    this.paramSub = this.route.paramMap.subscribe(params => {
      const newChannelId = params.get('channelId') || params.get('courseId') || '';
      if (newChannelId && newChannelId !== this.channelId) {
        this.resetAndRebuildChat(newChannelId);
      }
    });
  }

  private resetAndRebuildChat(newChannelId: string) {
    if (this.chatSub) this.chatSub.unsubscribe();

    this.channelId = newChannelId;
    const state = window.history.state;
    this.channelName = state?.channelName || state?.courseName || 'Discussion';
    this.channelType = state?.channelType || '';
    this.courseId = state?.courseId || '';
    this.referenceId = state?.referenceId || '';

    if (this.channelType === 'LESSON' && this.referenceId) {
      this.pinnedLink = `/courses/${this.courseId}/lessons/${this.referenceId}`;
    } else if (this.channelType.startsWith('ASSIGNMENT') && this.referenceId) {
      this.pinnedLink = `/courses/${this.courseId}/assignments/${this.referenceId}`;
    } else if (this.channelType === 'COURSE' || this.channelType === 'ANNOUNCEMENT') {
      this.pinnedLink = `/courses/${this.courseId}/syllabus`;
    } else {
      this.pinnedLink = '';
    }

    if (this.channelType === 'ANNOUNCEMENT' && this.courseId) {
      this.http.get<any>(`/api/courses/${this.courseId}`).subscribe({
        next: (course) => {
          this.canSend.set(course.currentUserRole === 'TEACHER' || course.currentUserRole === 'ASSISTANT');
        },
        error: () => this.canSend.set(false)
      });
    } else {
      this.canSend.set(true);
    }

    this.messages.set([]);
    this.currentPage = 0;
    this.hasMoreHistory = true;
    this.isLoadingHistory = false;

    if (this.messageInput) {
      this.messageInput.nativeElement.style.height = 'auto';
    }

    this.loadHistory();

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

  goBack() {
    this.router.navigate(['/messages']);
  }

  loadHistory() {
    if (this.isLoadingHistory || !this.hasMoreHistory) return;
    this.isLoadingHistory = true;

    this.http.get<any>(`/api/chat/channels/${this.channelId}/history?page=${this.currentPage}&size=50`).subscribe({
      next: (response) => {
        const history = response.content;

        this.messages.update(currentMessages => {
          const consolidated = [...currentMessages, ...history];
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
      error: (err) => {
        console.error("Failed to load chat history:", err);
        this.isLoadingHistory = false;
      }
    });
  }

  onScroll(event: Event) {
    const target = event.target as HTMLElement;
    const distanceToTop = target.scrollHeight - Math.abs(target.scrollTop) - target.clientHeight;

    if (distanceToTop < 150) {
      this.loadHistory();
    }
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

  triggerFileInput() {
    this.fileInput.nativeElement.click();
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    this.isUploading.set(true);

    this.fileStorageService.uploadPending(file).subscribe({
      next: (attachmentDto) => {
        this.stagedAttachments.update(current => [...current, attachmentDto]);
        this.isUploading.set(false);
        input.value = '';
      },
      error: (err) => {
        console.error('File upload failed', err);
        this.isUploading.set(false);
        input.value = '';
      }
    });
  }

  removeStagedAttachment(index: number) {
    this.stagedAttachments.update(current => current.filter((_, i) => i !== index));
  }

  onEnter(event: any) {
    if (!event.shiftKey) {
      event.preventDefault();
      this.onSubmit();
    }
  }

  onSubmit() {
    if (!this.canSend()) return;

    const content = this.chatForm.get('message')?.value?.trim() || '';
    const attachments = this.stagedAttachments();

    if (!content && attachments.length === 0) return;

    this.chatService.sendMessage(this.channelId, { content, attachments });

    this.chatForm.reset();
    this.stagedAttachments.set([]);

    if (this.messageInput) {
      this.messageInput.nativeElement.style.height = 'auto';
    }
  }

  private scrollToBottom() {
    setTimeout(() => {
      if (this.viewport) {
        this.viewport.nativeElement.scrollTop = 0;
      }
    }, 50);
  }

  ngOnDestroy() {
    this.chatSub?.unsubscribe();
    this.paramSub?.unsubscribe();
  }
}
