package com.mbhoni_creative.contentmanagementservice;

    import org.springframework.boot.CommandLineRunner;
    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

    @SpringBootApplication
    @EnableFeignClients(basePackages = "com.mbhoni_creative.contentmanagementservice")
    public class ContentManagementServiceApplication {

        public static void main(String[] args) {
            SpringApplication.run(ContentManagementServiceApplication.class, args);
        }

     // Add these @Beans
        @Bean
        CommandLineRunner createAdmin(UserRepository userRepo, PasswordEncoder encoder) {
            return args -> {
                userRepo.deleteAll(); // Fresh start
                User admin = new User();
                admin.setUsername("mbhoni_admin");
                admin.setPassword(encoder.encode("secure_password"));
                admin.setRoles("ADMIN");
                userRepo.save(admin);
                System.out.println("=== ADMIN USER CREATED ===");
            };
        }
        
        @Bean
        CommandLineRunner seedTenants(TenantContentRepository repo) {
            return args -> {
                if (repo.findById("tenant1").isEmpty()) {
                    TenantContent t = new TenantContent();
                    t.setTenantId("tenant1");
                    t.setAbout("Welcome to Tenant 1");
                    t.setMedia("https://picsum.photos/800/600?random=1");
                    t.setProducts("Gold, Silver, Platinum");
                    repo.save(t);
                    System.out.println("FULLY SEEDED tenant1");
                }
            };
        }
        
        
    }