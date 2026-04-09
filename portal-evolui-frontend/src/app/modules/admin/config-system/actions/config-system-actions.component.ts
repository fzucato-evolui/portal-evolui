import {ChangeDetectorRef, Component, Input, OnInit, ViewEncapsulation} from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormGroup,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import {MatAutocompleteTrigger} from '@angular/material/autocomplete';
import {MatDialog} from '@angular/material/dialog';
import {MatTableDataSource} from '@angular/material/table';
import {cloneDeep} from 'lodash-es';
import {
  ActionConfigModel,
  ActionConfigTypeEnum,
  ActionsConfigModel,
  ActionTriggerConfigModel,
  ActionTriggerEnum,
  ActionVersionCreationScopeModel,
  SystemConfigModel,
  SystemConfigModelEnum
} from '../../../../shared/models/system-config.model';
import {VersionTypeEnum} from '../../../../shared/models/version.model';
import {MessageDialogService} from '../../../../shared/services/message/message-dialog-service';
import {ConfigSystemComponent} from '../config-system.component';
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {ProjectModel} from '../../../../shared/models/project.model';
import {RDSModel} from '../../../../shared/models/rds.model';
import {ActionRdsModel, ActionRDSTypeEnum} from '../../../../shared/models/action-rds.model';
import {ActionRdsModalComponent} from '../../aws/action-rds/modal/action-rds-modal.component';
import {ActionRdsService} from '../../aws/action-rds/action-rds.service';

@Component({
  selector       : 'config-system-actions',
  templateUrl    : './config-system-actions.component.html',
  styleUrls      : ['./config-system-actions.component.scss'],
  encapsulation  : ViewEncapsulation.None,

  standalone: false
})
export class ConfigSystemActionsComponent implements OnInit {
  private readonly _actionTypesByTrigger: Record<ActionTriggerEnum, ActionConfigTypeEnum[]> = {
    [ActionTriggerEnum.VERSION_CREATION]: [
      ActionConfigTypeEnum.DATABASE_CLONE
    ]
  };

  actionsForm: FormGroup;

  _model: SystemConfigModel = new SystemConfigModel();
  @Input()
  set model(value: SystemConfigModel) {
    if (value && value.id !== this._model.id) {
      this._model = cloneDeep(value);
      this.init();
    }
  }

  get model(): SystemConfigModel {
    return this._model;
  }

  actionsModel: ActionsConfigModel;
  ActionTriggerEnum = ActionTriggerEnum;
  ActionConfigTypeEnum = ActionConfigTypeEnum;
  pendingActionTypes: {[key: number]: ActionConfigTypeEnum} = {};
  versionTypes = Object.values(VersionTypeEnum);
  versionTokens = this.buildVersionTokens();
  filteredTemplateTokens: string[] = [];
  private _activeTemplateTarget: {
    actionControl: AbstractControl,
    field: 'sourceDatabase' | 'destinationDatabase',
    input: HTMLInputElement,
    originalValue: string,
    replaceStart: number,
    replaceEnd: number
  } | null = null;

  private readonly _resultFieldPaths: string[] = [
    'actionType',
    'sourceDatabase',
    'destinationDatabase',
    'destinationPassword',
    'status',
    'conclusion',
    'restoreKey',
    'error',
    'excludeBlobs',
    'rds.id',
    'rds.endpoint',
    'rds.engine',
    'rds.dbName',
    'rds.account',
    'dumpFile.name',
    'dumpFile.path',
    'dumpFile.arn',
    'dumpFile.account'
  ];

  public get produtos(): Array<ProjectModel> {
    return this.parent.produtos;
  }

  public get databases(): Array<RDSModel> {
    return this.parent.initialData?.rds || [];
  }

  constructor(
    private _formBuilder: FormBuilder,
    private _messageService: MessageDialogService,
    private _changeDetectorRef: ChangeDetectorRef,
    private _matDialog: MatDialog,
    private _actionRdsService: ActionRdsService,
    public parent: ConfigSystemComponent
  ) {
  }

  ngOnInit(): void {
    this.init();
  }

  init() {
    if (!this.actionsForm) {
      this.actionsForm = this._formBuilder.group({
        configs: this._formBuilder.array([])
      });
    }

    this.actionsModel = new ActionsConfigModel();
    this.getConfigs().clear();

    if (this.model && this.model.id > 0 && this.model.configType === SystemConfigModelEnum.ACTIONS) {
      this.actionsModel = this.model.config ? this.model.config as ActionsConfigModel : new ActionsConfigModel();
      if (UtilFunctions.isValidStringOrArray(this.actionsModel.configs) === true) {
        this.actionsModel.configs.forEach(config => this.addConfig(config));
      }
    }

    if (this.getConfigs().length === 0) {
      this.addTrigger(ActionTriggerEnum.VERSION_CREATION);
    }

    this.rebuildPendingActionTypes();
  }

  salvar() {
    this.model.configType = SystemConfigModelEnum.ACTIONS;
    this.actionsModel = this.actionsForm.getRawValue();
    this.model.config = this.actionsModel;
    this.parent.service.save(this.model)
      .then(value => {
        this.model = value;
        this._messageService.open('Configuração de Actions salva com sucesso', 'SUCESSO', 'success');
      });
  }

  getConfigs(): FormArray {
    return this.actionsForm.get('configs') as FormArray;
  }

  trackByIndex(index: number): number {
    return index;
  }

  getScope(config: AbstractControl): FormGroup {
    return config.get('scope') as FormGroup;
  }

  getActions(config: AbstractControl): FormArray {
    return this.getScope(config).get('actions') as FormArray;
  }

  addTrigger(triggerType: ActionTriggerEnum) {
    const model = new ActionTriggerConfigModel();
    model.triggerType = triggerType;
    model.scope = new ActionVersionCreationScopeModel();
    this.addConfig(model);
  }

  addConfig(model?: ActionTriggerConfigModel) {
    this.getConfigs().push(this.createTriggerGroup(model));
    this.rebuildPendingActionTypes();
  }

  removeTrigger(index: number) {
    this.getConfigs().removeAt(index);
    if (this.getConfigs().length === 0) {
      this.addTrigger(ActionTriggerEnum.VERSION_CREATION);
    }

    this.rebuildPendingActionTypes();
  }

  addAction(configIndex: number, type: ActionConfigTypeEnum) {
    const action = new ActionConfigModel();
    action.type = type;
    action.payload = null;

    const actions = this.getActions(this.getConfigs().at(configIndex));
    const actionControl = this.createActionGroup(action);
    actions.push(actionControl);
  }

  addSelectedAction(configIndex: number) {
    const type = this.getPendingActionType(configIndex);
    if (!type) {
      return;
    }
    this.addAction(configIndex, type);
  }

  getAvailableActionTypes(configIndex: number): ActionConfigTypeEnum[] {
    const triggerType = this.getConfigs().at(configIndex)?.get('triggerType')?.value as ActionTriggerEnum;
    return this._actionTypesByTrigger[triggerType] || [];
  }

  getPendingActionType(configIndex: number): ActionConfigTypeEnum | null {
    const availableTypes = this.getAvailableActionTypes(configIndex);
    if (availableTypes.length === 0) {
      return null;
    }

    const currentType = this.pendingActionTypes[configIndex];
    return availableTypes.includes(currentType) ? currentType : availableTypes[0];
  }

  setPendingActionType(configIndex: number, type: ActionConfigTypeEnum) {
    this.pendingActionTypes[configIndex] = type;
  }

  removeAction(configIndex: number, actionIndex: number) {
    const actions = this.getActions(this.getConfigs().at(configIndex));
    actions.removeAt(actionIndex);
    this.normalizeDependencies(actions, actionIndex);
  }

  getActionTypeLabel(type: ActionConfigTypeEnum): string {
    if (type === ActionConfigTypeEnum.DATABASE_CLONE) {
      return 'Clone de Banco';
    }
    return type;
  }

  getActionTitle(action: AbstractControl, index: number): string {
    return `Action ${index + 1} - ${this.getActionTypeLabel(action.get('type')?.value)}`;
  }

  getDependsOnOptions(configIndex: number, actionIndex: number): Array<{label: string, value: number}> {
    const actions = this.getActions(this.getConfigs().at(configIndex));
    const options = [];
    for (let i = 0; i < actionIndex; i++) {
      options.push({ label: this.getActionTitle(actions.at(i), i), value: i });
    }
    return options;
  }

  isDependencySelected(action: AbstractControl, dependencyIndex: number): boolean {
    return this.normalizeDependencyList(action.get('dependsOn')?.value).includes(dependencyIndex);
  }

  toggleDependency(configIndex: number, actionIndex: number, dependencyIndex: number) {
    const action = this.getActions(this.getConfigs().at(configIndex)).at(actionIndex);
    const current = this.normalizeDependencyList(action.get('dependsOn')?.value);
    const exists = current.includes(dependencyIndex);
    const nextValues = exists
      ? current.filter(index => index !== dependencyIndex)
      : [...current, dependencyIndex].sort((a, b) => a - b);

    action.get('dependsOn')?.patchValue(nextValues);
    this.onDependencyChange(configIndex, actionIndex);
  }

  getDependencyLabel(action: AbstractControl): string {
    const dependsOn = this.normalizeDependencyList(action.get('dependsOn')?.value);
    if (dependsOn.length === 0) {
      return 'Sem dependências';
    }
    return dependsOn.map(index => `Action ${index + 1}`).join(', ');
  }

  isDatabaseCloneAction(action: AbstractControl): boolean {
    return action.get('type')?.value === ActionConfigTypeEnum.DATABASE_CLONE;
  }

  getActionPayload(action: AbstractControl): ActionRdsModel | null {
    return action.get('payload')?.value || null;
  }

  getDatabaseCloneSummary(action: AbstractControl): string {
    const payload = this.getActionPayload(action);
    if (!payload?.rds?.id) {
      return 'Clone ainda não configurado.';
    }

    const parts = [
      payload.rds?.endpoint ? payload.rds.endpoint : null,
      payload.sourceDatabase ? `origem: ${payload.sourceDatabase}` : null,
      payload.destinationDatabase ? `destino: ${payload.destinationDatabase}` : null
    ].filter(Boolean);

    return parts.join(' | ');
  }

  onDependencyChange(configIndex: number, actionIndex: number) {
    const action = this.getActions(this.getConfigs().at(configIndex)).at(actionIndex);
    const validValues = this.normalizeDependencyList(action.get('dependsOn')?.value)
      .filter(index => index >= 0 && index < actionIndex)
      .filter((value, index, array) => array.indexOf(value) === index);
    action.get('dependsOn')?.patchValue(validValues, { emitEvent: false });
    action.updateValueAndValidity({ emitEvent: false });
  }

  openDatabaseCloneModal(configIndex: number, actionIndex: number) {
    const actionControl = this.getActions(this.getConfigs().at(configIndex)).at(actionIndex);
    const currentPayload = cloneDeep(this.getActionPayload(actionControl)) || new ActionRdsModel();
    currentPayload.actionType = ActionRDSTypeEnum.CLONE;

    const modal = this._matDialog.open(ActionRdsModalComponent, {
      disableClose: true,
      panelClass: 'action-rds-modal-container'
    });

    modal.componentInstance.service = this._actionRdsService;
    modal.componentInstance.dataSource = this.buildRdsDataSource();
    modal.componentInstance.model = currentPayload;
    modal.componentInstance.editorMode = true;

    modal.afterClosed().subscribe((result: ActionRdsModel | null) => {
      if (!result) {
        return;
      }
      actionControl.get('payload')?.patchValue(result);
      actionControl.get('payload')?.markAsDirty();
      actionControl.get('payload')?.updateValueAndValidity();
      actionControl.updateValueAndValidity();
      this._changeDetectorRef.detectChanges();
    });
  }

  updateDatabaseName(action: AbstractControl, field: 'sourceDatabase' | 'destinationDatabase', value: string) {
    const payload = cloneDeep(this.getActionPayload(action)) || new ActionRdsModel();
    payload.actionType = payload.actionType || ActionRDSTypeEnum.CLONE;
    payload[field] = value;
    action.get('payload')?.patchValue(payload);
    action.get('payload')?.markAsDirty();
    action.updateValueAndValidity();
  }

  onTemplateInput(action: AbstractControl,
                  field: 'sourceDatabase' | 'destinationDatabase',
                  input: HTMLInputElement,
                  trigger: MatAutocompleteTrigger) {
    const context = this.extractTemplateContext(action, input.value || '', input.selectionStart ?? (input.value || '').length);
    if (!context) {
      this.filteredTemplateTokens = [];
      this._activeTemplateTarget = null;
      trigger.closePanel();
      return;
    }

    this._activeTemplateTarget = {
      actionControl: action,
      field,
      input,
      originalValue: input.value || '',
      replaceStart: context.start,
      replaceEnd: context.end
    };

    this.filteredTemplateTokens = this.getAvailableTokens(action, context.query);
    this._changeDetectorRef.detectChanges();

    if (this.filteredTemplateTokens.length > 0) {
      trigger.openPanel();
      return;
    }

    trigger.closePanel();
  }

  insertTemplateToken(token: string) {
    if (!this._activeTemplateTarget) {
      return;
    }

    const {actionControl, field, input, originalValue, replaceStart, replaceEnd} = this._activeTemplateTarget;
    const nextValue = originalValue.slice(0, replaceStart) + token + originalValue.slice(replaceEnd);
    this.updateDatabaseName(actionControl, field, nextValue);
    input.value = nextValue;

    const nextCursor = replaceStart + token.length;
    requestAnimationFrame(() => {
      input.focus();
      input.setSelectionRange(nextCursor, nextCursor);
    });

    this.filteredTemplateTokens = [];
  }

  getDependencyTokens(action: AbstractControl): string[] {
    const dependsOn = this.normalizeDependencyList(action.get('dependsOn')?.value);
    return dependsOn.flatMap(index => {
      const prefix = `$R${index + 1}{`;
      return this._resultFieldPaths.slice(0, 6).map(field => `${prefix}${field}}`);
    });
  }

  private rebuildPendingActionTypes() {
    const nextValues: {[key: number]: ActionConfigTypeEnum} = {};
    for (let i = 0; i < this.getConfigs().length; i++) {
      const availableTypes = this.getAvailableActionTypes(i);
      if (availableTypes.length === 0) {
        continue;
      }

      const currentType = this.pendingActionTypes[i];
      nextValues[i] = availableTypes.includes(currentType) ? currentType : availableTypes[0];
    }
    this.pendingActionTypes = nextValues;
  }

  private createTriggerGroup(model?: ActionTriggerConfigModel): FormGroup {
    return this._formBuilder.group({
      triggerType: [model?.triggerType || ActionTriggerEnum.VERSION_CREATION, Validators.required],
      scope: this.createScopeGroup(model?.scope)
    });
  }

  private createScopeGroup(model?: ActionVersionCreationScopeModel): FormGroup {
    const group = this._formBuilder.group({
      productId: [model?.productId ?? null, Validators.required],
      versionType: [model?.versionType ?? null, Validators.required],
      actions: this._formBuilder.array([])
    });

    const actions = group.get('actions') as FormArray;
    if (UtilFunctions.isValidStringOrArray(model?.actions) === true) {
      model.actions.forEach(action => actions.push(this.createActionGroup(action)));
    }

    return group;
  }

  private createActionGroup(model?: ActionConfigModel): FormGroup {
    return this._formBuilder.group({
      type: [model?.type || null, Validators.required],
      dependsOn: [this.normalizeDependencyList(model?.dependsOn)],
      payload: [cloneDeep(model?.payload || null)]
    }, { validators: this.actionPayloadValidator() });
  }

  private actionPayloadValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const type = control.get('type')?.value;
      const payload = control.get('payload')?.value as ActionRdsModel | null;
      if (type === ActionConfigTypeEnum.DATABASE_CLONE && !payload?.rds?.id) {
        return { payloadRequired: true };
      }
      return null;
    };
  }

  private normalizeDependencies(actions: FormArray, removedIndex: number) {
    actions.controls.forEach(control => {
      const current = this.normalizeDependencyList(control.get('dependsOn')?.value);
      const nextValues = current
        .filter(index => index !== removedIndex)
        .map(index => index > removedIndex ? index - 1 : index);
      control.get('dependsOn')?.patchValue(nextValues, { emitEvent: false });
    });
  }

  private buildVersionTokens(): string[] {
    return [
      '$V{major}',
      '$V{minor}',
      '$V{patch}',
      '$V{build}',
      '$V{qualifier}',
      '$V{tag}',
      '$V{branch}',
      '$V{commit}',
      '$V{repository}',
      '$V{repositoryBranch}',
      '$V{relativePath}',
      '$V{versionType}'
    ];
  }

  private buildRdsDataSource(): {[key: string]: MatTableDataSource<RDSModel>} {
    const grouped = {} as {[key: string]: MatTableDataSource<RDSModel>};
    (this.databases || []).forEach(rds => {
      const account = rds.account || 'DEFAULT';
      if (!grouped[account]) {
        grouped[account] = new MatTableDataSource<RDSModel>();
        grouped[account].data = [];
      }
      grouped[account].data.push(rds);
      grouped[account]._updateChangeSubscription();
    });
    return grouped;
  }

  private extractTemplateContext(action: AbstractControl, value: string, cursor: number): {start: number, query: string, end: number} | null {
    const prefix = value.substring(0, cursor);
    const versionIndex = prefix.lastIndexOf('$V{');
    let resultStart = -1;
    let resultQuery = '';
    const resultRegex = /\$R(\d+)\{[^}]*$/g;
    let match: RegExpExecArray | null;
    while ((match = resultRegex.exec(prefix)) !== null) {
      resultStart = match.index;
      resultQuery = match[0];
    }

    if (versionIndex < 0 && resultStart < 0) {
      return null;
    }

    if (resultStart > versionIndex) {
      const expectedPrefixes = this.getResultPrefixes(action);
      if (expectedPrefixes.length === 0 || !expectedPrefixes.some(prefix => resultQuery.startsWith(prefix))) {
        return null;
      }
      return { start: resultStart, query: resultQuery, end: cursor };
    }

    const versionQuery = prefix.substring(versionIndex);
    if (versionQuery.includes('}')) {
      return null;
    }

    return { start: versionIndex, query: versionQuery, end: cursor };
  }

  private getAvailableTokens(action: AbstractControl, query: string): string[] {
    let tokens: string[] = [];
    if (query.startsWith('$V{')) {
      tokens = this.versionTokens;
    } else if (query.startsWith('$R')) {
      const prefixes = this.getResultPrefixes(action);
      if (prefixes.length === 0) {
        return [];
      }
      tokens = prefixes.flatMap(prefix => this._resultFieldPaths.map(field => `${prefix}${field}}`));
    }

    const normalized = query.toLowerCase();
    return tokens.filter(token => token.toLowerCase().includes(normalized));
  }

  private getResultPrefixes(action: AbstractControl): string[] {
    return this.normalizeDependencyList(action.get('dependsOn')?.value).map(index => `$R${index + 1}{`);
  }

  private normalizeDependencyList(value: any): number[] {
    if (!Array.isArray(value)) {
      return [];
    }
    return value
      .filter(item => item !== null && item !== undefined && item !== '')
      .map(item => Number(item))
      .filter(item => Number.isInteger(item) && item >= 0);
  }
}
