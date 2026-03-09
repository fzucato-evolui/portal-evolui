package br.com.evolui.healthchecker.controller;

import br.com.evolui.healthchecker.util.UtilFunctions;
import br.com.evolui.portalevolui.shared.dto.*;
import br.com.evolui.portalevolui.shared.enums.HealthCheckerAlertTypeEnum;
import br.com.evolui.portalevolui.shared.enums.HealthCheckerModuleTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Pattern;

public class HealthCheckController {
    HealthCheckerConfigDTO config;
    public HealthCheckController(HealthCheckerConfigDTO config) {
        this.config = config;
    }
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckController.class);
    public HealthCheckerDTO check(HealthCheckerSystemInfoDTO si) {
        logger.debug("Verificando healthcheck dos módulos...");
        HealthCheckerDTO dto = new HealthCheckerDTO();
        dto.setId(config.getId());
        for(Map.Entry<HealthCheckerAlertTypeEnum, HealthCheckerAlertConfigDTO> e : this.config.getAlerts().entrySet()) {
            if (e.getValue().getMaxPercentual() > 0) {
                if (e.getKey() == HealthCheckerAlertTypeEnum.MEMORY) {
                    HealthCheckerSystemInfoDTO.GlobalMemoryDTO mem = si.getHardware().getMemory();
                    long used = mem.getTotal() - mem.getAvailable();
                    long percent = Math.round((double)used * 100 / mem.getTotal());
                    if (percent > e.getValue().getMaxPercentual()) {
                        HealthCheckerAlertDTO alert = new HealthCheckerModuleDTO();
                        alert.setAlertType(e.getKey());
                        alert.setHealth(false);
                        alert.setError(String.format("Consumo de %s%%", percent));
                        dto.addAlert(alert);
                    }
                } else if (e.getKey() == HealthCheckerAlertTypeEnum.OPENED_FILES) {
                    HealthCheckerSystemInfoDTO.FileSystemDTO mem = si.getOperatingSystem().getFileSystem();
                    long used = mem.getOpenFileDescriptors();
                    long percent = Math.round((double)used * 100 / mem.getMaxFileDescriptors());
                    if (percent > e.getValue().getMaxPercentual()) {
                        HealthCheckerAlertDTO alert = new HealthCheckerModuleDTO();
                        alert.setAlertType(e.getKey());
                        alert.setHealth(false);
                        alert.setError(String.format("Consumo de %s%%", percent));
                        dto.addAlert(alert);
                    }
                } else if (e.getKey() == HealthCheckerAlertTypeEnum.DISK_USAGE) {
                    HealthCheckerSystemInfoDTO.FileSystemDTO mem = si.getOperatingSystem().getFileSystem();
                    for (HealthCheckerSystemInfoDTO.FileStoreDTO d : mem.getFileStores()) {
                        long used = d.getTotalSpace() - d.getFreeSpace();
                        long percent = Math.round((double)used * 100 / d.getTotalSpace());
                        if (percent > e.getValue().getMaxPercentual()) {
                            HealthCheckerAlertDTO alert = new HealthCheckerModuleDTO();
                            alert.setAlertType(e.getKey());
                            alert.setHealth(false);
                            alert.setError(String.format("Disco %s: Consumo de %s%%", d.getLabel(), percent));
                            dto.addAlert(alert);
                        }
                    }

                } else if (e.getKey() == HealthCheckerAlertTypeEnum.CPU) {
                    HealthCheckerSystemInfoDTO.CentralProcessorDTO mem = si.getHardware().getProcessor();
                    long percent = Math.round(mem.getCpuLoad());
                    if (percent > e.getValue().getMaxPercentual()) {
                        HealthCheckerAlertDTO alert = new HealthCheckerModuleDTO();
                        alert.setAlertType(e.getKey());
                        alert.setHealth(false);
                        alert.setError(String.format("Consumo de %s%%", percent));
                        dto.addAlert(alert);
                    }

                }
            }
        }
        for (HealthCheckerModuleConfigDTO module : config.getModules()) {
            dto.addModule(this.checkModule(module));
        }
        return dto;
    }
    public HealthCheckerModuleDTO checkModule(HealthCheckerModuleConfigDTO module) {
        logger.debug("Módulo " +module.getDescription());
        HealthCheckerModuleDTO moduleDTO = new HealthCheckerModuleDTO();
        moduleDTO.setId(module.getId());
        Pattern p = Pattern.compile(new String(module.getAcceptableResponsePattern().getBytes()));
        try {
            if (module.getModuleType() == HealthCheckerModuleTypeEnum.EXECUTABLE) {
                String resp = new CommandLineController().executeSingleCommand(module.getCommandAddress(), 30L);
                byte[] a = resp.getBytes();
                /*
                for(int i=0; i< a.length ; i++) {
                    System.out.print(a[i] + ", ");
                }
                */
                if (p.matcher(resp).find()) {
                    moduleDTO.setHealth(true);
                } else {
                    throw new Exception(resp);
                }

            } else if (module.getModuleType() == HealthCheckerModuleTypeEnum.WEB) {
                String resp = new WebClientController(this.config).doHealthCheck(module);
                if (p.matcher(resp).find()) {
                    moduleDTO.setHealth(true);
                } else {
                    throw new Exception(resp);
                }
            }
            logger.debug("SUCESSO");
        } catch(Throwable ex) {
            logger.error(UtilFunctions.exceptionToString(ex));
            moduleDTO.setError(ex.getMessage());
        }
        return moduleDTO;
    }
}
