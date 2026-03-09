package br.com.evolui.portalevolui.shared.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import oshi.hardware.NetworkIF;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HealthCheckerSystemInfoDTO {
    private OperatingSystemDTO operatingSystem;
    private HardwareDTO hardware;
    private Calendar lastUpdate;

    public OperatingSystemDTO getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(OperatingSystemDTO operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public HardwareDTO getHardware() {
        return hardware;
    }

    public void setHardware(HardwareDTO hardware) {
        this.hardware = hardware;
    }

    public Calendar getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Calendar lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public static class OperatingSystemDTO {
        private String family;
        private String manufacturer;
        private int bitness;
        private long systemBootTime;
        private boolean elevated;
        private long systemUptime;
        private int processCount;
        private int threadCount;
        private int processId;
        private int threadId;
        private OSVersionInfoDTO versionInfo;
        private FileSystemDTO fileSystem;
        private NetworkParamsDTO networkParams;
        private List<OSServiceDTO> services;
        private InternetProtocolStatsDTO internetProtocolStats;
        private List<OSSessionDTO> sessions;
        private List<OSProcessDTO> processes;
        private OSProcessDTO currentProcess;

        public String getFamily() {
            return family;
        }

        public void setFamily(String family) {
            this.family = family;
        }


        public String getManufacturer() {
            return manufacturer;
        }

        public void setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
        }


        public int getBitness() {
            return bitness;
        }

        public void setBitness(int bitness) {
            this.bitness = bitness;
        }


        public long getSystemBootTime() {
            return systemBootTime;
        }

        public void setSystemBootTime(long systemBootTime) {
            this.systemBootTime = systemBootTime;
        }


        public boolean isElevated() {
            return elevated;
        }

        public void setElevated(boolean elevated) {
            this.elevated = elevated;
        }


        public long getSystemUptime() {
            return systemUptime;
        }

        public void setSystemUptime(long systemUptime) {
            this.systemUptime = systemUptime;
        }


        public int getProcessCount() {
            return processCount;
        }

        public void setProcessCount(int processCount) {
            this.processCount = processCount;
        }


        public int getThreadCount() {
            return threadCount;
        }

        public void setThreadCount(int threadCount) {
            this.threadCount = threadCount;
        }


        public int getProcessId() {
            return processId;
        }

        public void setProcessId(int processId) {
            this.processId = processId;
        }


        public int getThreadId() {
            return threadId;
        }

        public void setThreadId(int threadId) {
            this.threadId = threadId;
        }

        public OSVersionInfoDTO getVersionInfo() {
            return versionInfo;
        }

        public void setVersionInfo(OSVersionInfoDTO versionInfo) {
            this.versionInfo = versionInfo;
        }

        public FileSystemDTO getFileSystem() {
            return fileSystem;
        }

        public void setFileSystem(FileSystemDTO fileSystem) {
            this.fileSystem = fileSystem;
        }

        public NetworkParamsDTO getNetworkParams() {
            return networkParams;
        }

        public void setNetworkParams(NetworkParamsDTO networkParams) {
            this.networkParams = networkParams;
        }

        public List<OSServiceDTO> getServices() {
            return services;
        }

        public void setServices(List<OSServiceDTO> services) {
            this.services = services;
        }

        public InternetProtocolStatsDTO getInternetProtocolStats() {
            return internetProtocolStats;
        }

        public void setInternetProtocolStats(InternetProtocolStatsDTO internetProtocolStats) {
            this.internetProtocolStats = internetProtocolStats;
        }

        public List<OSSessionDTO> getSessions() {
            return sessions;
        }

        public void setSessions(List<OSSessionDTO> sessions) {
            this.sessions = sessions;
        }

        public List<OSProcessDTO> getProcesses() {
            return processes;
        }

        public void setProcesses(List<OSProcessDTO> processes) {
            this.processes = processes;
        }

        public OSProcessDTO getCurrentProcess() {
            return currentProcess;
        }

        public void setCurrentProcess(OSProcessDTO currentProcess) {
            this.currentProcess = currentProcess;
        }
    }

    public static class OSVersionInfoDTO  {
        private String version;
        private String codeName;
        private String buildNumber;
        private String versionStr;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getCodeName() {
            return codeName;
        }

        public void setCodeName(String codeName) {
            this.codeName = codeName;
        }

        public String getBuildNumber() {
            return buildNumber;
        }

        public void setBuildNumber(String buildNumber) {
            this.buildNumber = buildNumber;
        }

        public String getVersionStr() {
            return versionStr;
        }

        public void setVersionStr(String versionStr) {
            this.versionStr = versionStr;
        }
    }

    public static class FileSystemDTO {
        private long openFileDescriptors;
        private long maxFileDescriptors;
        private long maxFileDescriptorsPerProcess;
        private List<FileStoreDTO> fileStores;

        public long getOpenFileDescriptors() {
            return openFileDescriptors;
        }

        public void setOpenFileDescriptors(long openFileDescriptors) {
            this.openFileDescriptors = openFileDescriptors;
        }

        public long getMaxFileDescriptors() {
            return maxFileDescriptors;
        }

        public void setMaxFileDescriptors(long maxFileDescriptors) {
            this.maxFileDescriptors = maxFileDescriptors;
        }

        public long getMaxFileDescriptorsPerProcess() {
            return maxFileDescriptorsPerProcess;
        }

        public void setMaxFileDescriptorsPerProcess(long maxFileDescriptorsPerProcess) {
            this.maxFileDescriptorsPerProcess = maxFileDescriptorsPerProcess;
        }

        public List<FileStoreDTO> getFileStores() {
            return fileStores;
        }

        public void setFileStores(List<FileStoreDTO> fileStores) {
            this.fileStores = fileStores;
        }
    }

    public static class FileStoreDTO {
        private String name;
        private String volume;
        private String label;
        private String mount;
        private String options;
        private String uuid;
        private String logicalVolume;
        private String description;
        private long freeSpace;
        private long usableSpace;
        private long totalSpace;
        private long freeInodes;
        private long totalInodes;
        private String type;
        private boolean updateAttributes;


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }


        public String getVolume() {
            return volume;
        }

        public void setVolume(String volume) {
            this.volume = volume;
        }


        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }


        public String getMount() {
            return mount;
        }

        public void setMount(String mount) {
            this.mount = mount;
        }


        public String getOptions() {
            return options;
        }

        public void setOptions(String options) {
            this.options = options;
        }

        public String getLogicalVolume() {
            return logicalVolume;
        }

        public void setLogicalVolume(String logicalVolume) {
            this.logicalVolume = logicalVolume;
        }


        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }


        public long getFreeSpace() {
            return freeSpace;
        }

        public void setFreeSpace(long freeSpace) {
            this.freeSpace = freeSpace;
        }


        public long getUsableSpace() {
            return usableSpace;
        }

        public void setUsableSpace(long usableSpace) {
            this.usableSpace = usableSpace;
        }


        public long getTotalSpace() {
            return totalSpace;
        }

        public void setTotalSpace(long totalSpace) {
            this.totalSpace = totalSpace;
        }


        public long getFreeInodes() {
            return freeInodes;
        }

        public void setFreeInodes(long freeInodes) {
            this.freeInodes = freeInodes;
        }


        public long getTotalInodes() {
            return totalInodes;
        }

        public void setTotalInodes(long totalInodes) {
            this.totalInodes = totalInodes;
        }


        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean updateAttributes() {
            return updateAttributes;
        }

        public void setUpdateAttributes(boolean updateAttributes) {
            this.updateAttributes = updateAttributes;
        }


        public String getUUID() {
            return this.uuid;
        }

        public void setUUID(String UUID) {
            this.uuid = UUID;
        }
    }

    public static class NetworkParamsDTO {
        private String hostName;
        private String ipv4DefaultGateway;
        private String ipv6DefaultGateway;
        private List<String> dnsServers;
        private String domainName;

        public String getHostName() {
            return hostName;
        }

        public void setHostName(String hostName) {
            this.hostName = hostName;
        }

        public String getIpv4DefaultGateway() {
            return ipv4DefaultGateway;
        }

        public void setIpv4DefaultGateway(String ipv4DefaultGateway) {
            this.ipv4DefaultGateway = ipv4DefaultGateway;
        }

        public String getIpv6DefaultGateway() {
            return ipv6DefaultGateway;
        }

        public void setIpv6DefaultGateway(String ipv6DefaultGateway) {
            this.ipv6DefaultGateway = ipv6DefaultGateway;
        }

        public List<String> getDnsServers() {
            return dnsServers;
        }

        public void setDnsServers(List<String> dnsServers) {
            this.dnsServers = dnsServers;
        }

        public String getDomainName() {
            return domainName;
        }

        public void setDomainName(String domainName) {
            this.domainName = domainName;
        }
    }

    public static class OSServiceDTO {
        private String name;
        private int processID;
        private OSService.State state;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getProcessID() {
            return processID;
        }

        public void setProcessID(int processID) {
            this.processID = processID;
        }

        public OSService.State getState() {
            return state;
        }

        public void setState(OSService.State state) {
            this.state = state;
        }
    }

    public static class InternetProtocolStatsDTO {
        private UdpStatsDTO udpv4Stats;
        private UdpStatsDTO udpv6Stats;
        private TcpStatsDTO tcpv4Stats;
        private TcpStatsDTO tcpv6Stats;
        private List<IPConnectionDTO> connections;

        public UdpStatsDTO getUdpv4Stats() {
            return udpv4Stats;
        }

        public void setUdpv4Stats(UdpStatsDTO udpv4Stats) {
            this.udpv4Stats = udpv4Stats;
        }

        public UdpStatsDTO getUdpv6Stats() {
            return udpv6Stats;
        }

        public void setUdpv6Stats(UdpStatsDTO udpv6Stats) {
            this.udpv6Stats = udpv6Stats;
        }

        public TcpStatsDTO getTcpv4Stats() {
            return tcpv4Stats;
        }

        public void setTcpv4Stats(TcpStatsDTO tcpv4Stats) {
            this.tcpv4Stats = tcpv4Stats;
        }

        public TcpStatsDTO getTcpv6Stats() {
            return tcpv6Stats;
        }

        public void setTcpv6Stats(TcpStatsDTO tcpv6Stats) {
            this.tcpv6Stats = tcpv6Stats;
        }

        public List<IPConnectionDTO> getConnections() {
            return connections;
        }

        public void setConnections(List<IPConnectionDTO> connections) {
            this.connections = connections;
        }
    }

    public static class TcpStatsDTO {
        private long connectionsEstablished;
        private long connectionsActive;
        private long connectionsPassive;
        private long connectionFailures;
        private long connectionsReset;
        private long segmentsSent;
        private long segmentsReceived;
        private long segmentsRetransmitted;
        private long inErrors;
        private long outResets;

        public long getConnectionsEstablished() {
            return connectionsEstablished;
        }

        public void setConnectionsEstablished(long connectionsEstablished) {
            this.connectionsEstablished = connectionsEstablished;
        }

        public long getConnectionsActive() {
            return connectionsActive;
        }

        public void setConnectionsActive(long connectionsActive) {
            this.connectionsActive = connectionsActive;
        }

        public long getConnectionsPassive() {
            return connectionsPassive;
        }

        public void setConnectionsPassive(long connectionsPassive) {
            this.connectionsPassive = connectionsPassive;
        }

        public long getConnectionFailures() {
            return connectionFailures;
        }

        public void setConnectionFailures(long connectionFailures) {
            this.connectionFailures = connectionFailures;
        }

        public long getConnectionsReset() {
            return connectionsReset;
        }

        public void setConnectionsReset(long connectionsReset) {
            this.connectionsReset = connectionsReset;
        }

        public long getSegmentsSent() {
            return segmentsSent;
        }

        public void setSegmentsSent(long segmentsSent) {
            this.segmentsSent = segmentsSent;
        }

        public long getSegmentsReceived() {
            return segmentsReceived;
        }

        public void setSegmentsReceived(long segmentsReceived) {
            this.segmentsReceived = segmentsReceived;
        }

        public long getSegmentsRetransmitted() {
            return segmentsRetransmitted;
        }

        public void setSegmentsRetransmitted(long segmentsRetransmitted) {
            this.segmentsRetransmitted = segmentsRetransmitted;
        }

        public long getInErrors() {
            return inErrors;
        }

        public void setInErrors(long inErrors) {
            this.inErrors = inErrors;
        }

        public long getOutResets() {
            return outResets;
        }

        public void setOutResets(long outResets) {
            this.outResets = outResets;
        }
    }

    public static class UdpStatsDTO {
        private long datagramsSent;
        private long datagramsReceived;
        private long datagramsNoPort;
        private long datagramsReceivedErrors;

        public long getDatagramsSent() {
            return datagramsSent;
        }

        public void setDatagramsSent(long datagramsSent) {
            this.datagramsSent = datagramsSent;
        }

        public long getDatagramsReceived() {
            return datagramsReceived;
        }

        public void setDatagramsReceived(long datagramsReceived) {
            this.datagramsReceived = datagramsReceived;
        }

        public long getDatagramsNoPort() {
            return datagramsNoPort;
        }

        public void setDatagramsNoPort(long datagramsNoPort) {
            this.datagramsNoPort = datagramsNoPort;
        }

        public long getDatagramsReceivedErrors() {
            return datagramsReceivedErrors;
        }

        public void setDatagramsReceivedErrors(long datagramsReceivedErrors) {
            this.datagramsReceivedErrors = datagramsReceivedErrors;
        }
    }

    public static class IPConnectionDTO {
        private String type;
        private byte[] localAddress;
        private int localPort;
        private byte[] foreignAddress;
        private int foreignPort;
        private InternetProtocolStats.TcpState state;
        private int transmitQueue;
        private int receiveQueue;
        private int owningProcessId;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public byte[] getLocalAddress() {
            return localAddress;
        }

        public void setLocalAddress(byte[] localAddress) {
            this.localAddress = localAddress;
        }

        public int getLocalPort() {
            return localPort;
        }

        public void setLocalPort(int localPort) {
            this.localPort = localPort;
        }

        public byte[] getForeignAddress() {
            return foreignAddress;
        }

        public void setForeignAddress(byte[] foreignAddress) {
            this.foreignAddress = foreignAddress;
        }

        public int getForeignPort() {
            return foreignPort;
        }

        public void setForeignPort(int foreignPort) {
            this.foreignPort = foreignPort;
        }

        public InternetProtocolStats.TcpState getState() {
            return state;
        }

        public void setState(InternetProtocolStats.TcpState state) {
            this.state = state;
        }

        public int getTransmitQueue() {
            return transmitQueue;
        }

        public void setTransmitQueue(int transmitQueue) {
            this.transmitQueue = transmitQueue;
        }

        public int getReceiveQueue() {
            return receiveQueue;
        }

        public void setReceiveQueue(int receiveQueue) {
            this.receiveQueue = receiveQueue;
        }

        public int getOwningProcessId() {
            return owningProcessId;
        }

        public void setOwningProcessId(int owningProcessId) {
            this.owningProcessId = owningProcessId;
        }
    }

    public static class OSSessionDTO {
        private String userName;
        private String terminalDevice;
        private long loginTime;
        private String host;

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getTerminalDevice() {
            return terminalDevice;
        }

        public void setTerminalDevice(String terminalDevice) {
            this.terminalDevice = terminalDevice;
        }

        public long getLoginTime() {
            return loginTime;
        }

        public void setLoginTime(long loginTime) {
            this.loginTime = loginTime;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }
    }

    public static class OSThreadDTO {
        private int owningProcessId;
        private int threadId;
        private String name;
        private OSProcess.State state;
        private long startMemoryAddress;
        private long contextSwitches;
        private long kernelTime;
        private long userTime;
        private long startTime;
        private long upTime;
        private int priority;
        private double threadCpuLoadCumulative;
        private long minorFaults;
        private long majorFaults;
        private boolean updateAttributes;

        public int getOwningProcessId() {
            return owningProcessId;
        }

        public void setOwningProcessId(int owningProcessId) {
            this.owningProcessId = owningProcessId;
        }

        public int getThreadId() {
            return threadId;
        }

        public void setThreadId(int threadId) {
            this.threadId = threadId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public OSProcess.State getState() {
            return state;
        }

        public void setState(OSProcess.State state) {
            this.state = state;
        }

        public long getStartMemoryAddress() {
            return startMemoryAddress;
        }

        public void setStartMemoryAddress(long startMemoryAddress) {
            this.startMemoryAddress = startMemoryAddress;
        }

        public long getContextSwitches() {
            return contextSwitches;
        }

        public void setContextSwitches(long contextSwitches) {
            this.contextSwitches = contextSwitches;
        }

        public long getKernelTime() {
            return kernelTime;
        }

        public void setKernelTime(long kernelTime) {
            this.kernelTime = kernelTime;
        }

        public long getUserTime() {
            return userTime;
        }

        public void setUserTime(long userTime) {
            this.userTime = userTime;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public long getUpTime() {
            return upTime;
        }

        public void setUpTime(long upTime) {
            this.upTime = upTime;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public double getThreadCpuLoadCumulative() {
            return threadCpuLoadCumulative;
        }

        public void setThreadCpuLoadCumulative(double threadCpuLoadCumulative) {
            this.threadCpuLoadCumulative = threadCpuLoadCumulative;
        }

        public long getMinorFaults() {
            return minorFaults;
        }

        public void setMinorFaults(long minorFaults) {
            this.minorFaults = minorFaults;
        }

        public long getMajorFaults() {
            return majorFaults;
        }

        public void setMajorFaults(long majorFaults) {
            this.majorFaults = majorFaults;
        }

        public boolean updateAttributes() {
            return updateAttributes;
        }

        public void setUpdateAttributes(boolean updateAttributes) {
            this.updateAttributes = updateAttributes;
        }
    }

    public static class OSProcessDTO {
        private int processID;
        private String currentWorkingDirectory;
        private String commandLine;
        private String name;
        private String path;
        private OSProcess.State state;
        private int parentProcessID;
        private int threadCount;
        private int priority;
        private long virtualSize;
        private long residentSetSize;
        private long kernelTime;
        private long userTime;
        private long startTime;
        private long upTime;
        private long openFiles;
        private int bitness;
        private String group;
        private String userID;
        private String user;
        private String groupID;
        private double processCpuLoadCumulative;
        private Double cpuUsagePercent;
        private Double memoryUsagePercent;
        @JsonIgnore
        private OSProcess previousProcess;

        public int getProcessID() {
            return processID;
        }

        public void setProcessID(int processID) {
            this.processID = processID;
        }

        public String getCurrentWorkingDirectory() {
            return currentWorkingDirectory;
        }

        public void setCurrentWorkingDirectory(String currentWorkingDirectory) {
            this.currentWorkingDirectory = currentWorkingDirectory;
        }

        public String getCommandLine() {
            return commandLine;
        }

        public void setCommandLine(String commandLine) {
            this.commandLine = commandLine;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }


        public OSProcess.State getState() {
            return state;
        }

        public void setState(OSProcess.State state) {
            this.state = state;
        }

        public int getParentProcessID() {
            return parentProcessID;
        }

        public void setParentProcessID(int parentProcessID) {
            this.parentProcessID = parentProcessID;
        }

        public int getThreadCount() {
            return threadCount;
        }

        public void setThreadCount(int threadCount) {
            this.threadCount = threadCount;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public long getVirtualSize() {
            return virtualSize;
        }

        public void setVirtualSize(long virtualSize) {
            this.virtualSize = virtualSize;
        }

        public long getResidentSetSize() {
            return residentSetSize;
        }

        public void setResidentSetSize(long residentSetSize) {
            this.residentSetSize = residentSetSize;
        }

        public long getKernelTime() {
            return kernelTime;
        }

        public void setKernelTime(long kernelTime) {
            this.kernelTime = kernelTime;
        }

        public long getUserTime() {
            return userTime;
        }

        public void setUserTime(long userTime) {
            this.userTime = userTime;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public long getUpTime() {
            return upTime;
        }

        public void setUpTime(long upTime) {
            this.upTime = upTime;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getUserID() {
            return userID;
        }

        public void setUserID(String userID) {
            this.userID = userID;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getGroupID() {
            return groupID;
        }

        public void setGroupID(String groupID) {
            this.groupID = groupID;
        }

        public double getProcessCpuLoadCumulative() {
            return processCpuLoadCumulative;
        }

        public void setProcessCpuLoadCumulative(double processCpuLoadCumulative) {
            this.processCpuLoadCumulative = processCpuLoadCumulative;
        }

        public Double getCpuUsagePercent() {
            return cpuUsagePercent;
        }

        public void setCpuUsagePercent(Double cpuUsagePercent) {
            this.cpuUsagePercent = cpuUsagePercent;
        }

        public Double getMemoryUsagePercent() {
            return memoryUsagePercent;
        }

        public void setMemoryUsagePercent(Double memoryUsagePercent) {
            this.memoryUsagePercent = memoryUsagePercent;
        }

        public OSProcess getPreviousProcess() {
            return previousProcess;
        }

        public void setPreviousProcess(OSProcess previousProcess) {
            this.previousProcess = previousProcess;
        }
    }

    public static class HardwareDTO {
        private ComputerSystemDTO computerSystem;
        private CentralProcessorDTO processor;
        private GlobalMemoryDTO memory;
        private List<HWDiskStoreDTO> diskStores;
        private List<LogicalVolumeGroupDTO> logicalVolumeGroups;
        private List<NetworkIFDTO> networkIFs;

        public ComputerSystemDTO getComputerSystem() {
            return computerSystem;
        }

        public void setComputerSystem(ComputerSystemDTO computerSystem) {
            this.computerSystem = computerSystem;
        }

        public CentralProcessorDTO getProcessor() {
            return processor;
        }

        public void setProcessor(CentralProcessorDTO processor) {
            this.processor = processor;
        }

        public GlobalMemoryDTO getMemory() {
            return memory;
        }

        public void setMemory(GlobalMemoryDTO memory) {
            this.memory = memory;
        }

        public List<HWDiskStoreDTO> getDiskStores() {
            return diskStores;
        }

        public void setDiskStores(List<HWDiskStoreDTO> diskStores) {
            this.diskStores = diskStores;
        }

        public List<LogicalVolumeGroupDTO> getLogicalVolumeGroups() {
            return logicalVolumeGroups;
        }

        public void setLogicalVolumeGroups(List<LogicalVolumeGroupDTO> logicalVolumeGroups) {
            this.logicalVolumeGroups = logicalVolumeGroups;
        }

        public List<NetworkIFDTO> getNetworkIFs() {
            return networkIFs;
        }

        public void setNetworkIFs(List<NetworkIFDTO> networkIFs) {
            this.networkIFs = networkIFs;
        }
    }

    public static class ComputerSystemDTO {
        private String manufacturer;
        private String hardwareUUID;
        private String serialNumber;
        private String model;
        private FirmwareDTO firmware;
        private BaseboardDTO baseboard;

        public String getManufacturer() {
            return manufacturer;
        }

        public void setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
        }

        public String getHardwareUUID() {
            return hardwareUUID;
        }

        public void setHardwareUUID(String hardwareUUID) {
            this.hardwareUUID = hardwareUUID;
        }

        public String getSerialNumber() {
            return serialNumber;
        }

        public void setSerialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public FirmwareDTO getFirmware() {
            return firmware;
        }

        public void setFirmware(FirmwareDTO firmware) {
            this.firmware = firmware;
        }

        public BaseboardDTO getBaseboard() {
            return baseboard;
        }

        public void setBaseboard(BaseboardDTO baseboard) {
            this.baseboard = baseboard;
        }
    }

    public static class FirmwareDTO {
        private String name;
        private String description;
        private String version;
        private String manufacturer;
        private String releaseDate;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getManufacturer() {
            return manufacturer;
        }

        public void setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
        }

        public String getReleaseDate() {
            return releaseDate;
        }

        public void setReleaseDate(String releaseDate) {
            this.releaseDate = releaseDate;
        }
    }

    public static class BaseboardDTO {
        private String version;
        private String manufacturer;
        private String serialNumber;
        private String model;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getManufacturer() {
            return manufacturer;
        }

        public void setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
        }

        public String getSerialNumber() {
            return serialNumber;
        }

        public void setSerialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public static class CentralProcessorDTO {
        private ProcessorIdentifierDTO processorIdentifier;
        private int logicalProcessorCount;
        private int physicalProcessorCount;
        private int physicalPackageCount;
        private double cpuLoad;
        @JsonIgnore
        private long[] prevTicks;

        public ProcessorIdentifierDTO getProcessorIdentifier() {
            return processorIdentifier;
        }

        public void setProcessorIdentifier(ProcessorIdentifierDTO processorIdentifier) {
            this.processorIdentifier = processorIdentifier;
        }

        public int getLogicalProcessorCount() {
            return logicalProcessorCount;
        }

        public void setLogicalProcessorCount(int logicalProcessorCount) {
            this.logicalProcessorCount = logicalProcessorCount;
        }

        public int getPhysicalProcessorCount() {
            return physicalProcessorCount;
        }

        public void setPhysicalProcessorCount(int physicalProcessorCount) {
            this.physicalProcessorCount = physicalProcessorCount;
        }

        public int getPhysicalPackageCount() {
            return physicalPackageCount;
        }

        public void setPhysicalPackageCount(int physicalPackageCount) {
            this.physicalPackageCount = physicalPackageCount;
        }

        public double getCpuLoad() {
            return cpuLoad;
        }

        public void setCpuLoad(double cpuLoad) {
            this.cpuLoad = cpuLoad;
        }

        public long[] getPrevTicks() {
            return prevTicks;
        }

        public void setPrevTicks(long[] prevTicks) {
            this.prevTicks = prevTicks;
        }
    }

    public static class ProcessorIdentifierDTO {
        private String vendor;
        private String name;
        private String family;
        private String model;
        private String stepping;
        private String processorID;
        private String identifier;
        private boolean cpu64bit;
        private long vendorFreq;
        private String microarchitecture;

        public String getVendor() {
            return vendor;
        }

        public void setVendor(String vendor) {
            this.vendor = vendor;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFamily() {
            return family;
        }

        public void setFamily(String family) {
            this.family = family;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getStepping() {
            return stepping;
        }

        public void setStepping(String stepping) {
            this.stepping = stepping;
        }

        public String getProcessorID() {
            return processorID;
        }

        public void setProcessorID(String processorID) {
            this.processorID = processorID;
        }

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public boolean isCpu64bit() {
            return cpu64bit;
        }

        public void setCpu64bit(boolean cpu64bit) {
            this.cpu64bit = cpu64bit;
        }

        public long getVendorFreq() {
            return vendorFreq;
        }

        public void setVendorFreq(long vendorFreq) {
            this.vendorFreq = vendorFreq;
        }

        public String getMicroarchitecture() {
            return microarchitecture;
        }

        public void setMicroarchitecture(String microarchitecture) {
            this.microarchitecture = microarchitecture;
        }
    }

    public static class LogicalProcessorDTO {
        private int processorNumber;
        private int physicalProcessorNumber;
        private int physicalPackageNumber;
        private int numaNode;
        private int processorGroup;

        public int getProcessorNumber() {
            return processorNumber;
        }

        public void setProcessorNumber(int processorNumber) {
            this.processorNumber = processorNumber;
        }

        public int getPhysicalProcessorNumber() {
            return physicalProcessorNumber;
        }

        public void setPhysicalProcessorNumber(int physicalProcessorNumber) {
            this.physicalProcessorNumber = physicalProcessorNumber;
        }

        public int getPhysicalPackageNumber() {
            return physicalPackageNumber;
        }

        public void setPhysicalPackageNumber(int physicalPackageNumber) {
            this.physicalPackageNumber = physicalPackageNumber;
        }

        public int getNumaNode() {
            return numaNode;
        }

        public void setNumaNode(int numaNode) {
            this.numaNode = numaNode;
        }

        public int getProcessorGroup() {
            return processorGroup;
        }

        public void setProcessorGroup(int processorGroup) {
            this.processorGroup = processorGroup;
        }
    }

    public static class PhysicalProcessorDTO {
        private int physicalPackageNumber;
        private int physicalProcessorNumber;
        private int efficiency;
        private String idString;

        public int getPhysicalPackageNumber() {
            return physicalPackageNumber;
        }

        public void setPhysicalPackageNumber(int physicalPackageNumber) {
            this.physicalPackageNumber = physicalPackageNumber;
        }

        public int getPhysicalProcessorNumber() {
            return physicalProcessorNumber;
        }

        public void setPhysicalProcessorNumber(int physicalProcessorNumber) {
            this.physicalProcessorNumber = physicalProcessorNumber;
        }

        public int getEfficiency() {
            return efficiency;
        }

        public void setEfficiency(int efficiency) {
            this.efficiency = efficiency;
        }

        public String getIdString() {
            return idString;
        }

        public void setIdString(String idString) {
            this.idString = idString;
        }
    }

    public static class ProcessorCacheDTO {
        private byte level;
        private byte associativity;
        private short lineSize;
        private int cacheSize;
        private String type;

        public byte getLevel() {
            return level;
        }

        public void setLevel(byte level) {
            this.level = level;
        }

        public byte getAssociativity() {
            return associativity;
        }

        public void setAssociativity(byte associativity) {
            this.associativity = associativity;
        }

        public short getLineSize() {
            return lineSize;
        }

        public void setLineSize(short lineSize) {
            this.lineSize = lineSize;
        }

        public int getCacheSize() {
            return cacheSize;
        }

        public void setCacheSize(int cacheSize) {
            this.cacheSize = cacheSize;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class GlobalMemoryDTO {
        private long total;
        private long available;
        private long pageSize;
        private VirtualMemoryDTO virtualMemory;
        private List<PhysicalMemoryDTO> physicalMemory;

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getAvailable() {
            return available;
        }

        public void setAvailable(long available) {
            this.available = available;
        }

        public long getPageSize() {
            return pageSize;
        }

        public void setPageSize(long pageSize) {
            this.pageSize = pageSize;
        }

        public VirtualMemoryDTO getVirtualMemory() {
            return virtualMemory;
        }

        public void setVirtualMemory(VirtualMemoryDTO virtualMemory) {
            this.virtualMemory = virtualMemory;
        }

        public List<PhysicalMemoryDTO> getPhysicalMemory() {
            return physicalMemory;
        }

        public void setPhysicalMemory(List<PhysicalMemoryDTO> physicalMemory) {
            this.physicalMemory = physicalMemory;
        }
    }

    public static class VirtualMemoryDTO {
        private long swapTotal;
        private long swapUsed;
        private long virtualMax;
        private long virtualInUse;
        private long swapPagesIn;
        private long swapPagesOut;

        public long getSwapTotal() {
            return swapTotal;
        }

        public void setSwapTotal(long swapTotal) {
            this.swapTotal = swapTotal;
        }

        public long getSwapUsed() {
            return swapUsed;
        }

        public void setSwapUsed(long swapUsed) {
            this.swapUsed = swapUsed;
        }

        public long getVirtualMax() {
            return virtualMax;
        }

        public void setVirtualMax(long virtualMax) {
            this.virtualMax = virtualMax;
        }

        public long getVirtualInUse() {
            return virtualInUse;
        }

        public void setVirtualInUse(long virtualInUse) {
            this.virtualInUse = virtualInUse;
        }

        public long getSwapPagesIn() {
            return swapPagesIn;
        }

        public void setSwapPagesIn(long swapPagesIn) {
            this.swapPagesIn = swapPagesIn;
        }

        public long getSwapPagesOut() {
            return swapPagesOut;
        }

        public void setSwapPagesOut(long swapPagesOut) {
            this.swapPagesOut = swapPagesOut;
        }
    }

    public static class PhysicalMemoryDTO {
        private String bankLabel;
        private long capacity;
        private long clockSpeed;
        private String manufacturer;
        private String memoryType;

        public String getBankLabel() {
            return bankLabel;
        }

        public void setBankLabel(String bankLabel) {
            this.bankLabel = bankLabel;
        }

        public long getCapacity() {
            return capacity;
        }

        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        public long getClockSpeed() {
            return clockSpeed;
        }

        public void setClockSpeed(long clockSpeed) {
            this.clockSpeed = clockSpeed;
        }

        public String getManufacturer() {
            return manufacturer;
        }

        public void setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
        }

        public String getMemoryType() {
            return memoryType;
        }

        public void setMemoryType(String memoryType) {
            this.memoryType = memoryType;
        }
    }

    public static class HWDiskStoreDTO {
        private String name;
        private String model;
        private String serial;
        private long size;
        private long reads;
        private long readBytes;
        private long writes;
        private long writeBytes;
        private long currentQueueLength;
        private long transferTime;
        private List<HWPartitionDTO> partitions;
        private long timeStamp;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getSerial() {
            return serial;
        }

        public void setSerial(String serial) {
            this.serial = serial;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public long getReads() {
            return reads;
        }

        public void setReads(long reads) {
            this.reads = reads;
        }

        public long getReadBytes() {
            return readBytes;
        }

        public void setReadBytes(long readBytes) {
            this.readBytes = readBytes;
        }

        public long getWrites() {
            return writes;
        }

        public void setWrites(long writes) {
            this.writes = writes;
        }

        public long getWriteBytes() {
            return writeBytes;
        }

        public void setWriteBytes(long writeBytes) {
            this.writeBytes = writeBytes;
        }

        public long getCurrentQueueLength() {
            return currentQueueLength;
        }

        public void setCurrentQueueLength(long currentQueueLength) {
            this.currentQueueLength = currentQueueLength;
        }

        public long getTransferTime() {
            return transferTime;
        }

        public void setTransferTime(long transferTime) {
            this.transferTime = transferTime;
        }

        public List<HWPartitionDTO> getPartitions() {
            return partitions;
        }

        public void setPartitions(List<HWPartitionDTO> partitions) {
            this.partitions = partitions;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public void setTimeStamp(long timeStamp) {
            this.timeStamp = timeStamp;
        }
    }

    public static class HWPartitionDTO {
        private String identification;
        private String name;
        private String type;
        private String uuid;
        private long size;
        private int major;
        private int minor;
        private String mountPoint;

        public String getIdentification() {
            return identification;
        }

        public void setIdentification(String identification) {
            this.identification = identification;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public int getMajor() {
            return major;
        }

        public void setMajor(int major) {
            this.major = major;
        }

        public int getMinor() {
            return minor;
        }

        public void setMinor(int minor) {
            this.minor = minor;
        }

        public String getMountPoint() {
            return mountPoint;
        }

        public void setMountPoint(String mountPoint) {
            this.mountPoint = mountPoint;
        }
    }

    public static class LogicalVolumeGroupDTO {
        private String name;
        private Set<String> physicalVolumes;
        private Map<String, Set<String>> logicalVolumes;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Set<String> getPhysicalVolumes() {
            return physicalVolumes;
        }

        public void setPhysicalVolumes(Set<String> physicalVolumes) {
            this.physicalVolumes = physicalVolumes;
        }

        public Map<String, Set<String>> getLogicalVolumes() {
            return logicalVolumes;
        }

        public void setLogicalVolumes(Map<String, Set<String>> logicalVolumes) {
            this.logicalVolumes = logicalVolumes;
        }
    }

    public static class NetworkIFDTO {
        private String name;
        private int index;
        private String displayName;
        private String ifAlias;
        private NetworkIF.IfOperStatus ifOperStatus;
        private long mtu;
        private String macaddr;
        private List<String> ipv4addr;
        private List<Short> subnetMasks;
        private List<String> ipv6addr;
        private List<Short> prefixLengths;
        private int ifType;
        private int ndisPhysicalMediumType;
        private boolean connectorPresent;
        private long speed;
        private long timeStamp;
        private boolean knownVmMacAddr;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getIfAlias() {
            return ifAlias;
        }

        public void setIfAlias(String ifAlias) {
            this.ifAlias = ifAlias;
        }

        public NetworkIF.IfOperStatus getIfOperStatus() {
            return ifOperStatus;
        }

        public void setIfOperStatus(NetworkIF.IfOperStatus ifOperStatus) {
            this.ifOperStatus = ifOperStatus;
        }

        public long getMtu() {
            return mtu;
        }

        public void setMtu(long mtu) {
            this.mtu = mtu;
        }

        public String getMacaddr() {
            return macaddr;
        }

        public void setMacaddr(String macaddr) {
            this.macaddr = macaddr;
        }

        public List<String> getIpv4addr() {
            return ipv4addr;
        }

        public void setIpv4addr(List<String> ipv4addr) {
            this.ipv4addr = ipv4addr;
        }

        public List<Short> getSubnetMasks() {
            return subnetMasks;
        }

        public void setSubnetMasks(List<Short> subnetMasks) {
            this.subnetMasks = subnetMasks;
        }

        public List<String> getIpv6addr() {
            return ipv6addr;
        }

        public void setIpv6addr(List<String> ipv6addr) {
            this.ipv6addr = ipv6addr;
        }

        public List<Short> getPrefixLengths() {
            return prefixLengths;
        }

        public void setPrefixLengths(List<Short> prefixLengths) {
            this.prefixLengths = prefixLengths;
        }

        public int getIfType() {
            return ifType;
        }

        public void setIfType(int ifType) {
            this.ifType = ifType;
        }

        public int getNdisPhysicalMediumType() {
            return ndisPhysicalMediumType;
        }

        public void setNdisPhysicalMediumType(int ndisPhysicalMediumType) {
            this.ndisPhysicalMediumType = ndisPhysicalMediumType;
        }

        public boolean isConnectorPresent() {
            return connectorPresent;
        }

        public void setConnectorPresent(boolean connectorPresent) {
            this.connectorPresent = connectorPresent;
        }

        public long getSpeed() {
            return speed;
        }

        public void setSpeed(long speed) {
            this.speed = speed;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public void setTimeStamp(long timeStamp) {
            this.timeStamp = timeStamp;
        }

        public boolean isKnownVmMacAddr() {
            return knownVmMacAddr;
        }

        public void setKnownVmMacAddr(boolean knownVmMacAddr) {
            this.knownVmMacAddr = knownVmMacAddr;
        }
    }
}
