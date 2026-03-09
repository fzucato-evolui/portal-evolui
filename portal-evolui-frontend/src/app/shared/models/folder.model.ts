import {FileModel} from "./file.model";
import {UsuarioModel} from "./usuario.model";

export class FolderModel {
  public id: number;
  public user: UsuarioModel;
  public identifier: string;
  public code: string;
  public files: Array<FileModel>;
}

export class FolderFilterModel {
  public id: number;
  public userId: number;
  public userName: string;
  public userEmail: string;
  public identifier: string;
  public code: string;

}
