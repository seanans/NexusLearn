import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CourseService } from '../../services/course.service';
import { FileStorageService } from '../../services/file-storage.service';
import { AttachmentResponse, PendingAttachmentDto } from '../../models/course.models';

@Component({
  selector: 'app-assignment-edit',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './assignment-edit.component.html',
  styleUrls: ['./assignment-edit.component.scss']
})
export class AssignmentEditComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private courseService = inject(CourseService);
  private fileStorageService = inject(FileStorageService);
  private cdr = inject(ChangeDetectorRef);

  courseId!: string;
  assignmentId!: string;

  title: string = '';
  description: string = '';
  maxScore: number = 100;
  dueDate: string = '';
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
    this.assignmentId = this.route.snapshot.paramMap.get('assignmentId') || '';

    if (this.courseId && this.assignmentId) {
      this.courseService.getAssignmentById(this.courseId, this.assignmentId).subscribe({
        next: (assignment) => {
          this.title = assignment.title;
          this.description = assignment.description;
          this.maxScore = assignment.maxScore;
          this.isPublished = assignment.published;

          this.dueDate = assignment.dueDate ? new Date(assignment.dueDate).toISOString().slice(0, 16) : '';
          this.availableFrom = assignment.availableFrom ? new Date(assignment.availableFrom).toISOString().slice(0, 16) : '';

          this.existingAttachments = assignment.attachments || [];
          this.cdr.detectChanges();
        },
        error: (err) => alert("Failed to load existing assignment data.")
      });
    }
  }

  saveChanges(): void {
    const availableFromPayload = this.availableFrom ? new Date(this.availableFrom).toISOString() : null;
    const dueDatePayload = this.dueDate ? new Date(this.dueDate).toISOString() : new Date().toISOString();

    const payload = {
      title: this.title,
      description: this.description,
      maxScore: this.maxScore,
      dueDate: dueDatePayload,
      availableFrom: availableFromPayload,
      isPublished: this.isPublished,
      newAttachments: this.stagedFiles
    };

    this.courseService.updateAssignment(this.assignmentId, payload).subscribe({
      next: () => {
        this.router.navigate(['/courses', this.courseId, 'assignments', this.assignmentId]).then();
      },
      error: (err) => {
        alert("Could not save changes: " + (err.error?.message || "Please check your inputs for typos."));
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
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

  deleteExistingAttachment(attachmentId: string, index: number): void {
    const confirmed = window.confirm("Are you sure? This will permanently delete the file.");
    if (!confirmed) return;

    this.fileStorageService.deleteAttachment(attachmentId).subscribe(() => {
      this.existingAttachments.splice(index, 1);
      this.cdr.detectChanges();
    });
  }

  deleteAssignment(): void {
    const confirmed = window.confirm("Are you sure you want to permanently delete this assignment? All student submissions and files will be lost.");
    if (!confirmed) return;

    this.courseService.deleteAssignment(this.assignmentId).subscribe(() => {
      this.router.navigate(['/courses', this.courseId, 'syllabus']).then();
    });
  }

  cancel(): void {
    this.router.navigate(['/courses', this.courseId, 'assignments', this.assignmentId]).then();
  }
}
