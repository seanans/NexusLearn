export enum ItemType {
  LESSON = 'LESSON',
  ASSIGNMENT = 'ASSIGNMENT'
}

export enum CourseRole {
  TEACHER = 'TEACHER',
  ASSISTANT = 'ASSISTANT',
  STUDENT = 'STUDENT'
}

export enum EntityType {
  LESSON = 'LESSON',
  ASSIGNMENT = 'ASSIGNMENT',
  SUBMISSION = 'SUBMISSION',
  MESSAGE = 'MESSAGE'
}

export interface SyllabusItem {
  itemId: string;
  title: string;
  type: ItemType;
  orderIndex: number;
}

export interface SyllabusModule {
  moduleId: string;
  title: string;
  description: string;
  published: boolean;
  orderIndex: number;
  items: SyllabusItem[];
}

export interface CourseSyllabusResponse {
  courseId: string;
  modules: SyllabusModule[];
}

export interface CourseResponse {
  id: string;
  title: string;
  description: string;
  currentUserRole: CourseRole;
}

export interface LessonResponse {
  id: string;
  title: string;
  content: string;
  orderIndex: number;
  published: boolean;
  availableFrom: string | null;
  createdAt: string;
  updatedAt: string;
  attachments?: AttachmentResponse[];
}

export interface AssignmentResponse {
  id: string;
  title: string;
  description: string;
  maxScore: number;
  dueDate: string;
  orderIndex: number;
  published: boolean;
  availableFrom: string | null;
  createdAt: string;
  updatedAt: string;
  attachments?: AttachmentResponse[];
}

export interface SubmissionResponse {
  id: string;
  assignmentId: string;
  userId: string;
  studentName: string;
  submissionText: string;
  score: number | null;
  feedback: string | null;
  submittedAt: string;
  late: boolean;
  gradedBy: string | null;
  attachments?: AttachmentResponse[];
}

export interface AttachmentResponse {
  id: string;
  entityId: string;
  entityType: EntityType;
  fileUrl: string;
  fileName: string;
  fileType: string;
  createdAt: string;
}

export interface PendingAttachmentDto {
  fileUrl: string;
  fileName: string;
  fileType: string;
}

export interface AttachmentCreateRequest {
  entityId: string;
  entityType: EntityType;
  fileUrl: string;
  fileName: string;
  fileType: string;
}

export interface PresignedUrlResponse {
  uploadUrl: string;
  fileUrl: string;
  objectName: string;
}


