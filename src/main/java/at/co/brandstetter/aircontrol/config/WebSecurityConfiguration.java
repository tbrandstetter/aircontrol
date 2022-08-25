package at.co.brandstetter.aircontrol.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/api/v1", "/index")
                .permitAll();

        http.cors().and().csrf().disable();

        /* .and().authorizeRequests().antMatchers("/h2/**").permitAll();

        http.csrf().disable();

        http.headers().frameOptions().disable(); */

        return http.build();
    }
}