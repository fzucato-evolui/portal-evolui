package br.com.evolui.portalevolui.web.rest.dto.aws;

import br.com.evolui.portalevolui.web.rest.dto.enums.BucketFileTypeEnum;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.StringUtils;

public class BucketDTO {
    private String account;
    private String name;
    private BucketFileTypeEnum type;
    private String path;
    private String arn;

    public BucketDTO() {

    }
    public BucketDTO(String account, String name, BucketFileTypeEnum bucket, String path, String bucketArn) {
        this.account = account;
        this.name = name;
        this.type = bucket;
        this.path = path;
        this.arn = bucketArn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BucketFileTypeEnum getType() {
        return type;
    }

    public void setType(BucketFileTypeEnum type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        if (!StringUtils.isEmpty(path) && !StringUtils.isEmpty(this.name)) {
            // Verifica se o path já termina com o nome; se não, adiciona
            if (!path.endsWith("/" + this.name)) {
                if (!path.endsWith("/")) {
                    path += "/";
                }
                path += this.name;
            }
        }
        this.path = path;
    }

    public String getArn() {
        return arn;
    }

    public void setArn(String arn) {
        if (!StringUtils.isEmpty(arn) && !StringUtils.isEmpty(this.name)) {
            // Verifica se o path já termina com o nome; se não, adiciona
            if (!arn.endsWith("/" + this.name)) {
                if (!arn.endsWith("/")) {
                    arn += "/";
                }
                arn += this.name;
            }
        }
        this.arn = arn;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonGetter
    public String getBucket() {
        if (StringUtils.isEmpty(path)) {
            return "";
        }
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        return normalizedPath.split("/", 2)[0]; // Sempre pega a primeira parte, que é o bucket
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonGetter
    public String getKey() {
        if (StringUtils.isEmpty(path)) {
            return "";
        }
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        String[] parts = normalizedPath.split("/", 2);
        return parts.length > 1 ? parts[1] : ""; // Pega tudo depois do bucket, se existir
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonGetter
    public String getPrefix() {
        if (StringUtils.isEmpty(path) || StringUtils.isEmpty(name)) {
            return "";
        }
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        int bucketEndIndex = normalizedPath.indexOf("/");
        if (bucketEndIndex == -1 || !normalizedPath.contains("/")) {
            return ""; // Não há prefixo se não houver subdiretórios
        }
        String key = normalizedPath.substring(bucketEndIndex + 1);
        if (!key.contains("/")) {
            return ""; // Não há prefixo se não houver mais subdiretórios
        }
        int lastSlashIndex = key.lastIndexOf("/");
        return lastSlashIndex > 0 ? "/" + key.substring(0, lastSlashIndex + 1) : "";
    }
}
