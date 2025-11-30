package com.the198thstreet.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * 외부 HTTP 요청을 담당하는 {@link RestTemplate} 한 개만 정의하는 설정 클래스입니다.
 * <p>
 * <strong>왜 이렇게 단순하게 유지했나요?</strong>
 * <ul>
 *     <li>지금 애플리케이션은 구글 RSS 한 곳만 호출합니다.</li>
 *     <li>초보자도 바로 이해할 수 있도록 커스텀 설정을 최소화했습니다.</li>
 *     <li>모든 호출이 UTF-8 텍스트이므로 문자열 컨버터 우선순위만 명확히 지정합니다.</li>
 * </ul>
 */
@Configuration
public class RestClientConfig {

    /**
     * 구글 RSS를 호출할 때 사용할 단 하나의 {@link RestTemplate} 빈을 생성합니다.
     * <p>
     * - 연결/응답 타임아웃을 짧게(5초/10초) 두어, 외부 지연이 발생해도 스케줄러 스레드가 오래 잡히지 않도록 합니다.<br>
     * - 기본 문자열 컨버터를 제거하고 UTF-8 컨버터를 맨 앞에 둡니다. RSS XML은 UTF-8이므로 인코딩 꼬임을 예방합니다.<br>
     * - 별도 인터셉터나 에러 핸들러는 넣지 않았습니다. 실패는 호출부에서 try-catch로 처리합니다.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();

        // 기본 StringHttpMessageConverter 들은 우선순위가 뒤섞일 수 있으므로 모두 제거한 뒤
        // UTF-8 컨버터를 맨 앞에 삽입해 "항상" UTF-8로 읽도록 강제합니다.
        restTemplate.getMessageConverters().removeIf(converter -> converter instanceof StringHttpMessageConverter);
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }
}
