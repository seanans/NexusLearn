import { Component, inject, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { filter, Subscription } from 'rxjs';
import { ChatInboxItem } from './models/chat-inbox.model';
import { ChatConnectionService } from '../../core/services/chat-connection/chat-connection.service';
import { Subject, debounceTime, distinctUntilChanged, switchMap, of } from 'rxjs';

type TabType = 'ALL' | 'COURSES' | 'DMS' | 'GROUPS';

interface ChatCategory {
  id: string;
  name: string;
  channels: ChatInboxItem[];
  unreadCount: number;
}

interface CourseGroup {
  courseId: string;
  courseTitle: string;
  unreadCount: number;
  categories: ChatCategory[];
}

@Component({
  selector: 'app-messages-hub',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './messages-hub.component.html',
  styleUrls: ['./messages-hub.component.scss']
})
export class MessagesHubComponent implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly chatService = inject(ChatConnectionService);
  public router = inject(Router);

  inboxItems = signal<ChatInboxItem[]>([]);
  isLoading = signal<boolean>(true);

  activeTab = signal<TabType>('ALL');
  expandedCourses = signal<Set<string>>(new Set());
  expandedCategories = signal<Set<string>>(new Set());

  isNewMessageModalOpen = signal<boolean>(false);
  searchQuery = signal<string>('');
  isSearching = signal<boolean>(false);
  searchResults = signal<any[]>([]);
  private searchSubject = new Subject<string>();

  private stompSubscriptions: Subscription[] = [];
  private routerSub!: Subscription;

  formatBadge(count: number): string {
    return count > 99 ? '99+' : count.toString();
  }

  private getSafeIsoDate(dateInput: any): string {
    if (!dateInput) return new Date().toISOString();
    if (Array.isArray(dateInput)) {
      return new Date(dateInput[0], dateInput[1] - 1, dateInput[2], dateInput[3] || 0, dateInput[4] || 0, dateInput[5] || 0).toISOString();
    }
    return new Date(dateInput).toISOString();
  }

  private getSafeTime(dateInput: any): number {
    const time = new Date(this.getSafeIsoDate(dateInput)).getTime();
    return isNaN(time) ? 0 : time;
  }

  totalAllUnread = computed(() => this.inboxItems().filter(i => i.hasUnread).length);

  dmChats = computed(() => this.inboxItems().filter(i => i.channelType === 'DIRECT'));
  totalDmUnread = computed(() => this.dmChats().filter(i => i.hasUnread).length);

  groupChats = computed(() => this.inboxItems().filter(i => i.channelType === 'GROUP'));
  totalGroupUnread = computed(() => this.groupChats().filter(i => i.hasUnread).length);

  totalCourseUnread = computed(() => this.inboxItems().filter(i => i.hasUnread && !['DIRECT', 'GROUP'].includes(i.channelType)).length);

  courseGroups = computed(() => {
    const courseChannels = this.inboxItems().filter(i =>
      ['COURSE', 'LESSON', 'ASSIGNMENT_PUBLIC', 'ASSIGNMENT_PRIVATE', 'ANNOUNCEMENT'].includes(i.channelType)
    );

    const groupsMap = new Map<string, CourseGroup>();

    for (const chat of courseChannels) {
      const cId = chat.courseId || 'unknown';
      if (!groupsMap.has(cId)) {
        groupsMap.set(cId, {
          courseId: cId, courseTitle: chat.courseTitle || 'Other', unreadCount: 0,
          categories: [
            { id: cId + '_ann', name: 'Announcements', channels: [], unreadCount: 0 },
            { id: cId + '_gen', name: 'General Lobby', channels: [], unreadCount: 0 },
            { id: cId + '_les', name: 'Lessons', channels: [], unreadCount: 0 },
            { id: cId + '_pub', name: 'Assignments (Public)', channels: [], unreadCount: 0 },
            { id: cId + '_priv', name: 'Assignments (Private)', channels: [], unreadCount: 0 },
          ]
        });
      }

      const group = groupsMap.get(cId)!;
      let targetCategory!: ChatCategory;

      if (chat.channelType === 'ANNOUNCEMENT') targetCategory = group.categories[0];
      else if (chat.channelType === 'COURSE') targetCategory = group.categories[1];
      else if (chat.channelType === 'LESSON') targetCategory = group.categories[2];
      else if (chat.channelType === 'ASSIGNMENT_PUBLIC') targetCategory = group.categories[3];
      else if (chat.channelType === 'ASSIGNMENT_PRIVATE') targetCategory = group.categories[4];

      targetCategory.channels.push(chat);
      if (chat.hasUnread) {
        targetCategory.unreadCount++;
        group.unreadCount++;
      }
    }

    return Array.from(groupsMap.values()).map(g => {
      g.categories = g.categories.filter(c => {
        if (c.channels.length > 0) {
          c.channels.sort((a, b) => this.getSafeTime(b.latestMessageTimestamp) - this.getSafeTime(a.latestMessageTimestamp));
          return true;
        }
        return false;
      });
      return g;
    });
  });

  get isChatActive(): boolean { return this.router.url !== '/messages'; }

  ngOnInit() {
    this.fetchInbox();
    const state = window.history.state;
    if (state?.expandCourseId) {
      this.activeTab.set('COURSES');
      this.expandedCourses.update(s => new Set(s).add(state.expandCourseId));
    }
    this.routerSub = this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe(() => {
      if (this.router.url.startsWith('/messages/')) {
        const openedChannelId = this.router.url.split('/')[2];
        this.inboxItems.update(items => items.map(i => i.channelId === openedChannelId ? { ...i, hasUnread: false } : i));
      }
    });

    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(query => {
        if (!query.trim()) return of([]);
        this.isSearching.set(true);
        return this.http.get<any[]>(`/api/users/search?query=${query}`);
      })
    ).subscribe({
      next: (results) => {
        this.searchResults.set(results);
        this.isSearching.set(false);
      },
      error: () => this.isSearching.set(false)
    });
  }

  onSearchInput(event: any) {
    const query = event.target.value;
    this.searchQuery.set(query);
    this.searchSubject.next(query);
  }

  openNewMessageModal() {
    this.isNewMessageModalOpen.set(true);
    this.searchQuery.set('');
    this.searchResults.set([]);
  }

  closeModal() {
    this.isNewMessageModalOpen.set(false);
  }

  startDirectMessage(targetUser: any) {
    const channelName = `${targetUser.firstName} ${targetUser.lastName}`;

    this.http.post<{channelId: string}>(`/api/chat/channels/direct/${targetUser.id}`, {}).subscribe({
      next: (res) => {
        this.closeModal();
        this.setTab('DMS');

        const newInboxItem: ChatInboxItem = {
          channelId: res.channelId,
          channelName: channelName,
          latestMessageSnippet: 'New conversation started',
          latestMessageTimestamp: new Date().toISOString(),
          channelType: 'DIRECT',
          hasUnread: false
        };

        this.inboxItems.update(items => {
          const existingFiltered = items.filter(i => i.channelId !== res.channelId);
          return [newInboxItem, ...existingFiltered];
        });

        this.setupGlobalStompWatchers(this.inboxItems());

        this.router.navigate(['/messages', res.channelId], {
          state: {
            channelName: channelName,
            channelType: 'DIRECT'
          }
        });
      },
      error: (err) => {
        console.error("Failed to start DM:", err);
        alert("Failed to start conversation.");
      }
    });
  }

  setTab(tab: TabType) { this.activeTab.set(tab); }

  toggleCourse(id: string) {
    this.expandedCourses.update(set => {
      const newSet = new Set(set);
      newSet.has(id) ? newSet.delete(id) : newSet.add(id);
      return newSet;
    });
  }

  toggleCategory(id: string) {
    this.expandedCategories.update(set => {
      const newSet = new Set(set);
      newSet.has(id) ? newSet.delete(id) : newSet.add(id);
      return newSet;
    });
  }

  isExpanded(id: string, type: 'course'|'category'): boolean {
    return type === 'course' ? this.expandedCourses().has(id) : this.expandedCategories().has(id);
  }

  private fetchInbox() {
    this.http.get<ChatInboxItem[]>('/api/chat/inbox').subscribe({
      next: (items) => {
        this.inboxItems.set(items);
        this.setupGlobalStompWatchers(items);
        this.isLoading.set(false);
      },
      error: (err) => this.isLoading.set(false)
    });
  }

  private setupGlobalStompWatchers(items: ChatInboxItem[]) {
    this.stompSubscriptions.forEach(sub => sub.unsubscribe());
    this.stompSubscriptions = [];

    items.forEach(item => {
      const sub = this.chatService.watchChannel(item.channelId).subscribe(msg => {
        const parsed = JSON.parse(msg.body);
        const isCurrentlyViewing = this.router.url === `/messages/${item.channelId}`;

        let snippet = parsed.content;
        if (!snippet || snippet.trim() === '') {
          snippet = (parsed.attachments && parsed.attachments.length > 0) ? `📎 ${parsed.attachments[0].fileName}` : "New message";
        }

        this.inboxItems.update(current => {
          const updatedItems = current.map(i => {
            if (i.channelId === item.channelId) {
              return {
                ...i,
                latestMessageSnippet: snippet,
                latestMessageTimestamp: this.getSafeIsoDate(parsed.createdAt),
                hasUnread: !isCurrentlyViewing
              };
            }
            return i;
          });

          return updatedItems.sort((a, b) => {
            return this.getSafeTime(b.latestMessageTimestamp) - this.getSafeTime(a.latestMessageTimestamp);
          });
        });
      });
      this.stompSubscriptions.push(sub);
    });
  }

  ngOnDestroy() {
    this.stompSubscriptions.forEach(sub => sub.unsubscribe());
    if (this.routerSub) this.routerSub.unsubscribe();
  }
}
