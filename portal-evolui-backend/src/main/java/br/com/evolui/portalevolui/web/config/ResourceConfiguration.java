package br.com.evolui.portalevolui.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class ResourceConfiguration implements WebMvcConfigurer {

    private String externalFolder;

    public ResourceConfiguration(@Value("${evolui.app.externalFolder}") final String externalFolder, final WebProperties properties) {
        this.externalFolder = externalFolder;
        if (StringUtils.hasLength(this.externalFolder)) {
            String[] staticResources = properties.getResources().getStaticLocations();
            List<String> append = new ArrayList<>();
            append.addAll(Arrays.asList(staticResources));
            append.add(this.externalFolder);
            properties.getResources().setStaticLocations(append.toArray(new String[0]));
            int ze = 0;

        }
    }


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (StringUtils.hasLength(this.externalFolder)) {
            registry
                    .addResourceHandler("/server-files/**")
                    .addResourceLocations(externalFolder);

        }

    }
}
