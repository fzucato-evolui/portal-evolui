import {Component, ElementRef, EventEmitter, Input, Output, ViewChild, ViewEncapsulation} from '@angular/core';

@Component({
  selector: 'app-file-drop',
  encapsulation: ViewEncapsulation.None,
  template: `
    <div class="file-drop-zone"
         [class.file-drop-zone--over]="isDragOver"
         (dragover)="onDragOver($event)"
         (dragleave)="onDragLeave($event)"
         (drop)="onDrop($event)">
      <ng-content></ng-content>
      <input type="file" #fileInput hidden
             [accept]="accept"
             [multiple]="multiple"
             (change)="onFileSelected($event)">
    </div>
  `,
  styles: [`
    .file-drop-zone {
      position: relative;
    }
    .file-drop-zone--over {
      opacity: 0.7;
      outline: 2px dashed #1976d2;
      outline-offset: -2px;
    }
  `],

  standalone: false
})
export class FileDropComponent {

  @Input() accept: string = '';
  @Input() multiple: boolean = false;
  @Output() fileDrop = new EventEmitter<File[]>();

  @ViewChild('fileInput') fileInput: ElementRef<HTMLInputElement>;

  isDragOver = false;

  openFileSelector(): void {
    this.fileInput.nativeElement.click();
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
    const files = this.filterFiles(Array.from(event.dataTransfer?.files || []));
    if (files.length) {
      this.fileDrop.emit(files);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = this.filterFiles(Array.from(input.files || []));
    if (files.length) {
      this.fileDrop.emit(files);
    }
    input.value = '';
  }

  private filterFiles(files: File[]): File[] {
    if (!this.accept) return files;
    const extensions = this.accept.split(',').map(e => e.trim().toLowerCase());
    return files.filter(f => {
      const name = f.name.toLowerCase();
      return extensions.some(ext => name.endsWith(ext));
    });
  }
}
