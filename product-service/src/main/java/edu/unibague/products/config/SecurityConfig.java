package edu.unibague.products.config;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import java.util.*;

@Configuration

public class SecurityConfig {

 @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
  return http.csrf(csrf->csrf.disable())
   .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a->a
    .requestMatchers("/actuator/health/**","/swagger-ui/**","/v3/api-docs/**").permitAll()
    .requestMatchers(HttpMethod.GET,"/api/products/**").authenticated()
    .requestMatchers("/api/products/**").hasRole("ADMIN")
    .anyRequest().authenticated())
   .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(jwtConverter())))
   .build();
 }
 
 @Bean Converter<Jwt,? extends AbstractAuthenticationToken> jwtConverter(){
  JwtAuthenticationConverter c=new JwtAuthenticationConverter();
  c.setJwtGrantedAuthoritiesConverter(jwt->{
   Object value=jwt.getClaim("roles");
   Collection<String> roles=value instanceof Collection<?> xs
    ? xs.stream().map(Object::toString).toList()
    : (value==null ? List.of() : List.of(value.toString()));
   return roles.stream().map(role->(GrantedAuthority)new SimpleGrantedAuthority(
     role.startsWith("ROLE_")?role:"ROLE_"+role)).toList();
  });return c;
 }

 @Bean JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.public-key-location}") Resource resource){
  try {
   String pem=resource.getContentAsString(StandardCharsets.UTF_8)
     .replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
   RSAPublicKey key=(RSAPublicKey)KeyFactory.getInstance("RSA")
       .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pem)));
   NimbusJwtDecoder decoder=NimbusJwtDecoder.withPublicKey(key).signatureAlgorithm(SignatureAlgorithm.RS256).build();
   decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("proyecto03-auth"));
   return decoder;
  } catch(Exception ex) { throw new IllegalStateException("Clave RSA pública inválida",ex); }
 }
}
