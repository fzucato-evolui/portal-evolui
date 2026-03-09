import {UsuarioModel} from './usuario.model';
import {VersionConclusionEnum, VersionStatusEnum} from './version.model';
import {BucketModel} from './bucket.model';
import {RDSModel} from './rds.model';

export enum ActionRDSTypeEnum {
  BACKUP = "BACKUP",
  RESTORE = "RESTORE"
}

export class ActionRdsModel {
  id: number;
  user: UsuarioModel;
  schedulerDate?: Date | null;
  requestDate: Date;
  conclusionDate?: Date | null;
  public status: VersionStatusEnum;
  public conclusion: VersionConclusionEnum;
  restoreKey?: string | null;
  error?: string | null;
  dumpFile: BucketModel;
  destinationDatabase: string;
  destinationPassword: string;
  sourceDatabase: string;
  rds: RDSModel;
  actionType: ActionRDSTypeEnum;
  excludeBlobs: boolean;
  remaps: {[key: string]: ActionRdsRemapModel[]};
}

export enum ActionRDSRemapTypeEnum {
  SCHEMA = 'SCHEMA',
  TABLESPACE = 'TABLESPACE',
  DUMP_DIR = 'DUMP_DIR'
}

export class ActionRdsRemapModel {
  source: string;
  destination: string;
}

export class ActionRdsFilterModel {
  public id?: number;
  public actionType?: ActionRDSTypeEnum;
  public requestDateFrom?: Date;
  public requestDateTo?: Date;
  public userName?: string;
  public userEmail?: string;
  public rds?: string;
  public status: VersionStatusEnum;
  public conclusion: VersionConclusionEnum;
}

export enum BackupRestoreRDSStatusLevelEnum {
  ERROR = "ERROR",
  WARN = "WARN",
  INFO = "INFO",
  DEBUG ="DEBUG",
  TRACE = "TRACE"
}
export class BackupRestoreRDSStatusModel {
  id: any;
  timeStamp: Date;
  message: string;
  logLevel: BackupRestoreRDSStatusLevelEnum;
  finished: boolean;
  heartbeat: boolean;
  formattedMessage: string;
}
