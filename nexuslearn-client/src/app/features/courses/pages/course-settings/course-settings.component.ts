import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CourseService } from '../../services/course.service';

@Component({
  selector: 'app-course-settings',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './course-settings.component.html',
  styleUrls: ['./course-settings.component.scss']
})
export class CourseSettingsComponent implements OnInit {
  private courseService = inject(CourseService);
  private router = inject(Router);

  courseId = '';
  title = '';
  description = '';
  isSaving = false;

  ngOnInit(): void {
    this.courseService.currentCourse$.subscribe(course => {
      if (course) {
        this.courseId = course.id;
        this.title = course.title;
        this.description = course.description || '';
      }
    });
  }

  saveSettings(): void {
    if (!this.title.trim()) return;
    this.isSaving = true;

    this.courseService.updateCourse(this.courseId, { title: this.title, description: this.description }).subscribe({
      next: () => {
        this.isSaving = false;
        this.router.navigate(['/courses', this.courseId, 'syllabus']).then();
      },
      error: (err) => {
        this.isSaving = false;
        alert('Failed to update course: ' + (err.error?.message || err.message));
      }
    });
  }

  deleteCourse(): void {
    const confirm1 = window.confirm("WARNING: You are about to delete this entire course.");
    if (!confirm1) return;

    const confirm2 = window.confirm("FINAL WARNING: This action cannot be undone. All modules, lessons, assignments, submissions, and files will be permanently wiped. Proceed?");

    if (confirm2) {
      this.courseService.deleteCourse(this.courseId).subscribe({
        next: () => {
          this.router.navigate(['/courses']).then();
        },
        error: (err) => alert('Failed to delete course: ' + (err.error?.message || err.message))
      });
    }
  }
}
