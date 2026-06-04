import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AttachmentGalleryComponent } from './attachment-gallery.component';

describe('AttachmentGalleryComponent', () => {
  let component: AttachmentGalleryComponent;
  let fixture: ComponentFixture<AttachmentGalleryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AttachmentGalleryComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AttachmentGalleryComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
