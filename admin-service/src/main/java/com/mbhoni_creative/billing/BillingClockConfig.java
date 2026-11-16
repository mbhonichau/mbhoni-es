package com.mbhoni_creative.billing;
import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
@Configuration
@EnableScheduling
public class BillingClockConfig {
    @Bean public Clock billingClock() { return Clock.systemUTC(); }
}
