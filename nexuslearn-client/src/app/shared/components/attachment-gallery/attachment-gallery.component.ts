import { Component, Input, Output, EventEmitter } from '@angular/core';
import { AttachmentResponse } from '../../../features/courses/models/course.models';

@Component({
  selector: 'app-attachment-gallery',
  standalone: true,
  templateUrl: './attachment-gallery.component.html',
  styleUrl: './attachment-gallery.component.scss'
})
export class AttachmentGalleryComponent {
  @Input() attachments: AttachmentResponse[] = [];
  @Input() allowDelete: boolean = false;
  @Output() deleted = new EventEmitter<string>();

  onDelete(id: string) {
    this.deleted.emit(id);
  }
}
