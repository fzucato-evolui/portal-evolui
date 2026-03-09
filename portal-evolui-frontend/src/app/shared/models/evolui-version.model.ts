import * as semver from "semver";
import {SemVer} from "semver";
import {UtilFunctions} from '../util/util-functions';

export class EvoluiVersionModel extends SemVer {
  public buildNumber: any = 0;
  public buildNumberType: BuildNumberType = BuildNumberType.PURE_NUMBER;
  public beta: boolean = false;
  public hash: string;
  public abnormalBranch: boolean = false;
  public branchName: string = '';

  get completeVersion(): string {
    if (this.abnormalBranch) {
      return this.branchName;
    }
    return this.version + "." + this.buildNumber + (this.beta ? '-BETA' : '');
  }

  constructor(version: string, beta:boolean = false, buildNumberType?: BuildNumberType) {
    const isAbnormalBranch = !version.match(/^\d+(\.\d+)*(\.\w+)?$/) && !version.includes('-');
    const superVersion = isAbnormalBranch ? '0.0.0'
      : version === 'master' ? '99.99.99'
      : semver.valid(semver.coerce(version));

    super(superVersion);

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
      if (version.toUpperCase().endsWith('-BETA')) {
        this.beta = true;
      }
      numbers[3] = numbers[3].replace('-BETA', '');
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
      if (this.beta != other.beta) {
        if (this.beta) {
          return -1;
        }
        return 1;
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
