import {Injectable} from "@angular/core";
import {BehaviorSubject, Observable} from "rxjs";
import {map, tap} from "rxjs/operators";
import {HttpClient} from "@angular/common/http";
import {RootModel} from "../../models/root.model";
import {UsuarioModel} from "../../models/usuario.model";
import {GoogleConfigModel, SystemConfigModelEnum} from "../../models/system-config.model";

@Injectable()
export class RootService {
    private _model: BehaviorSubject <RootModel> = new BehaviorSubject <RootModel>(new RootModel());
    constructor(private _httpClient: HttpClient) {

    }

    get model$(): Observable<RootModel>
    {
        return this._model.asObservable();
    }

    get googleConfig$(): Observable<GoogleConfigModel>
    {
      return this._model.asObservable().pipe(
        map(value => {
           const index = value.configs.findIndex(x => x.configType.toString() === SystemConfigModelEnum.GOOGLE.toString());
           if (index >= 0) {
             const c: GoogleConfigModel = value.configs[index].config as GoogleConfigModel;
             return c;
           }

          return null;
        })
      );
    }


    set user(model: UsuarioModel) {
        const current = this._model.getValue();
        current.user = model;
        this._model.next(current);
    }

    // -----------------------------------------------------------------------------------------------------
    // @ Public methods
    // -----------------------------------------------------------------------------------------------------

    /**
     * Get all messages
     */
    get(): Observable<RootModel>
    {
        return this._httpClient.get<RootModel>('/api/public/root').pipe(
            tap((resp) => {
                this._model.next(resp);

            },err => {
                console.error("pipe error", err);
                throw err;
            })
        );

        //return of(new RootModel())
    }


}
