import {Component, OnDestroy, OnInit, ViewEncapsulation} from "@angular/core";
import {ClientModel,} from '../../../../shared/models/client.model';
import {Subject} from 'rxjs';
import {ClienteService} from '../cliente.service';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {MessageDialogService} from '../../../../shared/services/message/message-dialog-service';
import {MatDialogRef} from '@angular/material/dialog';
import {MatChipInputEvent} from '@angular/material/chips';
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {COMMA, ENTER} from '@angular/cdk/keycodes';
import {ProjectModel} from '../../../../shared/models/project.model';
import {ClienteComponent} from '../cliente.component';

@Component({
  selector       : 'cliente-modal',
  styleUrls      : ['/cliente-modal.component.scss'],
  templateUrl    : './cliente-modal.component.html',
  encapsulation  : ViewEncapsulation.None,

  standalone: false
})
export class ClienteModalComponent implements OnInit, OnDestroy
{
  addOnBlur = true;
  readonly separatorKeysCodes = [ENTER, COMMA] as const;
  formSave: FormGroup;
  private _unsubscribeAll: Subject<any> = new Subject<any>();

  public model: ClientModel;
  title: string;
  private _parent: ClienteComponent;
  private _service: ClienteService;
  private _target: ProjectModel = null;

  public customPatterns = { 'I': { pattern: new RegExp('\[a-zA-Z0-9_\]')} };

  set target(value: ProjectModel) {
    this._target = value;
    this.title = 'Cliente ' + this._target.title;
  }

  get target(): ProjectModel {
    return  this._target;
  }

  set parent(value: ClienteComponent) {
    this._parent = value;
    this._service = this.parent.service;
  }

  get parent(): ClienteComponent {
    return  this._parent;
  }
  constructor(private _formBuilder: FormBuilder,
              public dialogRef: MatDialogRef<ClienteModalComponent>,
              private _messageService: MessageDialogService)
  {
  }

  ngOnInit(): void {
    if (this.model && this.model.id > 0) {
      // Create the form
      this.formSave = this._formBuilder.group({
        id: [this.model.id],
        identifier: [this.model.identifier, [Validators.required]],
        description: [this.model.description]

      });
    } else {
      this.model = new ClientModel();
      // Create the form
      this.formSave = this._formBuilder.group({
        id: [this.model.id],
        identifier: ['', [Validators.required]],
        description: ['']

      });
    }
    // Create the form


  }

  ngOnDestroy(): void {
    this._unsubscribeAll.next(undefined);
    this._unsubscribeAll.complete();
  }


  doSaving() {
    const savedModel = this.formSave.value as ClientModel;
    savedModel.keywords = this.model.keywords;
    savedModel.identifier = savedModel.identifier.toUpperCase();
    savedModel.produto = this._target;
    this._service.save(savedModel).then(value => {
      this._messageService.open("Cliente salvo com sucesso!", "SUCESSO", "success")
      this.dialogRef.close();
    });
  }

  canSave(): boolean {
    if (this.formSave) {
      return !this.formSave.invalid;
    }
    return false;
  }


  addKeyword(event: MatChipInputEvent) {
    const value = (event.value || '').trim();
    if (UtilFunctions.isValidStringOrArray(this.model.keywords) === false) {
      this.model.keywords = [];
    }
    if (value) {
      this.model.keywords.push(value);
    }

    // Clear the input value
    event.chipInput!.clear();
  }

  removeKeyword(keyword: string) {
    const index = this.model.keywords.indexOf(keyword);

    if (index >= 0) {
      this.model.keywords.splice(index, 1);
    }
  }


}
