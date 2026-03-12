import {ChangeDetectorRef, Component, OnDestroy, OnInit, ViewChild, ViewEncapsulation} from "@angular/core";
import {FormArray, FormBuilder, FormGroup, Validators} from '@angular/forms';
import {ReplaySubject, Subject} from 'rxjs';
import {UserService} from '../../../../../shared/services/user/user.service';
import {MatDialogRef} from '@angular/material/dialog';
import {MessageDialogService} from '../../../../../shared/services/message/message-dialog-service';
import {ActionRdsModel, ActionRDSRemapTypeEnum, ActionRDSTypeEnum} from '../../../../../shared/models/action-rds.model';
import {UtilFunctions} from '../../../../../shared/util/util-functions';
import {MatStepper} from '@angular/material/stepper';
import {BucketFileTypeEnum, BucketModel} from '../../../../../shared/models/bucket.model';
import {ActionRdsService} from '../action-rds.service';
import {RDSModel} from '../../../../../shared/models/rds.model';
import {StepperSelectionEvent} from '@angular/cdk/stepper';
import {MatSlideToggleChange} from '@angular/material/slide-toggle';
import {MatTableDataSource} from '@angular/material/table';
import {MAT_DATE_FORMATS} from '@angular/material/core';

@Component({
  selector       : 'action-rds-modal',
  styleUrls      : ['/action-rds-modal.component.scss'],
  templateUrl    : './action-rds-modal.component.html',
  encapsulation  : ViewEncapsulation.None,
  providers: [
    {
      provide: MAT_DATE_FORMATS,
      useValue: {
        parse: { dateInput: null },
        display: {
          dateInput: { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' },
          monthYearLabel: { year: 'numeric', month: 'short' },
          dateA11yLabel: { year: 'numeric', month: 'long', day: 'numeric' },
          monthYearA11yLabel: { year: 'numeric', month: 'long' },
        },
      },
    },
  ],

  standalone: false
})
export class ActionRdsModalComponent implements OnInit, OnDestroy
{
  @ViewChild('stepper') stepper: MatStepper;
  formSave: FormGroup;
  private _unsubscribeAll: Subject<any> = new Subject<any>();
  firstFormGroup;
  secondFormGroup;
  dataSource: {[key: string]: MatTableDataSource<RDSModel>};
  service: ActionRdsService;
  private _model: ActionRdsModel;
  schemas: string[];
  tableSpaces: string[];
  buckets: {[key: string]: BucketModel[] } = {};
  filteredBuckets: ReplaySubject<{[key: string]: BucketModel[] }> = new ReplaySubject<{[key: string]: BucketModel[] }>(1);
  filteredSchemas: ReplaySubject<string[]> = new ReplaySubject<string[]>(1);
  breadcrumb: {[key: string]: string[] } = {};
  currentPath: string = '';  // Caminho atual
  fileFilter: string;
  databaseFilter: string;
  rebuildStepper = false;
  private _lastItemTapTime = 0;
  private _lastItemTapKey: string | null = null;
  private _lastDbTapTime = 0;
  private _lastDbTapKey: string | null = null;
  public customPatterns = { 'I': { pattern: new RegExp('\[a-zA-Z0-9_\-\]')} };
  get title(): string {
    return this.model && this.model.actionType === ActionRDSTypeEnum.RESTORE ? 'Restore RDS' : 'Backup RDS';
  }
  get accounts(): string[] {
    return Object.keys(this.dataSource);
  }

  get model(): ActionRdsModel {
    return this._model;
  }

  set model(value: ActionRdsModel) {
    this._model = value;
  }

  constructor(public _userService: UserService,
              private _formBuilder: FormBuilder,
              private _changeDetectorRef: ChangeDetectorRef,
              public dialogRef: MatDialogRef<ActionRdsModalComponent>,
              private _messageService: MessageDialogService)
  {
    this.firstFormGroup = this._formBuilder.group({
      firstCtrl: ['', Validators.required],
    });
    this.secondFormGroup = this._formBuilder.group({
      secondCtrl: ['', Validators.required],
    });
  }

  ngOnInit(): void {
    this.formSave = this._formBuilder.group({
      id: [''],
      actionType: ['', Validators.required],
      schedulerDate: [{value: null, disabled: true}],
      excludeBlobs: [false],
      destinationDatabase: ['', this.model.actionType === ActionRDSTypeEnum.RESTORE ? Validators.required : ''],
      destinationPassword: ['', this.model.actionType === ActionRDSTypeEnum.RESTORE ? Validators.required : ''],
      sourceDatabase: ['', this.model.actionType === ActionRDSTypeEnum.BACKUP ? Validators.required : ''],
      rds: this._formBuilder.group({
        id: ['', Validators.required],
        endpoint: [''],
        engine: [''],
        port: [''],
        dbName: [''],
        account: [''],
        username: ['', Validators.required],
        password: ['', Validators.required]
      }),
      dumpFile: this._formBuilder.group({
        name: ['', Validators.required],
        path: ['', Validators.required],
        arn: ['', Validators.required],
        type: ['', Validators.required],
        account: ['', Validators.required]
      }),
      remaps: this._formBuilder.group({
        [ActionRDSRemapTypeEnum.SCHEMA]: this._formBuilder.array([
          this._formBuilder.group({
            source: [],
            destination: []
          })
        ]),
        [ActionRDSRemapTypeEnum.TABLESPACE]: this._formBuilder.array([
          this._formBuilder.group({
            source: [],
            destination: [null, this.model.actionType === ActionRDSTypeEnum.RESTORE ? Validators.required : '']
          })
        ]),
        [ActionRDSRemapTypeEnum.DUMP_DIR]: this._formBuilder.array([
          this._formBuilder.group({
            source: [],
            destination: []
          })
        ])
      })
    });
    //this.model.rds.username = 'portalevolui';
    //this.model.rds.password = 'QKDquRTJBBdM2MvS';
    this.accounts.forEach(account => {
      this.breadcrumb[account] = ['Home'];
    });
    // Sempre que destinationDatabase mudar, atualiza o destination do SCHEMA
    this.formSave.get('destinationDatabase')?.valueChanges.subscribe(value => {
      const schemaArray = this.formSave.get('remaps')?.get('SCHEMA') as FormArray;
      if (schemaArray && schemaArray.length > 0) {
        (schemaArray.at(0) as FormGroup).get('destination')?.patchValue(value, { emitEvent: false });
      }
    });
    if (UtilFunctions.isValidStringOrArray(this.model.id) === false) {
      this.formSave.patchValue(this.model);
    }
    else { // usuario clicou no clone
      this.model.id = null;
      this.model.schedulerDate = null;

      const rds = this.dataSource[this.model.rds.account].data.find(r => r.id === this.model.rds.id);
      if (rds) {
        const models = this.accounts.map(account => {
          const bucket = new BucketModel();
          bucket.account = account;
          if (account === this.model.dumpFile.account) {
            bucket.path = this.model.dumpFile.path.replace(this.model.dumpFile.name, '');
          }
          return bucket;
        });

        const promises = [];
        promises.push(this.service.retrieveSchemas(this.model.rds));
        promises.push(this.service.retrieveTableSpaces(this.model.rds));
        promises.push(...models.map(bucket => {
          return this.service.retrieveBuckets(bucket)
        }));

        Promise.all(promises).then(results => {
          this.schemas = results[0];
          this.tableSpaces = results[1];
          this.buckets = {};
          for (let i = 2; i < results.length; i++) {
            this.buckets[this.accounts[i-2]] = results[i];
          }
          this.filteredBuckets.next(this.buckets);
          this.filteredSchemas.next(this.schemas);
          this.updateBreadcrumb(this.model.dumpFile.path.replace(this.model.dumpFile.name, ''), this.model.dumpFile.account);
          if (this.model.actionType === ActionRDSTypeEnum.RESTORE) {
            if (this.buckets[this.model.dumpFile.account].filter(f => UtilFunctions.arePathsEqual(f.path, this.model.dumpFile.path)).length <= 0) {
              this.model.dumpFile = null;
            }
            if (this.model.remaps[ActionRDSRemapTypeEnum.TABLESPACE]) {
              const tablespaces = this.model.remaps[ActionRDSRemapTypeEnum.TABLESPACE];
              if (Array.isArray(tablespaces) && tablespaces.length > 0) {
                let allValid = true;
                for (let i = 0; i < tablespaces.length; i++) {
                  const tablespace = tablespaces[i];
                  if (i > 0) {
                    this.addTablespaceMapping();
                  }
                  if (tablespace && tablespace.destination) {
                    if (!this.tableSpaces.includes(tablespace.destination)) {
                      allValid = false;
                      break;
                    }
                  }
                }
                if (!allValid) {
                  // Redefinir todas as destinations de tablespace
                  const tablespaceArray = this.formSave.get('remaps')?.get('TABLESPACE') as FormArray;
                  if (tablespaceArray) {
                    for (let i = 0; i < tablespaceArray.length; i++) {
                      (tablespaceArray.at(i) as FormGroup).get('destination')?.patchValue(null);
                    }
                  }
                }
              }
            }
          }
          else {
            if (this.schemas.includes(this.model.sourceDatabase) === false) {
              this.model.sourceDatabase = null;
            }
          }
          this.formSave.patchValue(this.model);
          this.stepper.next();
        })

      }
      else {
        const model = new ActionRdsModel();
        model.actionType = this.model.actionType;
        this.model = model;
        this.formSave.patchValue(this.model);

      }
    }
  }

  ngOnDestroy(): void {
    this._unsubscribeAll.next(undefined);
    this._unsubscribeAll.complete();
  }

  retrieveBuckets(model: BucketModel) {
    if (model === null) {
      if (UtilFunctions.isValidStringOrArray(Object.keys(this.buckets)) === true) {
        this.stepper.next();
        return;
      }
      const models = this.accounts.map(account => {
        const bucket = new BucketModel();
        bucket.account = account;
        return bucket;
      });

      models.reduce((promise, m, i) => {
        return promise.then(() =>
          this.service.retrieveBuckets(m).then(value => {
            this.buckets[m.account] = value;
          })
        );
      }, Promise.resolve())
        .then(() => {
          this.stepper.next();
          this.filterFiles();
        })
        .catch(error => {
          console.error(error);
        });
    }
    else {
      this.service.retrieveBuckets(model)
        .then(value => {
          this.buckets[model.account] = value;
          this.filterFiles();
        })
        .catch(error => {
          console.error(error);
        });
    }
  }

  checkDuplicatedFile() {
    if (this.model.actionType === ActionRDSTypeEnum.RESTORE) {
      this.stepper.next();
      return;
    }
    let name = this.formSave.get('dumpFile').get('name').value;
    name = name + this.getDatabaseSuffix();
    const dumpFile = this.formSave.get('dumpFile').value as BucketModel;

    const completePath = UtilFunctions.joinPaths(dumpFile.path, name);
    //console.log(dumpFile.path, dumpFile.name, completePath);
    if (this.buckets[dumpFile.account].filter(f => UtilFunctions.arePathsEqual(f.path, completePath)).length > 0) {
      this._messageService.open(`O arquivo ${completePath} já existe no bucket. Ele será sobrescrito. Deseja continuar?`, 'Confirmação', 'confirm').subscribe((result) => {
        if (result === 'confirmed') {
          this.stepper.next();
        }
      });
    }
    else {
      this.stepper.next();
    }

  }

  retrieveSchemas() {
    if (UtilFunctions.isValidStringOrArray(this.schemas) === true) {
      this.stepper.next();
      return;
    }
    this.service.retrieveSchemas(this.formSave.get('rds').value)
      .then(value => {
        this.schemas = value;
        this.filterSchemas();
        this.stepper.next();
      }).catch(error => {
      console.error(error);
    });
  }

  retrieveTableSpaces() {
    if (UtilFunctions.isValidStringOrArray(this.tableSpaces) === true) {
      this.stepper.next();
      return;
    }
    this.service.retrieveTableSpaces(this.formSave.get('rds').value)
      .then(value => {
        this.tableSpaces = value;
        this.stepper.next();
      }).catch(error => {
      console.error(error);
    });
  }

  updateBreadcrumb(path: string, account: string) {
    this.currentPath = path;
    if (!this.breadcrumb) {
      this.breadcrumb = {};
    }
    this.breadcrumb[account] = path ? ['Home', ...path.split('/').filter(p => p)] : ['Home'];
  }

  navigateTo(account: string, index: number) {
    const newPath = this.breadcrumb[account].slice(1, index + 1).join('/');
    this.updateBreadcrumb(newPath, account);
    const bucket = new BucketModel();
    bucket.path = newPath;
    bucket.account = account;
    if (this.model.actionType === ActionRDSTypeEnum.BACKUP && UtilFunctions.isValidStringOrArray(newPath) === false) {
      this.formSave.get('dumpFile').patchValue({
        path: '',
        arn: '',
        type: '',
        account: account
      })
    }
    this.retrieveBuckets(bucket);
  }

  onItemClick(account: string,item: BucketModel) {
    if (item.type === BucketFileTypeEnum.FILE) {
      if (this.model.actionType === ActionRDSTypeEnum.BACKUP) {
        /*
        let name = UtilFunctions.removeFileExtension(item.name);
        this.formSave.get('dumpFile').patchValue(item);
        this.formSave.get('dumpFile').get('name').patchValue(name);

         */
      }
      else {
        this.formSave.get('dumpFile').patchValue(item);
      }
      return;
    }

    // Cria o novo path baseado no breadcrumb atual
    const newPath = item.path;

    this.updateBreadcrumb(newPath, account);
    const bucket = new BucketModel();
    bucket.path = newPath;
    bucket.account = account;
    if (this.model.actionType === ActionRDSTypeEnum.BACKUP) {
      this.formSave.get('dumpFile').patchValue({
        path: newPath,
        arn: item.arn,
        type: BucketFileTypeEnum.FILE,
        account: account
      });
    }
    this.retrieveBuckets(bucket);
  }

  filterFiles() {
    if (UtilFunctions.isValidStringOrArray(this.fileFilter) === false) {
      this.filteredBuckets.next(this.buckets);
      return;
    }
    const filtered = Object.keys(this.buckets).reduce((acc, account) => {
      acc[account] = this.buckets[account].filter(b => UtilFunctions.removeAccents(b.name).toUpperCase().includes( UtilFunctions.removeAccents(this.fileFilter).toUpperCase()));
      return acc;
    }, {});
    this.filteredBuckets.next(filtered);
  }

  filterSchemas() {
    if (UtilFunctions.isValidStringOrArray(this.databaseFilter) === false) {
      this.filteredSchemas.next(this.schemas);
      return;
    }
    const filtered = this.schemas.filter(s => UtilFunctions.removeAccents(s).toUpperCase().includes(UtilFunctions.removeAccents(this.databaseFilter).toUpperCase()));
    this.filteredSchemas.next(filtered);
  }

  rowDoubleClicked(row: RDSModel) {
    if (row.instanceState !== 'available') {
      this._messageService.open('A instância não está disponível para uso.', 'Atenção', 'warning');
      return;
    }
    if (row.busy) {
      this._messageService.open('A instância já está sendo usada para outro backup ou restore.', 'Atenção', 'warning');
      return;
    }
    if (row.engine.toLowerCase().includes("aurora")) {
      this._messageService.open('A instância não é suportada.', 'Atenção', 'warning');
      return;
    }
    if (!row.engine.toLowerCase().includes("oracle") && !row.engine.toLowerCase().includes("sqlserver") && !row.engine.toLowerCase().includes("postgres")) {
      this._messageService.open('A instância não é suportada.', 'Atenção', 'warning');
      return;
    }

    //row.username = 'portalevolui';
    //row.password = 'QKDquRTJBBdM2MvS';
    this.formSave.get('rds').patchValue(row);
    this.formSave.get('sourceDatabase').patchValue('');
    this.formSave.get('destinationDatabase').patchValue('');
    this.formSave.get('dumpFile').get('name').patchValue(null);
    if (this.hasMap()){
      this.rebuildStepper = true;
      setTimeout(() => {
        this.rebuildStepper = false;
        this._changeDetectorRef.detectChanges();
        setTimeout(() => {  this.retrieveBuckets(null); }, 100);
      }, 100);
    }
    else {
      this.retrieveBuckets(null);
    }

  }

  stepChanged(event: StepperSelectionEvent) {
    if (event.selectedIndex === 0) {
      this.formSave.get('rds').patchValue(null)
    }
    if (event.selectedIndex < 2) {
      this.schemas = [];
    }
    /*
    if (event.selectedIndex <= 2) {
      this.updateBreadcrumb('');
      this.formSave.get('dumpFile').patchValue({
        path: '',
        arn: '',
        account: '',
        type: BucketFileTypeEnum.FILE
      })
    }
     */
  }

  onDatabaseClick(item: string) {
    if (this.model.actionType === ActionRDSTypeEnum.BACKUP) {
      this.formSave.get('sourceDatabase').patchValue(item);
    }
    else if (this.model.actionType === ActionRDSTypeEnum.RESTORE) {
      this.formSave.get('destinationDatabase').patchValue(item);
    }
  }

  changeScheduler(event: MatSlideToggleChange) {
    const schedulerDateCtrl = this.formSave.get("schedulerDate");
    if (event.checked) {
      schedulerDateCtrl.enable();
      schedulerDateCtrl.setValue(null);
      schedulerDateCtrl.setValidators(Validators.required);
    } else {
      schedulerDateCtrl.setValue(null);
      schedulerDateCtrl.clearValidators();
      schedulerDateCtrl.disable();
    }
    schedulerDateCtrl.updateValueAndValidity();
  }

  // Métodos para gerenciar o FormArray de tablespaces
  getTablespaceMappings(): FormArray {
    return this.formSave.get('remaps')?.get('TABLESPACE') as FormArray;
  }

  addTablespaceMapping(): void {
    const tablespaceArray = this.getTablespaceMappings();

    // Valor default para o novo mapeamento
    const newMapping = this._formBuilder.group({
      source: [''],
      destination: ['', this.model.actionType === ActionRDSTypeEnum.RESTORE ? Validators.required : '']
    });

    // Adiciona o novo mapeamento ao array
    tablespaceArray.push(newMapping);
  }

  removeTablespaceMapping(index: number): void {
    const tablespaceArray = this.getTablespaceMappings();
    if (tablespaceArray.length > 1) {
      tablespaceArray.removeAt(index);
    }
  }

  // Verifica se um tablespace origem já existe nos mapeamentos
  isTablespaceSourceDuplicate(source: string): boolean {
    if (!source || source.trim() === '') {
      return false; // Não consideramos vazios como duplicados
    }

    const tablespaceArray = this.getTablespaceMappings();
    let count = 0;

    for (let i = 0; i < tablespaceArray.length; i++) {
      const mapping = tablespaceArray.at(i) as FormGroup;
      const currentSource = mapping.get('source')?.value;

      if (currentSource && currentSource.trim() === source.trim()) {
        count++;
        if (count > 1) {
          return true;
        }
      }
    }

    return false;
  }

  // Validar tablespace ao mudar o valor
  validateTablespaceMapping(index: number): void {
    const tablespaceArray = this.getTablespaceMappings();
    const currentGroup = tablespaceArray.at(index) as FormGroup;
    const source = currentGroup.get('source')?.value;

    if (this.isTablespaceSourceDuplicate(source)) {
      this._messageService.open(
        `O tablespace de origem "${source}" já foi mapeado. Cada tablespace de origem deve ser único.`,
        'Atenção',
        'warning'
      );
      currentGroup.get('source')?.patchValue('');
    }
  }

  getMinDate() {
    const now = new Date();
    const minDate = new Date();
    minDate.setMinutes(now.getMinutes() + 1);
    return minDate;
  }

  save() {
    // Obter valores do formulário
    const formData = this.formSave.getRawValue();

    // Converter os valores para formato compatível com backend
    if (formData.remaps) {
      // Não precisamos converter, pois o backend agora espera arrays
    }

    this.model = formData;
    if (this.model.actionType === ActionRDSTypeEnum.BACKUP) {
      const fileName = this.formSave.get('dumpFile').get('name').value;
      this.model.dumpFile.name = fileName + this.getDatabaseSuffix();
    }

    this.service.save(this.model).then(value => {
      this._messageService.open(this.model.actionType === ActionRDSTypeEnum.BACKUP ? 'Backup programado com sucesso.' : 'Restore programado com sucesso.', 'Sucesso', 'success');
      this.dialogRef.close(value);
    });
  }

  getDatabaseSuffix() {
    const rds = this.formSave.get('rds').value;
    if (rds && rds.engine) {
      if (rds.engine.toLowerCase().includes("oracle")) {
        return '.dmp';
      }
      if (rds.engine.toLowerCase().includes("sqlserver")) {
        return '.bkp';
      }
      if (rds.engine.toLowerCase().includes("postgres")) {
        return '.pgdump';
      }
    }
  }

  hasMap() {
    const rds = this.formSave.get('rds').value;
    return rds && rds.engine && rds.engine.toLowerCase().includes("oracle");
  }

  hasNoblobs() {
    const rds = this.formSave.get('rds').value;
    return rds && rds.engine && rds.engine.toLowerCase().includes("postgres");
  }

  checkDuplicatedDatabase() {
    if (this.model.actionType === ActionRDSTypeEnum.BACKUP) {
      this.checkNeedSearchTableSpaces();
      return;
    }

    const database = this.formSave.get('destinationDatabase').value;
    if (this.schemas.includes(database)) {
      this._messageService.open(`O banco de dados ${database} já existe no RDS. Ele será removido. Deseja continuar?`, 'Confirmação', 'confirm').subscribe((result) => {
        if (result === 'confirmed') {
          this.checkNeedSearchTableSpaces();
        }
      });
    }
    else {
      this.checkNeedSearchTableSpaces();
    }

  }

  checkNeedSearchTableSpaces() {
    if (!this.hasMap()) {
      this.stepper.next();
      return;
    }
    if (this.model.actionType === ActionRDSTypeEnum.BACKUP) {
      this.stepper.next();
      return;
    }
    this.retrieveTableSpaces();
  }

  /** Double-tap fallback for bucket/file items on touch devices */
  onItemTap(account: string, item: BucketModel) {
    const now = Date.now();
    const key = account + ':' + item.name;
    if (this._lastItemTapKey === key && (now - this._lastItemTapTime) < 400) {
      this._lastItemTapTime = 0;
      this._lastItemTapKey = null;
      this.onItemClick(account, item);
      return;
    }
    this._lastItemTapTime = now;
    this._lastItemTapKey = key;
  }

  /** Double-tap fallback for database/schema items on touch devices */
  onDatabaseTap(item: string) {
    const now = Date.now();
    if (this._lastDbTapKey === item && (now - this._lastDbTapTime) < 400) {
      this._lastDbTapTime = 0;
      this._lastDbTapKey = null;
      this.onDatabaseClick(item);
      return;
    }
    this._lastDbTapTime = now;
    this._lastDbTapKey = item;
  }
}
