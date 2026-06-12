import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { MessagesHubComponent } from './features/messages-hub/messages-hub.component';
import { CourseChatComponent } from './features/course-chat/course-chat.component';

export const routes: Routes = [
  { path: '', redirectTo: 'courses', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./features/authentication/pages/login-page/login-page.component').then(m => m.LoginPageComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/authentication/pages/register-page/register-page.component').then(m => m.RegisterPageComponent)
  },
  {
    path: 'courses',
    canActivate: [authGuard],
    loadComponent: () => import('./features/courses/pages/course-list-page/course-list-page.component').then(m => m.CourseListPageComponent)
  },
  {
    path: 'courses/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./features/courses/pages/course-layout/course-layout.component').then(m => m.CourseLayoutComponent),
    children: [
      {
        path: 'syllabus',
        loadComponent: () => import('./features/courses/pages/course-detail-page/course-detail-page.component').then(m => m.CourseDetailPageComponent)
      },
      {
        path: 'lessons/:lessonId',
        loadComponent: () => import('./features/courses/pages/lesson-viewer/lesson-viewer.component').then(m => m.LessonViewerComponent)
      },
      {
        path: 'assignments/:assignmentId',
        loadComponent: () => import('./features/courses/pages/assignment-submission/assignment-submission.component').then(m => m.AssignmentSubmissionComponent)
      },
      {
        path: 'assignments/:assignmentId/edit',
        loadComponent: () => import('./features/courses/pages/assignment-edit/assignment-edit.component').then(m => m.AssignmentEditComponent)
      },
      {
        path: 'lessons/:lessonId/edit',
        loadComponent: () => import('./features/courses/pages/lesson-edit/lesson-edit.component').then(m => m.LessonEditComponent)
      },
      {
        path: 'settings',
        loadComponent: () => import('./features/courses/pages/course-settings/course-settings.component').then(m => m.CourseSettingsComponent)
      },
      {
        path: 'people',
        loadComponent: () => import('./features/courses/pages/course-people/course-people.component').then(m => m.CoursePeopleComponent)
      },
      { path: '', redirectTo: 'syllabus', pathMatch: 'full' }
    ]
  },
  {
    path: 'messages',
    component: MessagesHubComponent,
    children: [
      {
        path: ':channelId',
        component: CourseChatComponent
      }
    ]
  },
  { path: '**', redirectTo: 'courses' }
];
