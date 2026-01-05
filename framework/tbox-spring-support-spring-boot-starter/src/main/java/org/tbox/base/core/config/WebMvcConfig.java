package org.tbox.base.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.format.DateTimeFormatter;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    @Override
    public void addFormatters(FormatterRegistry registry) {

        registry.addFormatter(new DateFormatter(DATE_TIME_PATTERN));

        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setDateTimeFormatter(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
        registrar.setDateFormatter(DateTimeFormatter.ofPattern(DATE_PATTERN));
        registrar.registerFormatters(registry);
    }


}
