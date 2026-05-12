
 W tych AuthController, DeviceController itd. dodane są adnotacje 
 ```java 
 @CrossOrigin(origins = "http://localhost:5173")
```
i ten import 
 ```java
org.springframework.web.bind.annotation.CrossOrigin;
```

W WebSecurityConfig.java dodane jest konfiguracja CORS taka o:
```java
     @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173")); // Pozwalamy frontendowi
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
```
Ten SecurityFilterChain tez jest zmieniony na taki bo inaczej nie działało:
```java
    @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .cors(c -> c.configurationSource(corsConfigurationSource()))
                    .exceptionHandling(e -> e.authenticationEntryPoint(unauthorizedHandler))
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(a -> a
                            // --- KLUCZOWA LINIJKA: Pozwalamy na wszystkie zapytania OPTIONS bez tokenu ---
                            .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll() 
                            
                            .requestMatchers("/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                            .anyRequest().authenticated()
                    );

            http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
            return http.build();
        }
}
```

pobierasz reacta i takie biblioteki do reacta: ```npm install axios react-router-dom bootstrap react-bootstrap```
```npm run dev```  tym sie startuje
