import { Component, inject, OnInit } from '@angular/core';
import { AsyncPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable, BehaviorSubject, switchMap, combineLatest, map } from 'rxjs';
import { CourseService } from '../../services/course.service';
import { CourseRole } from '../../models/course.models';

@Component({
  selector: 'app-course-people',
  standalone: true,
  imports: [AsyncPipe, DatePipe, FormsModule],
  templateUrl: './course-people.component.html',
  styleUrls: ['./course-people.component.scss']
})
export class CoursePeopleComponent implements OnInit {
  private courseService = inject(CourseService);

  viewData$!: Observable<{ members: any[], role: CourseRole, courseId: string }>;
  private refreshTrigger$ = new BehaviorSubject<void>(undefined);
  CourseRole = CourseRole;

  isInviting = false;
  inviteForm = { email: '', role: CourseRole.STUDENT };
  isSaving = false;

  ngOnInit(): void {
    this.viewData$ = this.refreshTrigger$.pipe(
      switchMap(() => combineLatest([
        this.courseService.currentCourse$,
      ])),
      switchMap(([course]) =>
        this.courseService.getCourseMembers(course!.id).pipe(
          map(members => {
            const roleOrder = { [CourseRole.TEACHER]: 1, [CourseRole.ASSISTANT]: 2, [CourseRole.STUDENT]: 3 };
            members.sort((a, b) => roleOrder[a.role as CourseRole] - roleOrder[b.role as CourseRole]);
            return { members, role: course!.currentUserRole, courseId: course!.id };
          })
        )
      )
    );
  }

  sendInvite(courseId: string): void {
    if (!this.inviteForm.email.trim()) return;
    this.isSaving = true;

    this.courseService.addCourseMember(courseId, this.inviteForm.email, this.inviteForm.role).subscribe({
      next: () => {
        this.isSaving = false;
        this.isInviting = false;
        this.inviteForm.email = '';
        this.refreshTrigger$.next();
      },
      error: (err) => {
        this.isSaving = false;
        alert('Failed to add member: ' + (err.error?.message || err.message));
      }
    });
  }

  removeMember(courseId: string, email: string): void {
    if (confirm(`Are you sure you want to remove ${email} from this course?`)) {
      this.courseService.removeCourseMember(courseId, email).subscribe({
        next: () => this.refreshTrigger$.next(),
        error: (err) => alert('Failed to remove member: ' + (err.error?.message || err.message))
      });
    }
  }
}
