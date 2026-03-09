import {CertificateModel} from "./certificate.model";
import {LoginModel, UsuarioModel} from './usuario.model';
import {UtilFunctions} from '../util/util-functions';

export class HealthCheckerMessageTopicConstants {

  public static HEY = "hey";

  public static START_REQUEST = "start-request";

  public static START_RESPONSE = "start-response";

  public static SAVE_CONFIG_REQUEST = "save-config-request";

  public static SAVE_CONFIG_RESPONSE = "save-config-response";

  public static TEST_CONFIG_REQUEST = "test-config-request";

  public static TEST_CONFIG_RESPONSE = "test-config-response";

  public static EXECUTE_COMMAND_REQUEST = "execute-command-request";

  public static EXECUTE_COMMAND_RESPONSE = "execute-command-response";

  public static CLIENT_DISCONECTION = "client-disconnection";

  public static SYSTEM_INFO_RESPONSE = "system-info-response";

  public static SYSTEM_INFO_REQUEST_START = "system-info-request-start";

  public static SYSTEM_INFO_REQUEST_STOP = "system-info-request-stop";
}

export enum HealthCheckerAlertTypeEnum {
  MEMORY ="MEMORY",
  DISK_USAGE = "DISK_USAGE",
  OPENED_FILES = "OPENED_FILES",
  CPU = "CPU"
}

export enum HealthCheckerModuleTypeEnum {
  WEB ="WEB",
  EXECUTABLE = "EXECUTABLE"
}

export class HealthCheckerModuleConfigModel {
  public id: number;
  public moduleType: HealthCheckerModuleTypeEnum;
  public identifier: string;
  public description: string;
  public commandAddress: string;
  public acceptableResponsePattern: string;
  public clientCertificate: CertificateModel;
  public bypassCertificate = true;
  public sendNotification = true;
}
export class HealthCheckerSimpleSystemInfoModel {
  public memory: string;
  public machine: string;
  public software: string;
  public disks: string;
  public processor: string;

  public static parseFromSystemInfo(healthCheckerSystemInfo: HealthCheckerSystemInfoModel): HealthCheckerSimpleSystemInfoModel {
    const si: HealthCheckerSimpleSystemInfoModel = new HealthCheckerSimpleSystemInfoModel();
    si.software = `${healthCheckerSystemInfo.operatingSystem.manufacturer} ${healthCheckerSystemInfo.operatingSystem.family} ${healthCheckerSystemInfo.operatingSystem.versionInfo.version} ${healthCheckerSystemInfo.operatingSystem.bitness} Bits`;
    si.memory = UtilFunctions.bytes2Size(healthCheckerSystemInfo.hardware.memory.total);
    si.machine = `${healthCheckerSystemInfo.hardware.computerSystem.model} ${healthCheckerSystemInfo.hardware.computerSystem.manufacturer}`;
    si.processor = `${healthCheckerSystemInfo.hardware.processor.processorIdentifier.name} Família ${healthCheckerSystemInfo.hardware.processor.processorIdentifier.family} Modelo ${healthCheckerSystemInfo.hardware.processor.processorIdentifier.model}`;
    let disks = [];
    let uuids: Array<string> = [];
    for(const d of healthCheckerSystemInfo.operatingSystem.fileSystem.fileStores) {
      if (uuids.includes(d.uuid) === true) {
        continue;
      }
      if (UtilFunctions.isValidStringOrArray(d.description) === true) {
        const index = healthCheckerSystemInfo.hardware.diskStores.findIndex(x => {
          if (UtilFunctions.isValidStringOrArray(x.partitions) === true) {
            return x.partitions.findIndex(y => y.uuid === d.uuid) >= 0;
          }
          return false;
        });
        if (index >= 0) {
          const fileStore = healthCheckerSystemInfo.hardware.diskStores[index];
          const disk = `${disks.length + 1} - ${d.mount} ${(fileStore.model !== 'unknown' ? fileStore.model : fileStore.name)  + (UtilFunctions.isValidStringOrArray(d.label)===true ? ('(' + d.label + ')') : '')} ${UtilFunctions.bytes2Size(d.totalSpace)}`;
          disks.push(disk);
          uuids.push(d.uuid);
        }
      }
    }
    si.disks = disks.join('\r\n');
    return si;
  }

  public static getDiskDataLabel(healthCheckerSystemInfo: HealthCheckerSystemInfoModel): Array<{label: string, totalSize: number, availableSize: number}> {
    let disks = [];
    let uuids: Array<string> = [];
    for(const d of healthCheckerSystemInfo.operatingSystem.fileSystem.fileStores) {
      let item = {label: '', totalSize: 0, availableSize: 0};
      if (uuids.includes(d.uuid) === true) {
        continue;
      }
      if (UtilFunctions.isValidStringOrArray(d.description) === true) {
        const index = healthCheckerSystemInfo.hardware.diskStores.findIndex(x => {
          if (UtilFunctions.isValidStringOrArray(x.partitions) === true) {
            return x.partitions.findIndex(y => y.uuid === d.uuid) >= 0;
          }
          return false;
        });
        if (index >= 0) {
          const fileStore = healthCheckerSystemInfo.hardware.diskStores[index];
          if (UtilFunctions.removeNonAlphaNumeric(d.mount).length > 0) {
            item.label = d.mount;
          } else {
            item.label = fileStore.name;
          }
          item.totalSize = d.totalSpace;
          item.availableSize = d.freeSpace;
          disks.push(item);
          uuids.push(d.uuid);
        }
      }
    }
    return disks;
  }
}
export class HealthCheckerConfigModel {
  public id: number;
  public host: string;
  public identifier: string;
  public description: string;
  public healthCheckInterval: number;
  public modules: Array<HealthCheckerModuleConfigModel>;
  public login: LoginModel;
  public alerts: Array<{ [key: string]: {maxPercentual: number, sendNotification: boolean}}>;
  public systemInfo: HealthCheckerSimpleSystemInfoModel;
}

export class HealthCheckerAlertModel {
  public alertType: HealthCheckerAlertTypeEnum;
  public health: boolean;
  public error: string;
}

export class HealthCheckerModuleModel extends HealthCheckerAlertModel {
  public id: number;
  public identifier: string;
  public description: string;
  public lastHealthDate: Date;
  public  lastUpdate: Date;
}

export class HealthCheckerModel {
  public id: number;
  public identifier: string;
  public description: string;
  public user: UsuarioModel;
  public lastHealthDate: Date;
  public  lastUpdate: Date;
  public health: boolean;
  public systemInfo: HealthCheckerSimpleSystemInfoModel;
  public online: boolean;
  public modules: Array<HealthCheckerModuleModel>;
  public alerts: Array<HealthCheckerAlertModel>;
  public config: HealthCheckerConfigModel;
  //Transient
  public allHealth: boolean;
}
export class HealthCheckerSystemInfoModel {
  operatingSystem: OperatingSystem;
  hardware: Hardware;
  lastUpdate: Date | any;
}

export class OperatingSystem {
  family: string
  manufacturer: string
  bitness: number
  systemBootTime: number
  elevated: boolean
  systemUptime: number
  processCount: number
  threadCount: number
  processId: number
  threadId: number
  versionInfo: VersionInfo
  fileSystem: FileSystem
  networkParams: NetworkParams
  services: Service[]
  internetProtocolStats: InternetProtocolStats
  sessions: Session[]
  processes: Process[]
  currentProcess: Process;
}

export class VersionInfo {
  version: string
  codeName: string
  buildNumber: string
  versionStr: any
}

export class FileSystem {
  openFileDescriptors: number
  maxFileDescriptors: number
  maxFileDescriptorsPerProcess: number
  fileStores: FileStore[]
}

export class FileStore {
  name: string
  volume: string
  label: string
  mount: string
  options: string
  uuid: string
  logicalVolume: string
  description: string
  freeSpace: number
  usableSpace: number
  totalSpace: number
  freeInodes: number
  totalInodes: number
  type: string
}

export class NetworkParams {
  hostName: string
  ipv4DefaultGateway: string
  ipv6DefaultGateway: string
  dnsServers: string[]
  domainName: string
}

export class Service {
  name: string
  processID: number
  state: string
}

export class InternetProtocolStats {
  udpv4Stats: Udpv4Stats
  udpv6Stats: Udpv6Stats
  tcpv4Stats: Tcpv4Stats
  tcpv6Stats: Tcpv6Stats
  connections: Connection[]
}

export class Udpv4Stats {
  datagramsSent: number
  datagramsReceived: number
  datagramsNoPort: number
  datagramsReceivedErrors: number
}

export class Udpv6Stats {
  datagramsSent: number
  datagramsReceived: number
  datagramsNoPort: number
  datagramsReceivedErrors: number
}

export class Tcpv4Stats {
  connectionsEstablished: number
  connectionsActive: number
  connectionsPassive: number
  connectionFailures: number
  connectionsReset: number
  segmentsSent: number
  segmentsReceived: number
  segmentsRetransmitted: number
  inErrors: number
  outResets: number
}

export class Tcpv6Stats {
  connectionsEstablished: number
  connectionsActive: number
  connectionsPassive: number
  connectionFailures: number
  connectionsReset: number
  segmentsSent: number
  segmentsReceived: number
  segmentsRetransmitted: number
  inErrors: number
  outResets: number
}

export class Connection {
  type: string
  localAddress: string
  localPort: number
  foreignAddress: string
  foreignPort: number
  state: string
  transmitQueue: number
  receiveQueue: number
  owningProcessId: number
}

export class Session {
  userName: string
  terminalDevice: string
  loginTime: number
  host: string
}

export class Process {
  processID: number
  currentWorkingDirectory: string
  commandLine: string
  name: string
  path: string
  state: string
  parentProcessID: number
  threadCount: number
  priority: number
  virtualSize: number
  residentSetSize: number
  kernelTime: number
  userTime: number
  startTime: number
  upTime: number
  bytesRead: number
  bytesWritten: number
  openFiles: number
  bitness: number
  affinityMask: number
  group: string
  userID: string
  user: string
  groupID: string
  processCpuLoadCumulative: number
  cpuUsagePercent: number;
  memoryUsagePercent: number;
}


export class Hardware {
  computerSystem: ComputerSystem
  processor: Processor
  memory: Memory
  powerSources: PowerSource[]
  diskStores: DiskStore[]
  logicalVolumeGroups: any[]
  networkIFs: NetworkIf[]
}

export class ComputerSystem {
  manufacturer: string
  hardwareUUID: string
  serialNumber: string
  model: string
  firmware: Firmware
  baseboard: Baseboard
}

export class Firmware {
  name: string
  description: string
  version: string
  manufacturer: string
  releaseDate: string
}

export class Baseboard {
  version: string
  manufacturer: string
  serialNumber: string
  model: string
}

export class Processor {
  processorIdentifier: ProcessorIdentifier
  maxFreq: number
  currentFreq: number[]
  logicalProcessors: LogicalProcessor[]
  physicalProcessors: PhysicalProcessor[]
  processorCaches: ProcessorCache[]
  systemCpuLoadTicks: number[]
  processorCpuLoadTicks: number[][]
  logicalProcessorCount: number
  physicalProcessorCount: number
  physicalPackageCount: number
  contextSwitches: number
  interrupts: number
  tickSystemBefore: TickSystemBefore
  tickSystemAfter: TickSystemAfter
  tickSystemDiff: TickSystemDiff
  cpuLoad: number
  cpuLoadPerProcessor: number[]
  totalCpuDiffTicks: number
}

export class ProcessorIdentifier {
  vendor: string
  name: string
  family: string
  model: string
  stepping: string
  processorID: string
  identifier: string
  cpu64bit: boolean
  vendorFreq: number
  microarchitecture: string
}

export class LogicalProcessor {
  processorNumber: number
  physicalProcessorNumber: number
  physicalPackageNumber: number
  numaNode: number
  processorGroup: number
}

export class PhysicalProcessor {
  physicalPackageNumber: number
  physicalProcessorNumber: number
  efficiency: number
  idString: string
}

export class ProcessorCache {
  level: number
  associativity: number
  lineSize: number
  cacheSize: number
  type: string
}

export class TickSystemBefore {
  IDLE: number
  NICE: number
  SYSTEM: number
  SOFTIRQ: number
  USER: number
  STEAL: number
  IOWAIT: number
  IRQ: number
}

export class TickSystemAfter {
  IDLE: number
  NICE: number
  SYSTEM: number
  SOFTIRQ: number
  USER: number
  STEAL: number
  IOWAIT: number
  IRQ: number
}

export class TickSystemDiff {
  IDLE: number
  NICE: number
  SYSTEM: number
  SOFTIRQ: number
  USER: number
  STEAL: number
  IOWAIT: number
  IRQ: number
}

export class Memory {
  total: number
  available: number
  pageSize: number
  virtualMemory: VirtualMemory
  physicalMemory: PhysicalMemory[]
}

export class VirtualMemory {
  swapTotal: number
  swapUsed: number
  virtualMax: number
  virtualInUse: number
  swapPagesIn: number
  swapPagesOut: number
}

export class PhysicalMemory {
  bankLabel: string
  capacity: number
  clockSpeed: number
  manufacturer: string
  memoryType: string
}

export class PowerSource {
  name: string
  deviceName: string
  remainingCapacityPercent: number
  timeRemainingEstimated: number
  timeRemainingInstant: number
  powerUsageRate: number
  voltage: number
  amperage: number
  powerOnLine: boolean
  charging: boolean
  discharging: boolean
  capacityUnits: string
  currentCapacity: number
  maxCapacity: number
  designCapacity: number
  cycleCount: number
  chemistry: string
  manufactureDate: any
  manufacturer: string
  serialNumber: string
  temperature: number
}

export class DiskStore {
  name: string
  model: string
  serial: string
  size: number
  reads: number
  readBytes: number
  writes: number
  writeBytes: number
  currentQueueLength: number
  transferTime: number
  partitions: Partition[]
  timeStamp: number
}

export class Partition {
  identification: string
  name: string
  type: string
  uuid: string
  size: number
  major: number
  minor: number
  mountPoint: string
}

export class NetworkIf {
  name: string
  index: number
  displayName: string
  ifAlias: string
  ifOperStatus: string
  mtu: number
  macaddr: string
  ipv4addr: string[]
  subnetMasks: number[]
  ipv6addr: string[]
  prefixLengths: number[]
  ifType: number
  ndisPhysicalMediumType: number
  connectorPresent: boolean
  bytesRecv: number
  bytesSent: number
  packetsRecv: number
  packetsSent: number
  inErrors: number
  outErrors: number
  inDrops: number
  collisions: number
  speed: number
  timeStamp: number
  knownVmMacAddr: boolean
}
