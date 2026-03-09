package br.com.evolui.portalevolui.web.config;

import br.com.evolui.portalevolui.web.component.AuthEntryPointJwtComponent;
import br.com.evolui.portalevolui.web.security.DaoProviderSecurity;
import br.com.evolui.portalevolui.web.security.JWTAuthorizationFilterSecurity;
import br.com.evolui.portalevolui.web.service.UserAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
@EnableGlobalMethodSecurity(
        prePostEnabled = true,
        securedEnabled = true)
public class SecurityConfiguration {

    @Autowired
    UserAuthenticationService userDetailsService;

    @Autowired
    private AuthEntryPointJwtComponent unauthorizedHandler;

    @Bean
    public JWTAuthorizationFilterSecurity authenticationJwtTokenFilter() {
        return new JWTAuthorizationFilterSecurity();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoProviderSecurity authProvider = new DaoProviderSecurity();

        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }


    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/public/**"))
                //.requestMatchers(AntPathRequestMatcher.antMatcher("/app/**"))
                //.requestMatchers(AntPathRequestMatcher.antMatcher("/portalEvoluiWebSocket"))
                //.requestMatchers(AntPathRequestMatcher.antMatcher("/portalEvoluiWebSocket/**"))
                .requestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**"));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(x -> x.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(x -> x.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .authorizeHttpRequests(auth -> auth

                        // 🔓 Swagger / OpenAPI
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/swagger-ui/**"),
                                AntPathRequestMatcher.antMatcher("/v3/api-docs/**")
                        ).permitAll()

                        // 🔓 públicos
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/public/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/test/**")).permitAll()

                        // 🔐 protegidos
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/portalEvoluiWebSocket")).authenticated()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/app/**")).authenticated()

                        // regras genéricas
                        .requestMatchers(RegexRequestMatcher.regexMatcher("^(?!.*/app).*$")).permitAll()
                        .requestMatchers(RegexRequestMatcher.regexMatcher("^(?!.*/api).*$")).permitAll()

                        .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(
                authenticationJwtTokenFilter(),
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }



}
