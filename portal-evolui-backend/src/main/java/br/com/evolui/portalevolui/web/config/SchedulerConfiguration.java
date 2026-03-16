package br.com.evolui.portalevolui.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableScheduling
@EnableAsync(proxyTargetClass = true)
public class SchedulerConfiguration {

    @Value("${evolui.app.externalFolder}")
    protected String externalFolder;

    @Value("${evolui.app.jwtExpirationMs}")
    protected int jwtExpirationMs;

    //@Scheduled(cron = "0 */1 * * * *")
    public void deleteTempFiles() {

        try {
            LocalDateTime now = LocalDateTime.now();
            String folder = Paths.get(Paths.get(new URL(this.externalFolder).toURI()).toString(),
                    "temp").toString();
            if (Files.exists(Paths.get(folder))) {
                Set<File> files = Stream.of(new File(folder).listFiles())
                        .filter(file -> !file.isDirectory())
                        .collect(Collectors.toSet());

                files.stream().forEach(f -> {
                    try {
                        LocalDateTime lastModified = LocalDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()),
                                TimeZone.getDefault().toZoneId());

                        long diff = ChronoUnit.MILLIS.between(lastModified, now);
                        if (diff > this.jwtExpirationMs) {
                            f.delete();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
