import * as semver from "semver";
import {SemVer} from "semver";
import {UtilFunctions} from '../util/util-functions';
import {VersionTypeEnum} from './version.model';

const VERSION_TYPE_PRIORITY: { [key: string]: number } = {
  [VersionTypeEnum.alpha]: 0,
  [VersionTypeEnum.beta]: 1,
  [VersionTypeEnum.rc]: 2,
  [VersionTypeEnum.stable]: 3,
  [VersionTypeEnum.patch]: 4,
};

export class EvoluiVersionModel extends SemVer {
  public buildNumber: any = 0;
  public buildNumberType: BuildNumberType = BuildNumberType.PURE_NUMBER;
  public beta: boolean = false;
  public hash: string;
  public abnormalBranch: boolean = false;
  public branchName: string = '';
  public versionType?: VersionTypeEnum;
  public qualifier?: string;

  get completeVersion(): string {
    if (this.abnormalBranch) {
      return this.branchName;
    }
    return this.version + "." + this.buildNumber + (this.beta ? '-BETA' : '');
  }

  constructor(version: string, beta:boolean = false, buildNumberType?: BuildNumberType, versionType?: VersionTypeEnum) {
    const isAbnormalBranch = !version.match(/^\d+(\.\d+)*(\.\w+)?$/) && !version.includes('-');
    const superVersion = isAbnormalBranch ? '0.0.0'
      : version === 'master' ? '99.99.99'
      : semver.valid(semver.coerce(version));

    super(superVersion);

    this.versionType = versionType;

    if (isAbnormalBranch) {
      this.abnormalBranch = true;
      this.branchName = version;
      this.buildNumber = 0;
      this.beta = false;
      return;
    }

    if (version === 'master') {
      this.buildNumber = 999999;
      this.branchName = 'master';
      this.beta = false;
      return;
    }

    this.beta = beta;

    const numbers = version.split('.');
    if (buildNumberType) {
      this.buildNumberType = buildNumberType;
    }
    if (numbers.length === 4) {
      const dashIndex = numbers[3].indexOf('-');
      if (dashIndex >= 0) {
        this.qualifier = numbers[3].substring(dashIndex + 1).toUpperCase(); // ex.: RC, BETA, ALPHA
        numbers[3] = numbers[3].substring(0, dashIndex);
        if (this.qualifier === 'BETA') {
          this.beta = true;
        }
      }
      if (this.buildNumberType === BuildNumberType.PURE_NUMBER) {
        this.buildNumber = parseInt(numbers[3]);
      } else if (this.buildNumberType === BuildNumberType.DATE_STRING) {
        this.buildNumber = EvoluiVersionModel.parseDateVersion(numbers[3]);
      } else if (this.buildNumberType === BuildNumberType.SUFIX_NUMBER) {
        this.buildNumber = numbers[3];
      } else if (this.buildNumberType === BuildNumberType.EPOCH_MINUTES) {
        this.buildNumber = numbers[3];
      }
    } else {
      if (this.buildNumberType === BuildNumberType.PURE_NUMBER) {
        this.buildNumber = 0;
      } else if (this.buildNumberType === BuildNumberType.DATE_STRING) {
        this.buildNumber = UtilFunctions.dateToString(new Date(), 'YYYYMMDDHHmmss');
      } else if (this.buildNumberType === BuildNumberType.SUFIX_NUMBER) {
        this.buildNumber = '0';
      } else if (this.buildNumberType === BuildNumberType.EPOCH_MINUTES) {
        this.buildNumber = UtilFunctions.dateToMinutes(new Date());
      }
    }

    // Consistência: beta informado por parâmetro (sem sufixo no tag) equivale ao qualifier BETA
    if (this.beta && !this.qualifier) {
      this.qualifier = 'BETA';
    }
  }

  public incrementBuildNumber() {
    // Não faz sentido incrementar build para branches anormais
    if (this.abnormalBranch) {
      return;
    }

    if (this.buildNumberType === BuildNumberType.PURE_NUMBER) {
      this.buildNumber = parseInt(this.buildNumber.toString()) + 1;
    } else if (this.buildNumberType === BuildNumberType.DATE_STRING) {
      this.buildNumber = UtilFunctions.dateToString(new Date(), 'YYYYMMDDHHmmss');
    } else if (this.buildNumberType === BuildNumberType.SUFIX_NUMBER) {
      let n = '';
      for(let i = this.buildNumber.toString().length - 1; i >= 0; i--) {
        if (isNaN(this.buildNumber.toString().charAt(i)) == false) {
          n = this.buildNumber.toString().charAt(i) + n;
        } else {
          n = this.buildNumber.toString().substring(0, i + 1) + (parseInt(n) + 1).toString();
          break;
        }
      }
      this.buildNumber = n;
    } else if (this.buildNumberType === BuildNumberType.EPOCH_MINUTES) {
      this.buildNumber = UtilFunctions.dateToMinutes(new Date());
    }
  }

  public customCompare(other: EvoluiVersionModel): 1 | 0 | -1 {
    // Se uma das versões é abnormal e a outra não, a versão normal vem primeiro
    if (this.abnormalBranch && !other.abnormalBranch) {
      return -1;
    } else if (!this.abnormalBranch && other.abnormalBranch) {
      return 1;
    }

    // Se ambas são abnormais, compara strings diretamente
    if (this.abnormalBranch && other.abnormalBranch) {
      return this.branchName < other.branchName ? -1 : this.branchName > other.branchName ? 1 : 0;
    }

    // Comparação normal para versões semver
    const c = super.compare(other);
    if (c === 0) {
      if (this.versionType && other.versionType && this.versionType !== other.versionType) {
        const thisPriority = VERSION_TYPE_PRIORITY[this.versionType];
        const otherPriority = VERSION_TYPE_PRIORITY[other.versionType];
        if (thisPriority !== undefined && otherPriority !== undefined && thisPriority !== otherPriority) {
          return thisPriority < otherPriority ? -1 : 1;
        }
      }
      // qualifier: com qualifier (alpha/beta/rc) é menor que sem qualifier (stable/patch)
      const thisHasQualifier = !!this.qualifier;
      const otherHasQualifier = !!other.qualifier;
      if (thisHasQualifier && !otherHasQualifier) {
        return -1;
      } else if (!thisHasQualifier && otherHasQualifier) {
        return 1;
      } else if (thisHasQualifier && otherHasQualifier) {
        // ambos com qualifier: alfabético (ALPHA < BETA < RC)
        if (this.qualifier < other.qualifier) {
          return -1;
        }
        if (this.qualifier > other.qualifier) {
          return 1;
        }
        // mesmo qualifier → cai no build
      }
      if (this.buildNumber === other.buildNumber) {
        return 0;
      }
      if (this.buildNumber < other.buildNumber) {
        return -1;
      }
      if (this.buildNumber > other.buildNumber) {
        return 1;
      }
    }
    return c;
  }

  public static parseDateVersion(dateString: string): string {
    const d = UtilFunctions.stringToDate(dateString, 'YYYYMMDDHHmmss');
    return UtilFunctions.dateToString(d, 'YYYYMMDDHHmmss');
  }

  public static isValid(version: string): boolean {
    // Branches anormais são consideradas válidas, mas não são versões semver válidas
    if (!version.match(/^\d+(\.\d+)*(\.\w+)?$/) && !version.includes('-')) {
      return true;
    }
    return semver.valid(version) !== null;
  }

  public static comparableString(ver: EvoluiVersionModel): string {
    if (ver.abnormalBranch) {
      return ver.branchName;
    }
    return `${String(ver.major).padStart(20, '0')}.${String(ver.minor).padStart(20, '0')}.${String(ver.patch).padStart(20, '0')}.${UtilFunctions.isValidStringOrArray(ver.buildNumber) === true ? String(ver.buildNumber).padStart(20, '0') : ''}`;
  }

  public static fromString(version: string): EvoluiVersionModel {
    return new EvoluiVersionModel(version);
  }

  public static isBranchAbnormal(version: string): boolean {
    return !version.match(/^\d+(\.\d+)*(\.\w+)?$/) && !version.includes('-');
  }
}

export enum BuildNumberType {
  PURE_NUMBER,
  DATE_STRING,
  SUFIX_NUMBER,
  EPOCH_MINUTES
}
