import {UsuarioModel} from "./usuario.model";
import {SystemConfigModel} from "./system-config.model";

export class RootModel {
    public user: UsuarioModel;
    public configs: Array<SystemConfigModel>;
}
