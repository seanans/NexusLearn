import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AsyncPipe, NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable, switchMap, filter, combineLatest, BehaviorSubject, map } from 'rxjs';
import { CourseService } from '../../services/course.service';
import { CourseSyllabusResponse, ItemType, CourseRole, CourseResponse } from '../../models/course.models';

@Component({
  selector: 'app-course-detail-page',
  standalone: true,
  imports: [RouterLink, AsyncPipe, NgClass, FormsModule],
  templateUrl: './course-detail-page.component.html',
  styleUrls: ['./course-detail-page.component.scss']
})
export class CourseDetailPageComponent implements OnInit {
  private courseService = inject(CourseService);

  viewData$!: Observable<{ syllabus: CourseSyllabusResponse, role: CourseRole }>;
  expandedModules: Set<string> = this.courseService.expandedModulesCache;
  ItemType = ItemType;
  CourseRole = CourseRole;

  private refreshTrigger$ = new BehaviorSubject<void>(undefined);

  editingModuleId: string | null = null;
  editForm = { title: '', description: '', isPublished: false };
  isSaving = false;

  isCreatingModule = false;
  newModuleForm = { title: '', description: '' };

  creatingItemForModuleId: string | null = null;
  newItemType: ItemType | null = null;
  newItemTitle = '';

  ngOnInit(): void {
    this.viewData$ = this.refreshTrigger$.pipe(
      switchMap(() => this.courseService.currentCourse$.pipe(filter(c => c !== null))),
      switchMap(course =>
        this.courseService.getCourseSyllabus(course!.id).pipe(
          map(syllabus => ({ syllabus, role: course!.currentUserRole }))
        )
      )
    );
  }

  toggleModule(moduleId: string): void {
    if (this.editingModuleId === moduleId) return;
    if (this.expandedModules.has(moduleId)) this.expandedModules.delete(moduleId);
    else this.expandedModules.add(moduleId);
  }

  startEditing(module: any, event: Event): void {
    event.stopPropagation();
    this.editingModuleId = module.moduleId;
    this.editForm = {
      title: module.title,
      description: module.description || '',
      isPublished: module.published
    };
    this.expandedModules.add(module.moduleId);
  }

  cancelEditing(event: Event): void {
    event.stopPropagation();
    this.editingModuleId = null;
  }

  saveModule(moduleId: string, event: Event): void {
    event.stopPropagation();
    if (!this.editForm.title.trim()) return;

    this.isSaving = true;
    this.courseService.updateModule(moduleId, this.editForm).subscribe({
      next: () => {
        this.isSaving = false;
        this.editingModuleId = null;
        this.refreshTrigger$.next();
      },
      error: () => {
        this.isSaving = false;
        alert('Failed to save module updates.');
      }
    });
  }

  deleteModule(moduleId: string, event: Event): void {
    event.stopPropagation();
    if (confirm('Are you sure? This will delete the module AND all lessons/assignments inside it permanently.')) {
      this.courseService.deleteModule(moduleId).subscribe({
        next: () => this.refreshTrigger$.next(),
        error: () => alert('Failed to delete module.')
      });
    }
  }

  startCreatingModule(): void {
    this.isCreatingModule = true;
    this.newModuleForm = { title: '', description: '' };
  }

  saveNewModule(courseId: string): void {
    if (!this.newModuleForm.title.trim()) return;
    this.isSaving = true;
    this.courseService.createModule(courseId, this.newModuleForm).subscribe({
      next: () => { this.isSaving = false; this.isCreatingModule = false; this.refreshTrigger$.next(); },
      error: () => { this.isSaving = false; alert('Failed to create module.'); }
    });
  }

  startCreatingItem(moduleId: string, type: ItemType): void {
    this.creatingItemForModuleId = moduleId;
    this.newItemType = type;
    this.newItemTitle = '';
    this.expandedModules.add(moduleId);
  }

  saveNewItem(): void {
    if (!this.newItemTitle.trim() || !this.creatingItemForModuleId || !this.newItemType) return;
    this.isSaving = true;

    const request$ = this.newItemType === ItemType.LESSON
      ? this.courseService.createLesson(this.creatingItemForModuleId, { title: this.newItemTitle, content: '', isPublished: false })
      : this.courseService.createAssignment(this.creatingItemForModuleId, {
        title: this.newItemTitle, description: '', maxScore: 100,
        dueDate: new Date(Date.now() + 86400000 * 7).toISOString().slice(0, 16),
        isPublished: false
      });

    request$.subscribe({
      next: () => {
        this.isSaving = false;
        this.creatingItemForModuleId = null;
        this.newItemType = null;
        this.refreshTrigger$.next();
      },
      error: () => { this.isSaving = false; alert('Failed to create item.'); }
    });
  }
}
