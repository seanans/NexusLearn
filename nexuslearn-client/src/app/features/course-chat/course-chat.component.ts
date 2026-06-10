import { Component, inject, OnInit, signal, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { ScrollingModule, CdkVirtualScrollViewport } from '@angular/cdk/scrolling';
import { ChatConnectionService } from '../../core/services/chat-connection/chat-connection.service';
import { TokenStorageService } from '../../core/auth/token-storage.service';
import { FileStorageService } from '../courses/services/file-storage.service';
import { Subscription } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { Router } from '@angular/router';
import { PendingAttachmentDto } from '../courses/models/course.models';

interface ChatMessageDisplay {
  id: string;
  senderId: string;
  senderName: string;
  createdAt: string;
  content: string;
  attachments: any[];
}

@Component({
  selector: 'app-course-chat',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ScrollingModule],
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

  @ViewChild(CdkVirtualScrollViewport) viewport!: CdkVirtualScrollViewport;
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  messages = signal<ChatMessageDisplay[]>([]);
  courseId = '';
  courseTitle: string = '';
  private chatSub?: Subscription;
  private paramSub?: Subscription;

  stagedAttachments = signal<PendingAttachmentDto[]>([]);
  isUploading = signal<boolean>(false);
  myUserId: string = '';
  chatForm = this.fb.group({ message: [''] });

  currentPage = 0;
  isLoadingHistory = false;
  hasMoreHistory = true;

  ngOnInit() {
    this.myUserId = this.tokenStorage.getUserId() || '';
    this.paramSub = this.route.paramMap.subscribe(params => {
      const newCourseId = params.get('courseId') || '';
      if (newCourseId && newCourseId !== this.courseId) {
        this.resetAndRebuildChat(newCourseId);
      }
    });
  }

  private resetAndRebuildChat(newCourseId: string) {
    if (this.chatSub) {
      this.chatSub.unsubscribe();
    }

    this.courseId = newCourseId;
    const state = window.history.state;
    this.courseTitle = state?.courseName || 'Course Discussion';
    this.messages.set([]);
    this.currentPage = 0;
    this.hasMoreHistory = true;
    this.isLoadingHistory = false;

    this.loadHistory();

    this.chatSub = this.chatService.watchCourseChat(this.courseId).subscribe(message => {
      const parsedMessage = JSON.parse(message.body) as ChatMessageDisplay;

      this.messages.update(msgs => {
        if (msgs.some(m => m.id === parsedMessage.id)) return msgs;
        return [...msgs, parsedMessage];
      });
      this.scrollToBottom();
    });
  }

  goBack() {
    this.router.navigate(['/messages']);
  }

  loadHistory() {
    if (this.isLoadingHistory || !this.hasMoreHistory) return;
    this.isLoadingHistory = true;

    this.http.get<any>(`/api/chat/courses/${this.courseId}/history?page=${this.currentPage}&size=50`).subscribe({
      next: (response) => {
        const history = response.content.slice().reverse();
        this.messages.update(currentMessages => {
          const consolidated = [...history, ...currentMessages];
          const uniqueMap = new Map<string, ChatMessageDisplay>();
          consolidated.forEach(msg => uniqueMap.set(msg.id, msg));
          return Array.from(uniqueMap.values());
        });
        this.hasMoreHistory = !response.last;
        this.currentPage++;
        this.isLoadingHistory = false;

        if (this.currentPage === 1) this.scrollToBottom();
      },
      error: (err) => {
        console.error("Failed to load chat history:", err);
        this.isLoadingHistory = false;
      }
    });
  }

  onScroll(index: number) {
    if (index < 30) {
      this.loadHistory();
    }
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

  onEnter(event: Event) {
    event.preventDefault();
    this.onSubmit();
  }

  onSubmit() {
    const content = this.chatForm.get('message')?.value?.trim() || '';
    const attachments = this.stagedAttachments();

    if (!content && attachments.length === 0) return;

    console.log("Sending payload to STOMP broker:", { content, attachments });
    this.chatService.sendMessage(this.courseId, { content, attachments });

    this.chatForm.reset();
    this.stagedAttachments.set([]);
  }

  private scrollToBottom() {
    setTimeout(() => {
      if (this.viewport) {
        this.viewport.scrollTo({ bottom: 0, behavior: 'smooth' });
      }
    }, 50);
  }

  ngOnDestroy() {
    this.chatSub?.unsubscribe();
  }
}
