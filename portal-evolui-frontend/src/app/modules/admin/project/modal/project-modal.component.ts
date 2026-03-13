import {Component, OnDestroy, OnInit, ViewEncapsulation} from "@angular/core";
import {IconModel, ProjectModel, ProjectModuleModel,} from '../../../../shared/models/project.model';
import {Subject} from 'rxjs';
import {ProjectService} from '../project.service';
import {AbstractControl, FormArray, FormBuilder, FormControl, FormGroup, Validators} from '@angular/forms';
import {MessageDialogService} from '../../../../shared/services/message/message-dialog-service';
import {MatDialog, MatDialogRef} from '@angular/material/dialog';
import {UtilFunctions} from '../../../../shared/util/util-functions';
import {ProjectRepositoryModalComponent} from './project-repository-modal.component';
import {ProjectFolderModalComponent} from './project-folder-modal.component';
import {DetailedRepositoryGithubModel} from '../../../../shared/models/github.model';

@Component({
  selector       : 'project-modal',
  styleUrls      : ['/project-modal.component.scss'],
  templateUrl    : './project-modal.component.html',
  encapsulation  : ViewEncapsulation.None,

  standalone: false
})
export class ProjectModalComponent implements OnInit, OnDestroy
{
  formSave: FormGroup;
  private _unsubscribeAll: Subject<any> = new Subject<any>();

  public model: ProjectModel;
  title: string = "Projeto";
  service: ProjectService;

  public customPatterns = { 'I': { pattern: new RegExp('\[a-zA-Z0-9\-\]')} };

  constructor(private _formBuilder: FormBuilder,
              private _matDialog: MatDialog,
              public dialogRef: MatDialogRef<ProjectModalComponent>,
              private _messageService: MessageDialogService)
  {
  }

  ngOnInit(): void {
    this.formSave = this.buildModule(true);
    if (this.model &&
      this.model.id && this.model.id > 0 &&
      UtilFunctions.isValidStringOrArray(this.model.modules)
    ) {
      for(const x of this.model.modules) {
        if (x.bond == null) {
          const c = this.addModule();
          this.addBondsRecursively(c, x.childBonds);
        }
      }

    }
    // Filter to root modules only for patchValue (form structure is tree, not flat)
    const patchModel = {...this.model, modules: this.model.modules?.filter(x => x.bond == null)};
    this.formSave.patchValue(patchModel);


  }

  ngOnDestroy(): void {
    this._unsubscribeAll.next(undefined);
    this._unsubscribeAll.complete();
  }


  doSaving() {
    this.model = this.formSave.value as ProjectModel;

    this.service.save(this.model).then(value => {
      this._messageService.open("Project salvo com sucesso!", "SUCESSO", "success");
      this.dialogRef.close();
    });
  }

  canSave(): boolean {
    if (this.formSave) {
      return !this.formSave.invalid;
    }
    return false;
  }

  getModules(): FormArray {
    return (this.formSave.get('modules')) as FormArray;
  }

  addModule(): FormGroup {
    const c = this.buildModule(false);
    (this.formSave.get('modules') as FormArray).push(c);
    return c;
  }

  addBond(parent: AbstractControl): FormGroup {
    const c = this.buildModule(false);
    (parent.get('childBonds') as FormArray).push(c);
    return c;
  }

  /**
   * Recursively adds bond form controls for N-level depth.
   */
  addBondsRecursively(parent: AbstractControl, childBonds: ProjectModuleModel[]): void {
    if (!UtilFunctions.isValidStringOrArray(childBonds)) return;
    for (const bond of childBonds) {
      const c = this.addBond(parent);
      this.addBondsRecursively(c, bond.childBonds);
    }
  }

  buildModule(main: boolean): FormGroup {
    const c: FormGroup = this._formBuilder.group({
      id: [null],
      identifier: ['', [Validators.required]],
      title: ['', [Validators.required]],
      description: ['', [Validators.required]],
      framework: [false],
      repository: ['', [Validators.required]],
      icon: this._formBuilder.group( {
        fontSet: ['', [Validators.required]],
        fontIcon: ['', [Validators.required]],
      }),
    });
    if (main) {
      c.addControl(
        'luthierProject',
        this._formBuilder.control(false)
      );

      c.addControl(
        'modules',
        this._formBuilder.array([])
      );
    }
    else {
      c.addControl(
        'main',
        this._formBuilder.control(false)
      );
      c.addControl(
        'relativePath',
        this._formBuilder.control('')
      );
      c.addControl(
        'childBonds',
        this._formBuilder.array([])
      );
    }
    return c
  }

  removeModule(index: number) {
    if (index >= 0) {
      (this.formSave.get('modules') as FormArray).removeAt(index);
    }
  }

  getControlTitle(c: AbstractControl): string {
    const title = c.get('title').value as string;
    const identifier = c.get('identifier').value as string;
    return UtilFunctions.isValidStringOrArray(title) ? title : identifier;
  }

  getControlDescription(c: AbstractControl): string {
    const desc = c.get('description').value as string;
    return UtilFunctions.isValidStringOrArray(desc) ? desc : '';
  }

  getControlIcon(c: AbstractControl): IconModel {
    if (UtilFunctions.isValidStringOrArray(c.get('icon').get('fontSet').value as string) &&
      UtilFunctions.isValidStringOrArray(c.get('icon').get('fontIcon').value as string)) {
      const icon = new IconModel();
      icon.fontSet = c.get('icon').get('fontSet').value as string;
      icon.fontIcon = c.get('icon').get('fontIcon').value as string;
      return icon;
    }
    return null;
  }

  getModuleTitle(i: number, c: AbstractControl): string {
    let text = 'Módulo ' + i;
    if (i < 0) {
      text = 'Project';
    }
    if (UtilFunctions.isValidStringOrArray(c.get('title').value as string)) {
      text = c.get('title').value as string;
    }
    return text;
  }

  getModuleDescription(i: number, c: AbstractControl): string {
    let text = '';
    if (i < 0) {
      text = 'Dados do Project';
    }
    if (UtilFunctions.isValidStringOrArray(c.get('description').value as string)) {
      text = c.get('description').value as string;
    }
    return text;
  }

  getModuleIcon(i: number, c: AbstractControl): IconModel {
    return this.getControlIcon(c);
  }

  getBonds(module: AbstractControl): FormArray {
    return module.get('childBonds') as FormArray;
  }

  getBondCount(module: AbstractControl): number {
    return this.getBonds(module)?.length || 0;
  }

  hasBonds(module: AbstractControl): boolean {
    return this.getBondCount(module) > 0;
  }

  getDepthClass(depth: number): string {
    return `hierarchy-level-${Math.min(depth, 3)}`;
  }

  getHierarchyLabel(depth: number): string {
    if (depth <= 0) {
      return 'Módulo raiz';
    }
    return `Bond nível ${depth}`;
  }

  removeBondAt(parent: AbstractControl, index: number): void {
    (parent.get('childBonds') as FormArray).removeAt(index);
  }

  openWorkflowRepositories() {
    const modal = this._matDialog.open(ProjectRepositoryModalComponent, { disableClose: true, panelClass: 'project-repository-modal-container' });
    modal.componentInstance.workflow = true;
    modal.componentInstance.service = this.service;
    modal.afterClosed().subscribe((result: ProjectModel) => {
      if (result) {
        const currentModel = this.formSave.value as ProjectModel;
        this.formSave.reset();
        result.id = currentModel.id;
        result.title = currentModel.title;
        result.description = currentModel.description;
        result.icon = currentModel.icon;
        if (UtilFunctions.isValidStringOrArray(currentModel.identifier)) {
          result.identifier = currentModel.identifier;
        }

        if (UtilFunctions.isValidStringOrArray(result.modules) && UtilFunctions.isValidStringOrArray(currentModel.modules)) {
          const existingMap = new Map<string, any>();
          this.collectModulesByIdentifier(currentModel.modules, existingMap);
          this.applyExistingModuleData(result.modules, existingMap);
        }
        this.formSave = this.buildModule(true);
        if (UtilFunctions.isValidStringOrArray(result.modules)
        ) {
          for(const x of result.modules) {
            if (x.bond == null) {
              const c = this.addModule();
              this.addBondsRecursively(c, x.childBonds);
            }
          }
        }
        // Filter to root modules only for patchValue (form structure is tree, not flat)
        const patchResult = {...result, modules: result.modules?.filter(x => x.bond == null)};
        this.formSave.patchValue(patchResult);
      }
    })
  }

  openRepositories(group: AbstractControl) {
    const modal = this._matDialog.open(ProjectRepositoryModalComponent, { disableClose: true, panelClass: 'project-repository-modal-container' });
    modal.componentInstance.workflow = false;
    modal.componentInstance.service = this.service;
    modal.afterClosed().subscribe((result: DetailedRepositoryGithubModel) => {
      if (result) {
        (group.get('repository') as FormControl).setValue(result.name);
        group.get('relativePath').setValue('');
        this.propagateRepositoryToBonds(group, result.name);
      }
    })
  }

  private propagateRepositoryToBonds(parent: AbstractControl, repository: string): void {
    const bonds = parent.get('childBonds') as FormArray;
    if (!bonds) return;
    for (const bond of bonds.controls) {
      (bond.get('repository') as FormControl).setValue(repository);
      bond.get('relativePath').setValue('');
      this.propagateRepositoryToBonds(bond, repository);
    }
  }

  private collectModulesByIdentifier(modules: any[], map: Map<string, any>): void {
    if (!modules) return;
    for (const mod of modules) {
      if (mod.identifier) {
        map.set(mod.identifier.toLowerCase(), mod);
      }
      this.collectModulesByIdentifier(mod.childBonds, map);
    }
  }

  private applyExistingModuleData(modules: any[], existingMap: Map<string, any>): void {
    if (!modules) return;
    for (const mod of modules) {
      if (mod.identifier) {
        const existing = existingMap.get(mod.identifier.toLowerCase());
        if (existing) {
          mod.id = existing.id;
          mod.title = existing.title;
          mod.description = existing.description;
          mod.icon = existing.icon;
        }
      }
      this.applyExistingModuleData(mod.childBonds, existingMap);
    }
  }

  openFolders(group: AbstractControl): void {
    const repositoryValue = group.get('repository').value;
    if (!UtilFunctions.isValidStringOrArray(repositoryValue)) {
      this._messageService.open('Selecione um repositório antes de escolher a pasta!', 'AVISO', 'warning');
      return;
    }
    const modal = this._matDialog.open(ProjectFolderModalComponent, {
      disableClose: true,
      panelClass: 'project-folder-modal-container'
    });
    modal.componentInstance.repository = repositoryValue;
    modal.componentInstance.initialPath = group.get('relativePath').value || '';
    modal.afterClosed().subscribe((result: string) => {
      if (result !== null && result !== undefined) {
        group.get('relativePath').setValue(result);
      }
    });
  }

}
