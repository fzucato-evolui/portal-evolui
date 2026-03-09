import {Component, Input} from "@angular/core";
import {MatFormFieldControl} from "@angular/material/form-field";
import {UtilFunctions} from "../../util/util-functions";
import {MatSelect} from "@angular/material/select";

@Component({
  selector: 'select-map',
  template:'',
  providers: [{provide: MatFormFieldControl, useExisting: SelectMapComponent}],

  standalone: false
})
export class SelectMapComponent extends MatSelect {

    _mapOptions: {[key: string]: any}

    @Input()
    set mapOptions(map: {[key: string]: any}) {
      this._mapOptions = map;
    }


    @Input()
    get value(): Map<any, any> {


        return null;
    }
    set value(val: Map<any, any> | null) {
        if (UtilFunctions.isValidObject(val) === true) {
          const values = [];
            val.forEach(x => {

            })
        }
        this.stateChanges.next();
    }



    writeValue(val: Map<any, any> | null): void {
        this.value = val;
    }

}
