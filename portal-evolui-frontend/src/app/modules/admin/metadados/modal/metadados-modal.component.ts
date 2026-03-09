import {Component, OnDestroy, OnInit, ViewEncapsulation} from "@angular/core";
import {ClientModel,} from '../../../../shared/models/client.model';
import {Subject} from 'rxjs';
import {MetadadosService} from '../metadados.service';
import {FormArray, FormBuilder, FormGroup, Validators} from '@angular/forms';
import {MessageDialogService} from '../../../../shared/services/message/message-dialog-service';
import {MatDialogRef} from '@angular/material/dialog';
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {DatabaseTypeEnum, MetadadosBranchModel} from '../../../../shared/models/metadados-branch.model';
import {AvailableVersions} from "app/shared/models/version.model";
import {MetadadosComponent} from '../metadados.component';
import {ProjectModel} from '../../../../shared/models/project.model';

@Component({
  selector       : 'metadados-modal',
  styleUrls      : ['/metadados-modal.component.scss'],
  templateUrl    : './metadados-modal.component.html',
  encapsulation  : ViewEncapsulation.None,

  standalone: false
})
export class MetadadosModalComponent implements OnInit, OnDestroy
{
  formSave: FormGroup;
  private _unsubscribeAll: Subject<any> = new Subject<any>();

  public model: MetadadosBranchModel;
  title: string;
  private _service: MetadadosService;
  private _target: ProjectModel = null;
  private _parent: MetadadosComponent;
  DatabaseTypeEnum = DatabaseTypeEnum;

  initialData: {branches: AvailableVersions, clients: Array<ClientModel>};

  public customPatterns = { 'I': { pattern: new RegExp('\[a-zA-Z0-9_\]')} };

  set target(value: ProjectModel) {
    this._target = value;
    this.title = 'Metadados ' + this._target.title;
  }

  get target(): ProjectModel {
    return  this._target;
  }

  set parent(value: MetadadosComponent) {
    this._parent = value;
    this._service = this.parent.service;
  }

  get parent(): MetadadosComponent {
    return  this._parent;
  }
  constructor(private _formBuilder: FormBuilder,
              public dialogRef: MatDialogRef<MetadadosModalComponent>,
              private _messageService: MessageDialogService)
  {
  }

  ngOnInit(): void {

    // Create the form
    this.formSave = this._formBuilder.group({
      id: [this.model.id],
      branch: ['', [Validators.required]],
      dbType: [DatabaseTypeEnum.MSSQL, [Validators.required]],
      host: ['', [Validators.required]],
      port: [null],
      database: [''],
      dbUser: ['', [Validators.required]],
      dbPassword: ['', [Validators.required]],
      debugId: ['', [Validators.required]],
      lthUser: ['', [Validators.required]],
      lthPassword: ['', [Validators.required]],
      licenseServer: ['', [Validators.required]],
      jvmOptions: [''],
      clients: this._formBuilder.array([])
    });
    if (UtilFunctions.isValidStringOrArray(this.model.clients)) {
      for (const x of this.model.clients) {
        this.addClient();
      }
    }
    // Create the form
    this.formSave.patchValue(this.model);

  }

  ngOnDestroy(): void {
    this._unsubscribeAll.next(undefined);
    this._unsubscribeAll.complete();
  }


  doSaving() {
    this.model = this.formSave.value as MetadadosBranchModel;
    this.model.produto = this.target;
    this._service.save(this.model).then(value => {
      this._messageService.open("Metadados de versão salvo com sucesso!", "SUCESSO", "success")
      this.dialogRef.close();
    });
  }

  canSave(): boolean {
    if (this.formSave) {
      return !this.formSave.invalid;
    }
    return false;
  }

  getClients(): FormArray {
    return (this.formSave.get('clients')) as FormArray;
  }

  addClient() {
    const g = this._formBuilder.group({
      id: [null],
      client: [null, [Validators.required]]
    });
    (this.formSave.get('clients') as FormArray).push(g);
  }

  deleteClient(index) {
    (this.formSave.get('clients') as FormArray).removeAt(index);
  }

  compareClient(v1: ClientModel , v2: ClientModel): boolean {
    return v1 && v2 ? v1.id === v2.id : v1 === v2;
  }


}
