import {Component, ViewEncapsulation} from "@angular/core";
import {ProjectModel} from "app/shared/models/project.model";
import {GeracaoVersaoDiffModel} from '../../../../shared/models/geracao-versao.model';

@Component({
  selector       : 'geracao-versao-diff-modal',
  styleUrls      : ['/geracao-versao-diff-modal.component.scss'],
  templateUrl    : './geracao-versao-diff-modal.component.html',
  encapsulation  : ViewEncapsulation.None,

  standalone: false
})
export class GeracaoVersaoDiffModalComponent {
  public model: GeracaoVersaoDiffModel;

  public title: string;

  private _target: ProjectModel = null;
  set target(value: ProjectModel) {
    this._target = value;
    this.title = 'Alterações ' + this._target.title;
  }
  get target(): ProjectModel {
    return  this._target;
  }
  constructor()
  {}

  clearImage(model) {
    model.user.image = 'assets/images/noPicture.png';
  }

  trackByFn(index: number, item: any): any
  {
    return item.id || index;
  }

  gotoLink(link: string) {
    window.open(link, '_blank').focus();

  }

}
