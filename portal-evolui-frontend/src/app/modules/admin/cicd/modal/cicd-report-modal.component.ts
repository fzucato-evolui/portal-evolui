import {Component, OnInit, ViewEncapsulation} from "@angular/core";
import {Subject} from 'rxjs';
import {FormGroup} from '@angular/forms';
import {MatDialogRef} from '@angular/material/dialog';
import {ProjectModel} from '../../../../shared/models/project.model';
import {CICDReportModel} from '../../../../shared/models/cicd.model';
import {DomSanitizer, SafeResourceUrl} from "@angular/platform-browser";

@Component({
  selector       : 'cicd-report-modal',
  styleUrls      : ['/cicd-report-modal.component.scss'],
  templateUrl    : './cicd-report-modal.component.html',
  encapsulation  : ViewEncapsulation.None,

  standalone: false
})
export class CICDReportModalComponent implements OnInit
{
  formSave: FormGroup;
  private _unsubscribeAll: Subject<any> = new Subject<any>();

  public modelMap: { [key: string]: CICDReportModel } = {};
  url: SafeResourceUrl;
  title: string;
  private _target: ProjectModel = null;
  set target(value: ProjectModel) {
    this._target = value;
    this.title = 'Relatório CI/CD ' + this._target.title;
  }

  get target(): ProjectModel {
    return  this._target;
  }
  constructor(public dialogRef: MatDialogRef<CICDReportModalComponent>, protected sanitizer: DomSanitizer)
  {
    this.url = this.sanitizer.bypassSecurityTrustResourceUrl(
      'https://embed.hava.io?id=b7738dd4-3327-4777-978d-601eea284406&type=Views::Infrastructure');
  }

  ngOnInit(): void {

  }
}
