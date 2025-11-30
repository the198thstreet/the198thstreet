package com.the198thstreet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.the198thstreet.news.google.GoogleNewsProperties;

/**
 * 스프링 부트 애플리케이션의 진입점.
 * <p>
 * - @EnableScheduling 을 통해 RSS 수집 스케줄러(GoogleHeadlineNewsService)가 자동 등록된다.
 * - @EnableConfigurationProperties 로 news.collector.google.* 설정을 POJO(GoogleNewsProperties)로 주입한다.
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
