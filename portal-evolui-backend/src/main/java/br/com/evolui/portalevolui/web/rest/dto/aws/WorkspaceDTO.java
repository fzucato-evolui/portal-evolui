package br.com.evolui.portalevolui.web.rest.dto.aws;

import software.amazon.awssdk.services.workspaces.model.RunningMode;

public class WorkspaceDTO {
    private String id;
    private String userName;
    private String computerName;
    private String state;
    private String account;
    private RunningMode runningMode;
    private String privateIpAddress;
    private String privateDns;
    private String publicIpAddress;
    private String publicDns;
    private Integer rootVolumeSizeGib;
    private Integer userVolumeSizeGib;
    private String os;
    private String platform;
    private String protocol;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getComputerName() {
        return computerName;
    }

    public void setComputerName(String computerName) {
        this.computerName = computerName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public RunningMode getRunningMode() {
        return runningMode;
    }

    public void setRunningMode(RunningMode runningMode) {
        this.runningMode = runningMode;
    }


    public String getPrivateIpAddress() {
        return privateIpAddress;
    }

    public void setPrivateIpAddress(String privateIpAddress) {
        this.privateIpAddress = privateIpAddress;
    }

    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public Integer getRootVolumeSizeGib() {
        return rootVolumeSizeGib;
    }

    public void setRootVolumeSizeGib(Integer rootVolumeSizeGib) {
        this.rootVolumeSizeGib = rootVolumeSizeGib;
    }

    public Integer getUserVolumeSizeGib() {
        return userVolumeSizeGib;
    }

    public void setUserVolumeSizeGib(Integer userVolumeSizeGib) {
        this.userVolumeSizeGib = userVolumeSizeGib;
    }

    public String getPrivateDns() {
        return privateDns;
    }

    public void setPrivateDns(String privateDns) {
        this.privateDns = privateDns;
    }

    public String getPublicDns() {
        return publicDns;
    }

    public void setPublicDns(String publicDns) {
        this.publicDns = publicDns;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
