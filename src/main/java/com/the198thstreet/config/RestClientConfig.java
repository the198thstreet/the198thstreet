package com.the198thstreet.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * 외부 API 통신에 사용할 {@link RestTemplate} 설정을 정의한다.
 * <p>
 * 네이버 뉴스 API는 UTF-8 응답을 반환하므로, 문자열 컨버터를 UTF-8 우선으로 교체한다.
 * 기본 컨버터를 제거하지 않으면 환경에 따라 잘못된 인코딩이 적용될 수 있어 명시적으로 교체한다.
 */
@Configuration
public class RestClientConfig {

    /**
     * UTF-8 문자열 처리가 보장되는 {@link RestTemplate} 빈을 생성한다.
     *
     * @param builder 스프링에서 제공하는 빌더로, 커스텀 설정을 추가해 인스턴스를 만든다.
     * @return UTF-8 기반 메시지 컨버터가 우선 적용된 {@link RestTemplate}
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();

        // 기본 StringHttpMessageConverter 를 제거하고 UTF-8 컨버터를 맨 앞에 추가한다.
        restTemplate.getMessageConverters().removeIf(converter -> converter instanceof StringHttpMessageConverter);
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }
}
