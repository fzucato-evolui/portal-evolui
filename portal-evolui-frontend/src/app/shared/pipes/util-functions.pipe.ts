import {Pipe, PipeTransform} from '@angular/core';
import {UtilFunctions} from "../util/util-functions";
import {FolderModel} from "../models/folder.model";

@Pipe({name: 'isValidStringOrArray', standalone: false})
export class IsValidStringOrArrayPipe implements PipeTransform{
    transform(value: string | any[]): boolean {
        return UtilFunctions.isValidStringOrArray(value);
    }

}

@Pipe({name: 'enumArray', standalone: false})
export class EnumToArrayPipe implements PipeTransform {
    transform(data: Object) {
        //console.log(data);
        let keys = Object.keys(data);
        //console.log(keys);
        //keys = keys.slice(0, keys.length / 2);

        const keyPair = [];
        for (let i = 0; i < keys.length; i++) {
            const enumMember = keys[i];
            keyPair.push({key: enumMember, value: data[enumMember]});
        }
        return keyPair;
    }
}

@Pipe({name: 'formatNumber', standalone: false})
export class FormatNumberPipe implements PipeTransform {
    transform(data: any) {

        return UtilFunctions.formatNumber(data);
    }
}

@Pipe({name: 'formatTotalFolderFiles', standalone: false})
export class FormatTotalFolderFiles implements PipeTransform {
  transform(data: FolderModel) {

    return data.files.length.toString();
  }
}

@Pipe({name: 'formatTotalFolderSize', standalone: false})
export class FormatTotalFolderSize implements PipeTransform {
  transform(data: FolderModel) {

    let totalSize = 0;
    if (data) {
      totalSize = 0;
      if (data.files && data.files.length > 0) {
        data.files.forEach(x => {
          totalSize += x.size;
        });
      }
    }

    if (totalSize < 1000) {
      return totalSize + " B"
    } else if (totalSize < 1000000) {
      return UtilFunctions.formatNumber(totalSize/1000) + " KB"
    } else if (totalSize < 1000000000) {
      return UtilFunctions.formatNumber(totalSize/1000000) + " MB"
    } else {
      return UtilFunctions.formatNumber(totalSize/10000000000) + " GB"
    }
  }
}

@Pipe({name: 'formatFileSize', standalone: false})
export class FormatFileSize implements PipeTransform {
  transform(totalSize: number) {
    if (totalSize < 1000) {
      return totalSize + " B"
    } else if (totalSize < 1000000) {
      return UtilFunctions.formatNumber(totalSize/1000) + " KB"
    } else if (totalSize < 1000000000) {
      return UtilFunctions.formatNumber(totalSize/1000000) + " MB"
    } else {
      return UtilFunctions.formatNumber(totalSize/10000000000) + " GB"
    }
  }
}


@Pipe({
    name: 'filterJson',

    standalone: false
})
export class FilterJsonPipe implements PipeTransform {

    transform(items: any[], filters:{[key: string]: string}): any[] {
        if(!items) return [];
        if(!filters) return items;
        if (!FilterJsonPipe.validate(filters)) return items;

        return items.filter((value) => {
            for(let field in filters) {
                if(FilterJsonPipe.isEquals(filters[field], FilterJsonPipe.pathDataAccessor(value, field))) {
                    return value;
                }
            }
        });
    }

    private static validate(filters:{[key: string]: string}) {
        let isValue = false;
        for (let field in filters) {
            if (filters[field] !== undefined && filters[field] !== '') {
                isValue = true;
            }
        }
        return isValue;
    }

    private static isEquals(filter: any, value) {
        value = UtilFunctions.removeAccents(value.toString()).toLowerCase();
        filter = UtilFunctions.removeAccents(filter.toString()).toLowerCase();
        return value.includes(filter);
    }

    private static pathDataAccessor(item: any, path: string): any {
        return path.split('.')
            .reduce((accumulator: any, key: string) => {
                return accumulator ? accumulator[key] : undefined;
            }, item);
    }
}

