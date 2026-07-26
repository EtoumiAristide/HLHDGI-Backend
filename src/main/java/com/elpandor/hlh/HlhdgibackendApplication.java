package com.elpandor.hlh;

import com.elpandor.hlh.common.core.FileStorageProperties;
import com.elpandor.hlh.common.core.security.SpringSecurityAuditorAware;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing(auditorAwareRef="auditorAware")
@EnableCaching
@EnableConfigurationProperties({
        FileStorageProperties.class
})
public class HlhdgibackendApplication {

    @Bean
    public AuditorAware<String> auditorAware() {
        return new SpringSecurityAuditorAware();
    }

    public static void main(String[] args) {
        SpringApplication.run(HlhdgibackendApplication.class, args);
    }

    /*@Bean
    CommandLineRunner init(RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        return args -> {
            // Création des rôles
            Role adminRole = new Role();
            adminRole.setName(ERole.ROLE_ADMIN);
            roleRepository.save(adminRole);

            Role userRole = new Role();
            userRole.setName(ERole.ROLE_USER);
            roleRepository.save(userRole);

            // Création d'un admin
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("Azerty@2025"));

            Set<Role> adminRoles = new HashSet<>();
            adminRoles.add(adminRole);
            admin.setRoles(adminRoles);

            userRepository.save(admin);

            // Création d'un user
            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("Azerty@2025"));

            Set<Role> userRoles = new HashSet<>();
            userRoles.add(userRole);
            user.setRoles(userRoles);

            userRepository.save(user);
        };
    }*/

}
