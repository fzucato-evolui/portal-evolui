export class WorkspaceModel {
  public id: string;
  public userName: string;
  public computerName: string;
  public state: string;
  public account: string;
  public runningMode: 'AUTO_STOP' | 'ALWAYS_ON' | 'MANUAL' | undefined;
  public privateIpAddress: string;
  public privateDns: string;
  public publicIpAddress: string;
  public publicDns: string;
  public rootVolumeSizeGib: number;
  public userVolumeSizeGib: number;
  public os: string;
  public platform: string;
  public protocol: string;
}
