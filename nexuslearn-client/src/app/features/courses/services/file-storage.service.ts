import { Injectable, inject } from '@angular/core';
import {HttpBackend, HttpClient} from '@angular/common/http';
import { Observable, switchMap, map } from 'rxjs';
import {
  EntityType,
  PresignedUrlResponse,
  AttachmentCreateRequest,
  AttachmentResponse,
  PendingAttachmentDto
} from '../models/course.models';

@Injectable({ providedIn: 'root' })
export class FileStorageService {
  private http = inject(HttpClient);

  private httpBackend = inject(HttpBackend);
  private minioClient = new HttpClient(this.httpBackend);

  private readonly DUMMY_UUID = '00000000-0000-0000-0000-000000000000';

  private getUploadTicket(fileName: string, entityId: string, entityType: EntityType): Observable<PresignedUrlResponse> {
    return this.http.get<PresignedUrlResponse>('/api/files/upload-url', {
      params: { fileName, entityId, entityType }
    });
  }

  private uploadToMinio(uploadUrl: string, file: File): Observable<any> {
    return this.minioClient.put(uploadUrl, file, {
      headers: { 'Content-Type': file.type }
    });
  }

  private linkToDb(request: AttachmentCreateRequest): Observable<AttachmentResponse> {
    return this.http.post<AttachmentResponse>('/api/attachments', request);
  }

  public deleteAttachment(attachmentId: string): Observable<void> {
    return this.http.delete<void>(`/api/attachments/${attachmentId}`);
  }

  public uploadImmediate(file: File, entityId: string, entityType: EntityType): Observable<AttachmentResponse> {
    return this.getUploadTicket(file.name, entityId, entityType).pipe(
      switchMap(ticket => {
        return this.uploadToMinio(ticket.uploadUrl, file).pipe(
          switchMap(() => {
            const req: AttachmentCreateRequest = {
              entityId: entityId,
              entityType: entityType,
              fileUrl: ticket.fileUrl,
              fileName: file.name,
              fileType: this.determineFileType(file.type)
            };
            return this.linkToDb(req);
          })
        );
      })
    );
  }

  public uploadPending(file: File): Observable<PendingAttachmentDto> {
    return this.http.get<PresignedUrlResponse>('/api/files/staged-upload-url', {
      params: { fileName: file.name }
    }).pipe(
      switchMap(ticket => {
        return this.minioClient.put(ticket.uploadUrl, file, {
          headers: { 'Content-Type': file.type },
          responseType: 'text'
        }).pipe(
          map(() => {
            return {
              fileUrl: ticket.fileUrl,
              fileName: file.name,
              fileType: this.determineFileType(file.type)
            };
          })
        );
      })
    );
  }

  private determineFileType(mimeType: string): string {
    if (mimeType.startsWith('video/')) return 'VIDEO';
    if (mimeType.startsWith('image/')) return 'IMAGE';
    if (mimeType === 'application/pdf') return 'PDF';
    if (mimeType.includes('zip') || mimeType.includes('compressed') || mimeType.includes('rar')) return 'ZIP';
    return 'DOCUMENT';
  }
}
