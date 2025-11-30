package com.the198thstreet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.the198thstreet.news.google.GoogleNewsProperties;

/**
 * 스프링 부트 애플리케이션의 진입점.
 * <p>
 * 별도의 설정 없이도 {@link SpringApplication} 이 내부에서 자동 설정을 수행하며,
 * 내장 톰캣을 띄워 REST API 서버를 실행한다.
 */
@SpringBootApplication
@EnableConfigurationProperties(GoogleNewsProperties.class)
@EnableScheduling // 스케줄러 활성화
public class The198thstreetApplication {

        /**
         * 애플리케이션을 실행한다.
         * @param args 실행 시 전달되는 커맨드 라인 인수
         */
        public static void main(String[] args) {
                SpringApplication.run(The198thstreetApplication.class, args);
        }

}
