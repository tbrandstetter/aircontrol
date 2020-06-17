package at.co.brandstetter.aircontrolservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/api/v1", "/index")
                .permitAll();

        http.cors().and().csrf().disable();

        /* .and().authorizeRequests().antMatchers("/h2/**").permitAll();

        http.csrf().disable();

        http.headers().frameOptions().disable(); */
    }
}