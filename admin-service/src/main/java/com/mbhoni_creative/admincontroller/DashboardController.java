package com.mbhoni_creative.admincontroller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminrepository.PlatformModuleRepository;
import com.mbhoni_creative.adminrepository.SubscriptionPlanRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminrepository.UserRepository;
import com.mbhoni_creative.config.TenantSecurityService;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PlatformModuleRepository platformModuleRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final TenantSecurityService tenantSecurityService;

    public DashboardController(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            PlatformModuleRepository platformModuleRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            TenantSecurityService tenantSecurityService) {

        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.platformModuleRepository = platformModuleRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.tenantSecurityService = tenantSecurityService;
    }

    @GetMapping
    public String dashboard(Model model, Authentication authentication) {

        String username = authentication != null ? authentication.getName() : "Guest";
        boolean globalAdmin = tenantSecurityService.isGlobalAdmin();

        model.addAttribute("username", username);
        model.addAttribute("globalAdmin", globalAdmin);
        model.addAttribute("totalModules", platformModuleRepository.count());
        model.addAttribute("totalPlans", subscriptionPlanRepository.count());

        if (globalAdmin) {
            model.addAttribute("totalTenants", tenantRepository.count());
            model.addAttribute("activeTenants", tenantRepository.countByActive(true));
            model.addAttribute("totalUsers", userRepository.count());
        } else {
            Long tenantId = tenantSecurityService.getCurrentTenantId();

            Tenant tenant = tenantId != null
                    ? tenantRepository.findById(tenantId).orElse(null)
                    : null;

            model.addAttribute("tenantName", tenant != null ? tenant.getName() : "Assigned Workspace");
            model.addAttribute("tenantUsers", tenant != null ? userRepository.countByTenant(tenant) : 0);
        }

        return "dashboard";
    }
}