import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CourseService } from '../../services/course.service';
import { FileStorageService } from '../../services/file-storage.service';
import { AttachmentResponse, PendingAttachmentDto } from '../../models/course.models';

@Component({
  selector: 'app-lesson-edit',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './lesson-edit.component.html',
  styleUrls: ['./lesson-edit.component.scss']
})
export class LessonEditComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private courseService = inject(CourseService);
  private fileStorageService = inject(FileStorageService);
  private cdr = inject(ChangeDetectorRef);

  courseId!: string;
  lessonId!: string;

  title: string = '';
  content: string = '';
  availableFrom: string = '';
  isPublished: boolean = false;

  existingAttachments: AttachmentResponse[] = [];
  stagedFiles: PendingAttachmentDto[] = [];
  isUploadingFile = false;

  ngOnInit(): void {
    let currentRoute: any = this.route.snapshot;
    while (currentRoute && !currentRoute.paramMap.has('id')) {
      currentRoute = currentRoute.parent;
    }
    this.courseId = currentRoute?.paramMap.get('id') || '';
    this.lessonId = this.route.snapshot.paramMap.get('lessonId') || '';

    if (this.courseId && this.lessonId) {
      this.courseService.getLessonById(this.courseId, this.lessonId).subscribe({
        next: (lesson) => {
          this.title = lesson.title;
          this.content = lesson.content;
          this.isPublished = lesson.published;
          this.availableFrom = lesson.availableFrom ? new Date(lesson.availableFrom).toISOString().slice(0, 16) : '';
          this.existingAttachments = lesson.attachments || [];
          this.cdr.detectChanges();
        },
        error: () => alert("Failed to load existing lesson data.")
      });
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.isUploadingFile = true;
      this.fileStorageService.uploadPending(input.files[0]).subscribe({
        next: (pendingDto) => {
          this.stagedFiles = [...this.stagedFiles, pendingDto];
          this.isUploadingFile = false;
          input.value = '';
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.isUploadingFile = false;
          input.value = '';
          alert('Upload failed: ' + (err.error?.message || err.message));
        }
      });
    }
  }

  removeStagedFile(index: number): void {
    this.stagedFiles.splice(index, 1);
  }

  deleteExistingAttachment(attachmentId: string, index: number): void {
    if (!window.confirm("Are you sure? This will permanently delete the file.")) return;
    this.fileStorageService.deleteAttachment(attachmentId).subscribe(() => {
      this.existingAttachments.splice(index, 1);
      this.cdr.detectChanges();
    });
  }

  saveChanges(): void {
    const payload = {
      title: this.title,
      content: this.content,
      availableFrom: this.availableFrom ? new Date(this.availableFrom).toISOString() : null,
      isPublished: this.isPublished,
      newAttachments: this.stagedFiles
    };

    this.courseService.updateLesson(this.lessonId, payload).subscribe({
      next: () => {
        this.router.navigate(['/courses', this.courseId, 'lessons', this.lessonId]).then();
      },
      error: (err) => alert("Could not save changes: " + (err.error?.message || "Check your inputs."))
    });
  }

  deleteLesson(): void {
    if (!window.confirm("Are you sure you want to permanently delete this lesson?")) return;
    this.courseService.deleteLesson(this.lessonId).subscribe(() => {
      this.router.navigate(['/courses', this.courseId, 'syllabus']).then();
    });
  }

  cancel(): void {
    this.router.navigate(['/courses', this.courseId, 'lessons', this.lessonId]).then();
  }
}
