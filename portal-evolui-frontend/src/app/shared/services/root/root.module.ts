import {APP_INITIALIZER, ModuleWithProviders, NgModule} from '@angular/core';
import {RootService} from "./root.service";

function configFactory(service: RootService) {
    // do the async tasks at here
    return () => service.get();
}
@NgModule({

})
export class RootModule {
    static forRoot(): ModuleWithProviders<RootModule> {
        return {
            ngModule: RootModule,
            providers: [
                {
                    provide: RootService
                },
                {
                    provide: APP_INITIALIZER,
                    useFactory: configFactory,
                    deps: [RootService],
                    multi:  true
                },


            ]
        }
    }
}
