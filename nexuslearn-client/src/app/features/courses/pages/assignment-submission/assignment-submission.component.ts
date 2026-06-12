import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AsyncPipe, DatePipe, NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable, combineLatest, switchMap, filter, map, BehaviorSubject, of, catchError } from 'rxjs';
import { CourseService } from '../../services/course.service';
import { AssignmentResponse, CourseRole, SubmissionResponse, PendingAttachmentDto, EntityType } from '../../models/course.models';
import { AttachmentGalleryComponent } from '../../../../shared/components/attachment-gallery/attachment-gallery.component';
import { FileStorageService } from '../../services/file-storage.service';
import { MiniChatComponent } from '../../../../shared/components/mini-chat/mini-chat.component';

interface AssignmentViewData {
  assignment: AssignmentResponse;
  role: CourseRole;
  courseId: string;
  submissions: SubmissionResponse[];
}

@Component({
  selector: 'app-assignment-submission',
  standalone: true,
  imports: [AsyncPipe, DatePipe, RouterLink, FormsModule, NgClass, AttachmentGalleryComponent, MiniChatComponent],
  templateUrl: './assignment-submission.component.html',
  styleUrl: './assignment-submission.component.scss'
})
export class AssignmentSubmissionComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private courseService = inject(CourseService);
  private fileStorageService = inject(FileStorageService);
  private cdr = inject(ChangeDetectorRef);

  viewData$!: Observable<AssignmentViewData | null>;
  CourseRole = CourseRole;
  EntityType = EntityType;

  activeTab: 'details' | 'submissions' = 'details';
  searchQuery: string = '';
  selectedSubmissionId: string | null = null;

  private refreshTrigger$ = new BehaviorSubject<void>(undefined);
  isResubmitting: boolean = false;
  studentAnswer: string = '';

  stagedFiles: PendingAttachmentDto[] = [];
  isUploadingFile: boolean = false;

  gradingScores: { [submissionId: string]: number } = {};
  gradingFeedbacks: { [submissionId: string]: string } = {};

  ngOnInit(): void {
    this.viewData$ = this.refreshTrigger$.pipe(
      switchMap(() => combineLatest([
        this.route.paramMap,
        this.courseService.currentCourse$.pipe(filter(c => c !== null))
      ])),
      switchMap(([params, course]) => {
        const courseId = course!.id;
        const assignmentId = params.get('assignmentId')!;

        return combineLatest([
          this.courseService.getAssignmentById(courseId, assignmentId).pipe(catchError(() => of(null))),
          this.courseService.getSubmissions(assignmentId).pipe(catchError(() => of([])))
        ]).pipe(
          map(([assignment, submissions]) => {
            if (!assignment) return null;

            const sortedSubmissions = [...submissions].sort((a, b) => {
              if (a.score === null && b.score !== null) return -1;
              if (a.score !== null && b.score === null) return 1;
              return new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime();
            });

            if (course!.currentUserRole !== CourseRole.STUDENT) {
              sortedSubmissions.forEach(sub => {
                this.gradingScores[sub.id] = sub.score ?? 0;
                this.gradingFeedbacks[sub.id] = sub.feedback ?? '';
              });
            }
            return { assignment, role: course!.currentUserRole, courseId, submissions: sortedSubmissions };
          })
        );
      })
    );
  }

  getFilteredSubmissions(submissions: SubmissionResponse[]): SubmissionResponse[] {
    if (!submissions) return [];
    if (!this.searchQuery.trim()) return submissions;
    const q = this.searchQuery.toLowerCase();
    return submissions.filter(s => s.studentName?.toLowerCase().includes(q));
  }

  selectSubmission(id: string) {
    this.selectedSubmissionId = id;
  }

  onFileSelectedForSubmission(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file: File = input.files[0];
      this.isUploadingFile = true;

      this.fileStorageService.uploadPending(file).subscribe({
        next: (pendingDto) => {
          this.stagedFiles = [...this.stagedFiles, pendingDto];
          this.isUploadingFile = false;
          input.value = '';
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.isUploadingFile = false;
          input.value = '';
          this.cdr.detectChanges();
          alert('Upload failed: ' + (err.error?.message || err.message));
        }
      });
    }
  }

  removeStagedFile(index: number): void {
    this.stagedFiles.splice(index, 1);
  }

  submitWork(assignmentId: string): void {
    if (this.isUploadingFile) { alert("Please wait for the file to finish uploading."); return; }
    if (!this.studentAnswer.trim() && this.stagedFiles.length === 0) { alert("You must provide text or attach a file to submit."); return; }
    if (this.isResubmitting && !window.confirm("WARNING: Resubmitting will clear your current grade and feedback.\nProceed?")) return;

    this.courseService.submitAssignment(assignmentId, this.studentAnswer, this.stagedFiles).subscribe({
      next: () => {
        this.studentAnswer = ''; this.stagedFiles = []; this.isResubmitting = false; this.refreshTrigger$.next();
      }
    });
  }

  submitGrade(submissionId: string): void {
    this.courseService.gradeSubmission(submissionId, this.gradingScores[submissionId], this.gradingFeedbacks[submissionId]).subscribe({
      next: () => this.refreshTrigger$.next()
    });
  }

  enableResubmission(existingText: string): void {
    this.studentAnswer = existingText;
    this.isResubmitting = true;
  }
}
