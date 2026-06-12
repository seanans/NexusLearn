import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { Observable, switchMap } from 'rxjs';
import { CourseService } from '../../services/course.service';
import { CourseResponse, CourseRole } from '../../models/course.models';
import {AsyncPipe} from '@angular/common';

@Component({
  selector: 'app-course-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AsyncPipe],
  templateUrl: './course-layout.component.html',
  styleUrls: ['./course-layout.component.scss']
})
export class CourseLayoutComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private courseService = inject(CourseService);

  course$!: Observable<CourseResponse>;
  CourseRole = CourseRole;

  ngOnInit(): void {
    this.course$ = this.route.paramMap.pipe(
      switchMap(params => this.courseService.getCourseById(params.get('id')!))
    );
  }
}
