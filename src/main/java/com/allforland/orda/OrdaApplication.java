package com.allforland.orda;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class OrdaApplication extends SpringBootServletInitializer {
    /**
     * 외부 Tomcat에서 WAR를 초기화할 때 애플리케이션의 Spring 설정을 등록한다.
     *
     * @param application 서블릿 컨테이너에서 사용할 애플리케이션 빌더
     * @return ORDA 설정 클래스가 등록된 애플리케이션 빌더
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(OrdaApplication.class);
    }
}
