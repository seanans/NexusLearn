import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AsyncPipe } from '@angular/common';
import { Observable, combineLatest, switchMap, filter, map, BehaviorSubject, of, catchError } from 'rxjs';
import { CourseService } from '../../services/course.service';
import { LessonResponse, CourseRole } from '../../models/course.models';
import { AttachmentGalleryComponent } from '../../../../shared/components/attachment-gallery/attachment-gallery.component';

@Component({
  selector: 'app-lesson-viewer',
  standalone: true,
  imports: [AsyncPipe, RouterLink, AttachmentGalleryComponent],
  templateUrl: './lesson-viewer.component.html',
  styleUrls: ['./lesson-viewer.component.scss']
})
export class LessonViewerComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private courseService = inject(CourseService);
  private refreshTrigger$ = new BehaviorSubject<void>(undefined);

  viewData$!: Observable<{ lesson: LessonResponse, role: CourseRole, courseId: string } | null>;
  CourseRole = CourseRole;

  ngOnInit(): void {
    this.viewData$ = this.refreshTrigger$.pipe(
      switchMap(() => combineLatest([
        this.route.paramMap,
        this.courseService.currentCourse$.pipe(filter(c => c !== null))
      ])),
      switchMap(([params, course]) =>
        this.courseService.getLessonById(course!.id, params.get('lessonId')!).pipe(
          map(lesson => ({ lesson, role: course!.currentUserRole, courseId: course!.id })),
          catchError(err => {
            console.error("Failed to load lesson", err);
            return of(null);
          })
        )
      )
    );
  }
}
