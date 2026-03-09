import {Component, OnInit, ViewEncapsulation} from "@angular/core";
import {Subject} from 'rxjs';
import {AbstractControl, FormArray, FormBuilder, FormGroup, Validators} from '@angular/forms';
import {MatDialogRef} from '@angular/material/dialog';
import {ProjectModel} from '../../../../shared/models/project.model';
import {UtilFunctions} from '../../../../shared/util/util-functions';
import parser from 'cron-parser';
import {CicdService} from '../cicd.service';
import {MessageDialogService} from '../../../../shared/services/message/message-dialog-service';

export function cronValidator(c: AbstractControl) {
  if (UtilFunctions.isValidStringOrArray(c.value) === true) {
    try {
      var interval = parser.parseExpression(c.value);

    } catch (err) {
      return {cronInvalid: {value: c.value}}
    }
  }

  return null;
}
@Component({
  selector       : 'cicd-modal',
  styleUrls      : ['/cicd-modal.component.scss'],
  templateUrl    : './cicd-modal.component.html',
  encapsulation  : ViewEncapsulation.None,

  standalone: false
})
export class CICDModalComponent implements OnInit
{
  formSave: FormGroup;
  private _unsubscribeAll: Subject<any> = new Subject<any>();

  public customPatterns = { 'I': { pattern: new RegExp("[0-9|\*|/|L| |\-]+")} };

  title: string;
  private _target: ProjectModel = null;
  private _branches: Array<string> = [];
  service: CicdService;
  set target(value: ProjectModel) {
    this._target = value;
    this.title = 'Geração de Integração CI/CD ' + this._target.title;
  }

  get target(): ProjectModel {
    return  this._target;
  }

  set branches(value: Array<string>) {
    this._branches = value;
  }

  get branches(): Array<string> {
    return  this._branches;
  }
  constructor(private _formBuilder: FormBuilder,
              public dialogRef: MatDialogRef<CICDModalComponent>,
              private _messageService: MessageDialogService)
  {

  }

  ngOnInit(): void {
    this.formSave = this._formBuilder.group({
      productId: [this.target.id, Validators.required],
      branch: [null, Validators.required],
      cronExpression: [null],
      enabled: [true],
      modules: this._formBuilder.array([])
    });
    for (let m of this.target.modules) {
      if (m.framework === true) {
        continue;
      }
      this.getProductModules().push(this._formBuilder.group({
        productId: [m.id, Validators.required],
        productTitle: [m.title, Validators.required],
        enabled: [false],
        includeTests: [false],
        ignoreHashCommit: [false]
      }))
    }
  }

  getProductModules(): FormArray {

    return this.formSave.get('modules') as FormArray;
  }

  doSaving() {
    this.service.save(this.formSave.value).then(value => {
      this._messageService.open("Nova integração foi iniciada com sucesso!", "SUCESSO", "success")
      this.dialogRef.close();
    });
  }
}
