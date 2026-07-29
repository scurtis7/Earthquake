package com.scurtis.earthquakes.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import static com.scurtis.earthquakes.common.AppConstants.USGS_BASE_URL;

@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "scurtis")
public class AppConfig {

    private String appVersion;

    @PostConstruct
    public void init() {
        log.info("Earthquakes Server Version: {}", appVersion);
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .baseUrl(USGS_BASE_URL)
            .exchangeStrategies(ExchangeStrategies
                .builder()
                .codecs(codecs -> codecs
                    .defaultCodecs()
                    .maxInMemorySize(10000 * 1024))
                .build())
            .build();
    }

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
            .baseUrl(USGS_BASE_URL)
            .requestFactory(new JdkClientHttpRequestFactory())
            .build();
    }

}
