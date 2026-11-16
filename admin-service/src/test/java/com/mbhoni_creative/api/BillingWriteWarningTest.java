package com.mbhoni_creative.api;

import com.mbhoni_creative.api.config.BillingWriteSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class BillingWriteWarningTest {
    @Test void deploymentPropertyDefaultsOffAndCanBeEnabledExplicitly() {
        var runner=new org.springframework.boot.test.context.runner.ApplicationContextRunner().withUserConfiguration(BillingWriteSettings.class);
        runner.run(context->assertFalse(context.getBean(BillingWriteSettings.class).enabled()));
        runner.withPropertyValues("api.billing.writes.enabled=true").run(context->assertTrue(context.getBean(BillingWriteSettings.class).enabled()));
    }
    @Test void enabledWritesWarnWithoutBlockingAndDisabledWritesAreQuiet(CapturedOutput output) {
        new BillingWriteSettings(false).run(null);assertFalse(output.getOut().contains("API billing writes enabled"));
        new BillingWriteSettings(true).run(null);
        assertTrue(output.getOut().contains("API drafts are counted in UI aggregates, appear in the owner export, and advance UI document numbering"));
    }
}
