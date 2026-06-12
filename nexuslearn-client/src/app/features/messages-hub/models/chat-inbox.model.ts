export interface ChatInboxItem {
  channelId: string;
  channelName: string;
  latestMessageSnippet: string;
  latestMessageTimestamp: string;
  channelType: string;
  courseId?: string;
  courseTitle?: string;
  referenceId?: string;
  hasUnread: boolean;
}
