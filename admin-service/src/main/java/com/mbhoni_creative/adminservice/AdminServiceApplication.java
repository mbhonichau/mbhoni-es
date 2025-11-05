package com.mbhoni_creative.adminservice;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class AdminServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner createAdmin(UserRepository userRepo, PasswordEncoder encoder) {
        return args -> {
            userRepo.deleteAll();
            User admin = new User();
            admin.setUsername("mbhoni_admin");
            admin.setPassword(encoder.encode("secure_password"));
            admin.setRoles("ADMIN");
            userRepo.save(admin);
            System.out.println("=== ADMIN-SERVICE: ADMIN USER CREATED ===");
        };
    }

    @Bean
    CommandLineRunner seedMetrics(TenantMetricRepository repo) {
        return args -> {
            if (repo.findById("tenant1").isEmpty()) {
                TenantMetric m = new TenantMetric();
                m.setTenantId("tenant1");
                m.setUsage("150MB");
                m.setTimestamp(LocalDateTime.now());
                repo.save(m);
                System.out.println("Seeded tenant1 metrics");
            }
        };
    }
}