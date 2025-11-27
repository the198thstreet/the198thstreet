package com.the198thstreet;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 아주 단순한 {@link RestTemplate} 빈을 등록한다.
 * <p>
 * 별도 커스터마이징 없이 기본 설정을 사용하지만,
 * 한 곳에서 생성해두면 여러 컴포넌트에서 손쉽게 주입받아 재사용할 수 있다.
 */
@Configuration
public class RestTemplateConfig {

        /**
         * 스프링 컨테이너에 {@link RestTemplate} 빈을 등록한다.
         * @return HTTP 호출을 수행할 수 있는 기본 템플릿 객체
         */
        @Bean
        public RestTemplate restTemplate() {
                return new RestTemplate();
        }
}
