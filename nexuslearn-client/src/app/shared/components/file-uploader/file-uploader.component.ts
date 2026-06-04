import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { EntityType, AttachmentResponse } from '../../../features/courses/models/course.models';
import { FileStorageService } from '../../../features/courses/services/file-storage.service';

@Component({
  selector: 'app-file-uploader',
  standalone: true,
  templateUrl: './file-uploader.component.html',
  styleUrl: './file-uploader.component.scss'
})
export class FileUploaderComponent {
  @Input({ required: true }) entityId!: string;
  @Input({ required: true }) entityType!: EntityType;
  @Output() uploadComplete = new EventEmitter<AttachmentResponse>();

  private fileStorageService = inject(FileStorageService);
  isUploading = false;

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file: File = input.files[0];
      this.isUploading = true;

      this.fileStorageService.uploadImmediate(file, this.entityId, this.entityType).subscribe({
        next: (attachment) => {
          this.isUploading = false;
          input.value = '';
          this.uploadComplete.emit(attachment);
        },
        error: (err) => {
          this.isUploading = false;
          input.value = '';
          alert('Upload failed: ' + (err.error?.message || err.message));
        }
      });
    }
  }
}
