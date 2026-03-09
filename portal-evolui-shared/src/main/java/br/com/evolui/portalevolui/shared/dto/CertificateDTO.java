package br.com.evolui.portalevolui.shared.dto;

import br.com.evolui.portalevolui.shared.enums.CertificateTypeEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.net.ssl.KeyManagerFactory;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;

public class CertificateDTO {
    private CertificateTypeEnum certificateType;
    private byte[] file;
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password)  {
        this.password = password;
    }

    public CertificateTypeEnum getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(CertificateTypeEnum certificateType) {
        this.certificateType = certificateType;
    }

    @JsonIgnore
    public KeyManagerFactory getKeystoreFactory() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(this.certificateType.value());
        keyStore.load(new ByteArrayInputStream(this.getFile()), this.password.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, this.password.toCharArray());
        return kmf;
    }

    @JsonIgnore
    public KeyStore getKeystore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(this.certificateType.value());
        keyStore.load(new ByteArrayInputStream(this.getFile()), this.password.toCharArray());
        return keyStore;
    }

    public byte[] getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }
}
