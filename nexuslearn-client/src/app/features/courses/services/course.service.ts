import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import {
  CourseResponse, CourseSyllabusResponse,
  LessonResponse, AssignmentResponse, SubmissionResponse, PendingAttachmentDto
} from '../models/course.models';

@Injectable({ providedIn: 'root' })
export class CourseService {
  private http = inject(HttpClient);
  private readonly API_URL = '/api/courses';

  private currentCourseSubject = new BehaviorSubject<CourseResponse | null>(null);
  public currentCourse$ = this.currentCourseSubject.asObservable();

  public expandedModulesCache = new Set<string>();

  getMyCourses(page: number = 0, size: number = 10): Observable<any> {
    return this.http.get<any>(`${this.API_URL}/me?page=${page}&size=${size}`);
  }

  getCourseById(courseId: string): Observable<CourseResponse> {
    return this.http.get<CourseResponse>(`${this.API_URL}/${courseId}`).pipe(
      tap(course => this.currentCourseSubject.next(course))
    );
  }

  getCourseSyllabus(courseId: string): Observable<CourseSyllabusResponse> {
    return this.http.get<CourseSyllabusResponse>(`${this.API_URL}/${courseId}/syllabus`);
  }

  getLessonById(courseId: string, lessonId: string): Observable<LessonResponse> {
    return this.http.get<LessonResponse>(`${this.API_URL}/${courseId}/lessons/${lessonId}`);
  }

  getAssignmentById(courseId: string, assignmentId: string): Observable<AssignmentResponse> {
    return this.http.get<AssignmentResponse>(`${this.API_URL}/${courseId}/assignments/${assignmentId}`);
  }

  updateAssignment(assignmentId: string, updateData: {
    title: string;
    description: string;
    maxScore: number;
    dueDate: string;
    availableFrom: string | null;
    isPublished: boolean;
    newAttachments: PendingAttachmentDto[];
  }): Observable<void> {
    return this.http.put<void>(`/api/assignments/${assignmentId}`, updateData);
  }

  deleteAssignment(assignmentId: string): Observable<void> {
    return this.http.delete<void>(`/api/assignments/${assignmentId}`);
  }

  getSubmissions(assignmentId: string): Observable<SubmissionResponse[]> {
    return this.http.get<SubmissionResponse[]>(`/api/assignments/${assignmentId}/submissions`);
  }

  submitAssignment(assignmentId: string, submissionText: string, attachments: PendingAttachmentDto[] = []): Observable<SubmissionResponse> {
    return this.http.post<SubmissionResponse>(`/api/assignments/${assignmentId}/submissions`, {
      submissionText,
      attachments
    });
  }

  gradeSubmission(submissionId: string, score: number, feedback: string): Observable<SubmissionResponse> {
    return this.http.put<SubmissionResponse>(`/api/submissions/${submissionId}/grade`, { score, feedback });
  }

  updateLesson(lessonId: string, updateData: any): Observable<void> {
    return this.http.put<void>(`/api/lessons/${lessonId}`, updateData);
  }

  deleteLesson(lessonId: string): Observable<void> {
    return this.http.delete<void>(`/api/lessons/${lessonId}`);
  }

  updateModule(moduleId: string, updateData: { title: string; description: string; isPublished: boolean }): Observable<any> {
    return this.http.put(`/api/modules/${moduleId}`, updateData);
  }

  deleteModule(moduleId: string): Observable<void> {
    return this.http.delete<void>(`/api/modules/${moduleId}`);
  }

  createCourse(data: { title: string; description: string }): Observable<CourseResponse> {
    return this.http.post<CourseResponse>(this.API_URL, data);
  }

  createModule(courseId: string, data: { title: string; description: string }): Observable<{ id: string }> {
    return this.http.post<{ id: string }>(`/api/courses/${courseId}/modules`, data);
  }

  createLesson(moduleId: string, data: { title: string; content: string; isPublished: boolean }): Observable<{ id: string }> {
    return this.http.post<{ id: string }>(`/api/modules/${moduleId}/lessons`, data);
  }

  createAssignment(moduleId: string, data: { title: string; description: string; maxScore: number; dueDate: string; isPublished: boolean }): Observable<{ id: string }> {
    return this.http.post<{ id: string }>(`/api/modules/${moduleId}/assignments`, data);
  }

  updateCourse(courseId: string, data: { title: string; description: string }): Observable<CourseResponse> {
    return this.http.put<CourseResponse>(`${this.API_URL}/${courseId}`, data).pipe(
      tap(updatedCourse => this.currentCourseSubject.next(updatedCourse))
    );
  }

  deleteCourse(courseId: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${courseId}`);
  }

  getCourseMembers(courseId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/${courseId}/members`);
  }

  addCourseMember(courseId: string, email: string, role: string): Observable<any> {
    return this.http.post(`${this.API_URL}/${courseId}/members`, { email, role });
  }

  removeCourseMember(courseId: string, email: string): Observable<any> {
    return this.http.delete(`${this.API_URL}/${courseId}/members/${email}`);
  }
}
