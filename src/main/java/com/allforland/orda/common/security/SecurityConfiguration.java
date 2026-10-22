package com.allforland.orda.common.security;

import com.allforland.orda.common.error.ApiErrorResponse;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.common.logging.TraceIdFilter;
import com.allforland.orda.domain.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.DispatcherTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/** JWT 인증 경로, CORS와 공통 보안 오류 응답을 설정한다. */
@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {
    private final ObjectMapper mapper;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtTokenService tokens,
            AuthService auth,
            @Value("${orda.auth.cors.allowed-origins:}") String origins)
            throws Exception {
        http.csrf().disable().formLogin().disable().httpBasic().disable().logout().disable();
        http.requestCache().disable();
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.addFilterBefore(corsFilter(origins), CsrfFilter.class);
        String[] publicReadPaths = {
            "/actuator/health",
            "/actuator/health/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-ui.html",
            "/swagger-ui/**"
        };
        http.authorizeRequests()
                .requestMatchers(new DispatcherTypeRequestMatcher(DispatcherType.ERROR))
                .permitAll()
                .antMatchers(
                        HttpMethod.POST,
                        "/api/users/signup",
                        "/api/users/signup/",
                        "/api/auth/login",
                        "/api/auth/login/")
                .permitAll()
                .antMatchers(HttpMethod.GET, publicReadPaths)
                .permitAll()
                .antMatchers(HttpMethod.HEAD, publicReadPaths)
                .permitAll()
                .anyRequest()
                .authenticated();
        http.exceptionHandling()
                .authenticationEntryPoint(this::unauthorized)
                .accessDeniedHandler(
                        (request, response, exception) ->
                                writeError(request, response, ErrorCode.FORBIDDEN));
        http.oauth2ResourceServer()
                .withObjectPostProcessor(
                        new ObjectPostProcessor<BearerTokenAuthenticationFilter>() {
                            @Override
                            public <O extends BearerTokenAuthenticationFilter> O postProcess(
                                    O filter) {
                                filter.setAuthenticationFailureHandler(
                                        SecurityConfiguration.this::unauthorized);
                                return filter;
                            }
                        })
                .authenticationEntryPoint(this::unauthorized)
                .accessDeniedHandler(
                        (request, response, exception) ->
                                writeError(request, response, ErrorCode.FORBIDDEN))
                .jwt()
                .decoder(tokens)
                .jwtAuthenticationConverter(auth::authenticate);
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer firewallErrors() {
        return web ->
                web.requestRejectedHandler(
                        (request, response, exception) ->
                                writeError(request, response, ErrorCode.INVALID_REQUEST));
    }

    private CorsFilter corsFilter(String origins) {
        List<String> allowed =
                Arrays.stream(origins.split(","))
                        .map(String::strip)
                        .filter(origin -> !origin.isEmpty())
                        .distinct()
                        .toList();
        for (String origin : allowed) {
            URI uri;
            try {
                uri = URI.create(origin);
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("CORS origins must be explicit HTTP(S) origins");
            }
            if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null
                    || !uri.getPath().isEmpty()
                    || uri.getPort() > 65535
                    || origin.contains("*")) {
                throw new IllegalStateException("CORS origins must be explicit HTTP(S) origins");
            }
        }
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowed);
        configuration.setAllowedMethods(
                List.of("GET", "HEAD", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "If-Match", "Accept"));
        configuration.setExposedHeaders(
                List.of("ETag", "Location", "X-Trace-Id", "WWW-Authenticate"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        CorsFilter filter = new CorsFilter(source);
        DefaultCorsProcessor processor =
                new DefaultCorsProcessor() {
                    @Override
                    protected void rejectRequest(ServerHttpResponse response) {
                        response.setStatusCode(HttpStatus.FORBIDDEN);
                    }
                };
        filter.setCorsProcessor(
                (config, request, response) -> {
                    boolean accepted = processor.processRequest(config, request, response);
                    if (!accepted) {
                        writeError(request, response, ErrorCode.FORBIDDEN);
                    }
                    return accepted;
                });
        return filter;
    }

    private void unauthorized(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException {
        ErrorCode code =
                exception instanceof AuthenticationServiceException
                        ? ErrorCode.INTERNAL_SERVER_ERROR
                        : ErrorCode.UNAUTHORIZED;
        if (code == ErrorCode.UNAUTHORIZED) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        writeError(request, response, code);
    }

    private void writeError(
            HttpServletRequest request, HttpServletResponse response, ErrorCode code)
            throws IOException {
        String traceId = TraceIdFilter.getOrCreateTraceId(request);
        response.setStatus(code.getStatus().value());
        response.setHeader(TraceIdFilter.HEADER_NAME, traceId);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(
                response.getOutputStream(),
                new ApiErrorResponse(
                        code.getStatus().value(),
                        code.name(),
                        code.getMessage(),
                        List.of(),
                        traceId));
    }
}
