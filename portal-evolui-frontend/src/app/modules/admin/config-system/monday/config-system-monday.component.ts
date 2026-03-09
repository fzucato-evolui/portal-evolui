import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnInit,
  ViewEncapsulation
} from "@angular/core";
import {FormBuilder, FormControl, FormGroup, Validators} from "@angular/forms";
import {cloneDeep} from "lodash-es";
import {
  MondayBoardModel,
  MondayColumnModel,
  MondayConfigModel,
  MondayGroupModel,
  SystemConfigModel,
  SystemConfigModelEnum
} from "../../../../shared/models/system-config.model";
import {MessageDialogService} from "../../../../shared/services/message/message-dialog-service";
import {ConfigSystemComponent} from "../config-system.component";
import {MatSlideToggleChange} from '@angular/material/slide-toggle';
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {MatSelectChange} from "@angular/material/select";


@Component({
  selector       : 'config-system-monday',
  templateUrl    : './config-system-monday.component.html',
  styleUrls      : ['./config-system-monday.component.scss'],
  encapsulation  : ViewEncapsulation.None,
  changeDetection: ChangeDetectionStrategy.OnPush,

  standalone: false
})
export class ConfigSystemMondayComponent implements OnInit, AfterViewInit{
  mondayForm: FormGroup;
  _model: SystemConfigModel = new SystemConfigModel();
  @Input()
  set model(value: SystemConfigModel) {
    if (value && value.id !== this._model.id) {
      this._model = cloneDeep(value);
      this.init();
    }
  };

  get model(): SystemConfigModel {
    return this._model;
  }

  get boards(): Array<MondayBoardModel> {
    return this.parent.initialData.boards;
  }

  get boardBuildGroups(): Array<MondayGroupModel> {
    return this.parent.initialData.boardBuildGroups;
  }

  get boardBuildColumns(): Array<MondayColumnModel> {
    return this.parent.initialData.boardBuildColumns;
  }

  get boardTaskColumns(): Array<MondayColumnModel> {
    return this.parent.initialData.boardTaskColumns;
  }

  mondayModel: MondayConfigModel;

  constructor(
    private _formBuilder: FormBuilder,
    private _messageService: MessageDialogService,
    private _changeDetectorRef: ChangeDetectorRef,
    public parent: ConfigSystemComponent
  )
  {
  }

  ngAfterViewInit(): void {
    if (this.mondayModel && this.mondayModel.versionGenerationConfig &&
      this.mondayModel.versionGenerationConfig.enabled === true) {
      if (this.mondayModel.versionGenerationConfig.allowedStatusValues) {
        const keys = Object.keys(this.mondayModel.versionGenerationConfig.allowedStatusValues);
        const values: Array<{ [key: string]: string}> = [];
        for (const key of keys) {
          const keyPair: { [key: string]: string} = {};
          keyPair[key] = this.mondayModel.versionGenerationConfig.allowedStatusValues[key];

          values.push(keyPair);
          const value = this.mondayModel.versionGenerationConfig.mappedStatusValues ? this.mondayModel.versionGenerationConfig.mappedStatusValues[key] : null;
          (this.mondayForm.get('versionGenerationConfig').get('mappedStatusValues') as FormGroup)
            .addControl(key, this._formBuilder.control(value, Validators.required));
        }
        this.mondayForm.get('versionGenerationConfig').get('allowedStatusValues').setValue(values);
      }
      if (this.mondayModel.versionGenerationConfig.allowedItemStatusValues) {
        const keys = Object.keys(this.mondayModel.versionGenerationConfig.allowedItemStatusValues);
        const values: Array<{ [key: string]: string}> = [];
        for (const key of keys) {
          const keyPair: { [key: string]: string} = {};
          keyPair[key] = this.mondayModel.versionGenerationConfig.allowedItemStatusValues[key];

          values.push(keyPair);
          const value = this.mondayModel.versionGenerationConfig.mappedItemStatusValues ? this.mondayModel.versionGenerationConfig.mappedItemStatusValues[key] : null;
          (this.mondayForm.get('versionGenerationConfig').get('mappedItemStatusValues') as FormGroup)
            .addControl(key, this._formBuilder.control(value));
        }
        this.mondayForm.get('versionGenerationConfig').get('allowedItemStatusValues').setValue(values);
      }

    }

  }

  ngOnInit(): void {
    this.init();
  }

  init() {
    if (!this.mondayForm) {
      this.mondayForm = this._formBuilder.group({
        enabled: [false, []],
        endpoint: ['', [Validators.required]],
        token: ['', [Validators.required]],
        page: ['', [Validators.required]],
        versionGenerationConfig: this._formBuilder.group({
          enabled: [false, []],
          taskBoardId: ['', []],
          boardId: ['', []],
          groupId: ['', []],
          columnProduct: ['', []],
          columnStatus: ['', []],
          columnItemsStatus: ['', []],
          columnItemsIncluded: ['', []],
          columnResponsable: ['', []],
          columnMajorMinor: ['', []],
          columnPatch: ['', []],
          columnBuild: ['', []],
          columnVersionType: ['', []],
          columnGenerationDate: ['', []],
          columnGenerationHour: ['', []],
          allowedStatusValues: [[], []],
          allowedItemStatusValues: [[], []],
          mappedStatusValues: this._formBuilder.group({}),
          mappedItemStatusValues: this._formBuilder.group({})
        })
      });
    }
    this.mondayModel = new MondayConfigModel();
    if (this.model && this.model.id > 0 && this.model.configType === SystemConfigModelEnum.MONDAY) {
      this.mondayModel = this.model.config ? this.model.config as MondayConfigModel : new MondayConfigModel();
    }
    this.mondayForm.patchValue(this.mondayModel);
    // Ensure multiple mat-select controls remain arrays after patchValue
    // (backend sends objects like {key: value} which patchValue blindly sets)
    const vgc = this.mondayForm.get('versionGenerationConfig');
    if (!Array.isArray(vgc.get('allowedStatusValues').value)) {
      vgc.get('allowedStatusValues').setValue([]);
    }
    if (!Array.isArray(vgc.get('allowedItemStatusValues').value)) {
      vgc.get('allowedItemStatusValues').setValue([]);
    }
    if (this.mondayModel && this.mondayModel.versionGenerationConfig &&
      this.mondayModel.versionGenerationConfig.enabled === true) {
      const event = new MatSlideToggleChange(null, true);
      this.changeEnabled(event);
    }
  }

  salvar() {
    this.model.configType = SystemConfigModelEnum.MONDAY;
    this.mondayModel = this.mondayForm.value;
    if (this.mondayModel.versionGenerationConfig.enabled === false) {
      //this.mondayModel.versionGenerationConfig = new MondayVersionGenerationConfigModel();
      //this.mondayModel.versionGenerationConfig.enabled = false;
    }
    this.model.config = this.mondayModel;
    this.parent.service.save(this.model)
      .then(value => {
        this.model = value;
        this._messageService.open('Configuração de Sistema para autenticação Monday salva com sucesso', 'SUCESSO', 'success');
      });
  }

  changeEnabled(event: MatSlideToggleChange) {
    const c = (this.mondayForm.get('versionGenerationConfig') as FormGroup).controls;
    const keys = Object.keys(c);
    for (const key of keys) {
      if (key !== 'enabled') {
        c[key].setValidators(event.checked ? Validators.required : null);
        c[key].updateValueAndValidity();
      }

    }
  }

  filteredBoards(value: string) {
    if (UtilFunctions.isValidStringOrArray(this.boards) === true && UtilFunctions.isValidStringOrArray(value)) {
      return this.boards.filter(x => UtilFunctions.removeAccents(x.name.toLowerCase()).includes(UtilFunctions.removeAccents(value.toLowerCase())));
    }
    return this.boards;
  }

  filteredGroups(value: string) {
    if (UtilFunctions.isValidStringOrArray(this.boardBuildGroups) === true && UtilFunctions.isValidStringOrArray(value)) {
      return this.boardBuildGroups.filter(x => UtilFunctions.removeAccents(x.title.toLowerCase()).includes(UtilFunctions.removeAccents(value.toLowerCase())));
    }
    return this.boardBuildGroups;
  }

  filteredColumns(value: string) {
    if (UtilFunctions.isValidStringOrArray(this.boardBuildColumns) === true && UtilFunctions.isValidStringOrArray(value)) {
      return this.boardBuildColumns.filter(x => UtilFunctions.removeAccents(x.title.toLowerCase()).includes(UtilFunctions.removeAccents(value.toLowerCase())));
    }
    return this.boardBuildColumns;
  }

  filteredColumnsType(value: string, type: string) {
    if (UtilFunctions.isValidStringOrArray(this.boardBuildColumns) === true) {
      if (UtilFunctions.isValidStringOrArray(value)) {
        return this.boardBuildColumns.filter(x =>
          UtilFunctions.removeAccents(x.title.toLowerCase()).includes(UtilFunctions.removeAccents(value.toLowerCase())) &&
          UtilFunctions.removeAccents(x.type.toLowerCase()).includes(UtilFunctions.removeAccents(type.toLowerCase()))
        );
      } else {
        return this.boardBuildColumns.filter(x =>
          UtilFunctions.removeAccents(x.type.toLowerCase()).includes(UtilFunctions.removeAccents(type.toLowerCase()))
        );
      }
    }
    return this.boardBuildColumns;
  }

  filteredTaskColumnsType(value: string, type: string) {
    if (UtilFunctions.isValidStringOrArray(this.boardTaskColumns) === true) {
      if (UtilFunctions.isValidStringOrArray(value)) {
        return this.boardTaskColumns.filter(x =>
          UtilFunctions.removeAccents(x.title.toLowerCase()).includes(UtilFunctions.removeAccents(value.toLowerCase())) &&
          UtilFunctions.removeAccents(x.type.toLowerCase()).includes(UtilFunctions.removeAccents(type.toLowerCase()))
        );
      } else {
        return this.boardTaskColumns.filter(x =>
          UtilFunctions.removeAccents(x.type.toLowerCase()).includes(UtilFunctions.removeAccents(type.toLowerCase()))
        );
      }
    }
    return this.boardTaskColumns;
  }


  updateControl(groupId: string) {
    this.mondayForm.get('versionGenerationConfig').get(groupId).updateValueAndValidity();
  }

  changeBuildBoard(event: MatSelectChange) {
    const c = (this.mondayForm.get('versionGenerationConfig') as FormGroup).controls;
    const keys = Object.keys(c);
    for (const key of keys) {
      if (key !== 'enabled' && key != 'boardId' && key != 'taskBoardId' && key != 'columnItemsStatus' && key != 'validItemsValues') {
        const fg = c[key] instanceof FormGroup ? c[key] : null;
        if (fg) {
          fg.setValue({});
          fg.updateValueAndValidity();
        } else {
          const fc = c[key] as FormControl;
          if (fc) {
            fc.setValue(null);
            fc.updateValueAndValidity();
          }
        }
      }

    }
    this.parent.service.getMondayBuildGroupsColumns(event.value);
  }

  changeTaskBoard(event: MatSelectChange) {
    const c = (this.mondayForm.get('versionGenerationConfig') as FormGroup).controls;
    const keys = Object.keys(c);
    for (const key of keys) {
      if (key === 'columnItemsStatus' || key === 'validItemsValues') {
        c[key].setValue(null);
        c[key].updateValueAndValidity();
      }

    }
    this.parent.service.getMondayTaskColumns(event.value);
  }


  getPossibletatusValues(): { [key: string]: string} {
    const statusColumn = (this.mondayForm.get('versionGenerationConfig') as FormGroup).get('columnStatus').value;
    if (UtilFunctions.isValidStringOrArray(statusColumn) === true && UtilFunctions.isValidStringOrArray(this.boardBuildColumns) === true) {
      return this.boardBuildColumns.filter(x => x.id === statusColumn.id)[0].possibleValues;
    }
    return null;
  }

  getPossibleItemsStatusValues(): { [key: string]: string} {
    const statusColumn = (this.mondayForm.get('versionGenerationConfig') as FormGroup).get('columnItemsStatus').value;
    if (UtilFunctions.isValidStringOrArray(statusColumn) === true && UtilFunctions.isValidStringOrArray(this.boardTaskColumns) === true) {
      return this.boardTaskColumns.filter(x => x.id === statusColumn.id)[0].possibleValues;
    }
    return null;
  }

  getKeyPairValue(b: {key: string, value: string}): { [key: string]: string} {
    const value: { [key: string]: string} = {};
    value[b.key + ''] = b.value;
    return value;
  }

  allowedStatusChanged(event: MatSelectChange) {

    const fg = this._formBuilder.group({});
    const selected = event.value as Array<{ [key: string]: string}>;
    if (UtilFunctions.isValidStringOrArray(selected) === true) {
      for (const x of selected) {
        const key = Object.keys(x);
        fg.addControl(key[0], this._formBuilder.control(null, Validators.required));
      }
    }
    (this.mondayForm.get('versionGenerationConfig') as FormGroup).setControl('mappedStatusValues', fg);
  }

  getMappedStatus(): FormGroup {
    const fg = (this.mondayForm.get('versionGenerationConfig').get('mappedStatusValues') as FormGroup);
    const keys = Object.keys(fg.controls);
    if (keys.length > 0) {
      return fg;
    }
    return null;
  }

  allowedItemStatusChanged(event: MatSelectChange) {

    const fg = this._formBuilder.group({});
    const selected = event.value as Array<{ [key: string]: string}>;
    if (UtilFunctions.isValidStringOrArray(selected) === true) {
      for (const x of selected) {
        const key = Object.keys(x);
        fg.addControl(key[0], this._formBuilder.control(null));
      }
    }
    (this.mondayForm.get('versionGenerationConfig') as FormGroup).setControl('mappedItemStatusValues', fg);

  }
  getMappedItemStatus(): FormGroup {
    const fg = (this.mondayForm.get('versionGenerationConfig').get('mappedItemStatusValues') as FormGroup);
    const keys = Object.keys(fg.controls);
    if (keys.length > 0) {
      return fg;
    }
    return null;
  }


  compareFn(o1: { [key: string]: string}, o2: { [key: string]: string}) {
    //console.log(o1, o2);
    if (o1 && o2) {
      const key1 = Object.keys(o1);
      const key2 = Object.keys(o2);
      //console.log(key1, key2, key1.toString() === key2.toString());
      return key1.toString() === key2.toString();
    }
    return false;
  }

  compareColumn(o1: MondayColumnModel, o2: MondayColumnModel) {
    //console.log(o1, o2);
    if (o1 && o2) {
      return o1.id === o2.id;
    }
    return false;
  }
}
