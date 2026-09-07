package com.portfolio.security;
import com.portfolio.domain.AppUser; import com.portfolio.repository.AppUserRepository; import org.springframework.context.annotation.*; import org.springframework.security.config.annotation.web.builders.HttpSecurity; import org.springframework.security.core.userdetails.*; import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.security.web.SecurityFilterChain;
@Configuration public class SecurityConfig {
 @Bean SecurityFilterChain securityFilterChain(HttpSecurity http)throws Exception{return http.authorizeHttpRequests(a->a.requestMatchers("/login","/h2-console/**","/css/**","/actuator/health").permitAll().anyRequest().authenticated()).formLogin(f->f.permitAll()).httpBasic(b->{}).logout(l->l.logoutSuccessUrl("/login?logout")).csrf(c->c.ignoringRequestMatchers("/h2-console/**","/api/**")).headers(h->h.frameOptions(frame->frame.sameOrigin())).build();}
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean UserDetailsService userDetailsService(AppUserRepository repository){return username->repository.findByUserId(username).map(this::details).orElseThrow(()->new UsernameNotFoundException(username));}
 private UserDetails details(AppUser u){return User.withUsername(u.getUserId()).password(u.getPasswordHash()).roles(u.getRole()).disabled(!u.isEnabled()).build();}
}
