package com.allforland.orda.common.config;

import com.allforland.orda.common.error.ApiErrorController;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                "api",
                clazz ->
                        clazz.isAnnotationPresent(RestController.class)
                                && clazz.getPackageName().startsWith("com.allforland.orda.domain.")
                                && !ApiErrorController.class.isAssignableFrom(clazz));
    }
}
