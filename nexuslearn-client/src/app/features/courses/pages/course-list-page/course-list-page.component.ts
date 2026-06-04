import { Component, inject, OnInit } from '@angular/core';
import { RouterLink, Router } from '@angular/router';
import { AsyncPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable, BehaviorSubject, switchMap } from 'rxjs';
import { CourseService } from '../../services/course.service';

@Component({
  selector: 'app-course-list-page',
  standalone: true,
  imports: [RouterLink, AsyncPipe, DatePipe, FormsModule],
  templateUrl: './course-list-page.component.html',
  styleUrls: ['./course-list-page.component.scss']
})
export class CourseListPageComponent implements OnInit {
  private courseService = inject(CourseService);
  private router = inject(Router);

  courses$!: Observable<any>;
  private refreshTrigger$ = new BehaviorSubject<void>(undefined);

  isCreatingCourse = false;
  newCourseForm = { title: '', description: '' };
  isSaving = false;

  ngOnInit(): void {
    this.courses$ = this.refreshTrigger$.pipe(
      switchMap(() => this.courseService.getMyCourses())
    );
  }

  openCreateModal(): void {
    this.isCreatingCourse = true;
    this.newCourseForm = { title: '', description: '' };
  }

  closeCreateModal(): void {
    this.isCreatingCourse = false;
  }

  createCourse(): void {
    if (!this.newCourseForm.title.trim()) return;
    this.isSaving = true;

    this.courseService.createCourse(this.newCourseForm).subscribe({
      next: (newCourse) => {
        this.isSaving = false;
        this.isCreatingCourse = false;
        this.router.navigate(['/courses', newCourse.id, 'syllabus']);
      },
      error: (err) => {
        this.isSaving = false;
        alert('Failed to create course: ' + (err.error?.message || err.message));
      }
    });
  }
}
