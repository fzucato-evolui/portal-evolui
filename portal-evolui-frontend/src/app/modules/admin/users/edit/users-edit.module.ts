import {NgModule} from "@angular/core";
import {Route, RouterModule} from "@angular/router";
import {MatButtonModule} from "@angular/material/button";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatSelectModule} from "@angular/material/select";
import {MatIconModule} from "@angular/material/icon";
import {MatInputModule} from "@angular/material/input";
import {MatRadioModule} from "@angular/material/radio";
import {NgxMaskDirective, NgxMaskPipe, provideNgxMask} from "ngx-mask";
import {MatTabsModule} from "@angular/material/tabs";
import {MatCheckboxModule} from "@angular/material/checkbox";
import {NgxMatSelectSearchModule} from "ngx-mat-select-search";
import {UsersEditComponent} from "./users-edit.component";
import {UsersEditResolver} from "./users-edit.resolver";
import {MatSlideToggleModule} from "@angular/material/slide-toggle";
import {PerfilUsuarioEnum} from "../../../../shared/models/usuario.model";
import {SharedModule} from "../../../../shared/shared.module";
import {AdminContainerModule} from "../../../../shared/components/admin-container/admin-container.module";
import {UsersEditModalComponent} from "./modal/users-edit-modal.component";
import {UsersEditFormComponent} from "./form/users-edit-form.component";
import {MatDialogModule} from "@angular/material/dialog";

const UsersEditRoutes: Route[] = [
    {
        path     : 'users/:id',
        component: UsersEditComponent,
        resolve    : {
            initialData: UsersEditResolver
        },
        data: {
            roles: [PerfilUsuarioEnum.ROLE_ADMIN, PerfilUsuarioEnum.ROLE_SUPER, PerfilUsuarioEnum.ROLE_HYPER]
        }
    }
];
@NgModule({
    declarations: [
        UsersEditComponent,
        UsersEditModalComponent,
        UsersEditFormComponent
    ],
    imports     : [
        RouterModule.forChild(UsersEditRoutes),
        MatButtonModule,
        MatFormFieldModule,
        MatIconModule,
        MatInputModule,
        MatRadioModule,
        MatSelectModule,
        MatTabsModule,
        MatDialogModule,
        MatCheckboxModule,
        MatSlideToggleModule,
        NgxMatSelectSearchModule,
        SharedModule,
        AdminContainerModule,
        NgxMaskDirective,
        NgxMaskPipe
    ],
    providers: [
        provideNgxMask()
    ]
})
export class UsersEditModule {

}
