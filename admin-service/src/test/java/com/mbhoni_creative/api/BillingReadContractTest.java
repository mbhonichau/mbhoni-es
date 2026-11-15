package com.mbhoni_creative.api;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.api.config.*;
import com.mbhoni_creative.api.security.*;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

class BillingReadContractTest {
    @Test void billingNamesAreIdempotentlyRegisteredWithoutGrantingWriteScope() {
        var jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:billing-permissions;MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("CREATE TABLE permissions(id BIGINT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(100) UNIQUE,description VARCHAR(255))");
        var workforce=new WorkforcePermissionInitializer(jdbc);var billing=new BillingPermissionInitializer(jdbc);
        workforce.run(null);billing.run(null);workforce.run(null);billing.run(null);
        assertEquals(5,jdbc.queryForObject("SELECT COUNT(*) FROM permissions",Integer.class));
        var read=new Permission();read.setName("API_BILLING_READ");
        var write=new Permission();write.setName("API_BILLING_WRITE");
        assertEquals(Set.of("billing:read"),ApiScopes.from(Set.of(read)));
        assertEquals(Set.of("billing:write"),ApiScopes.from(Set.of(write)));
        var key=new TenantApiKey();key.setPermissions(Set.of(read,write));assertTrue(WorkforcePermissions.v1Only(key));
        var old=new Permission();old.setName("BILLING_VIEW");key.setPermissions(Set.of(read,old));assertFalse(WorkforcePermissions.v1Only(key));
        assertTrue(ApiScopes.from(Set.of(old)).isEmpty());
    }
    @Test void existingRoundingDiscrepanciesAreRecordedWithoutChangingTotals() {
        int[] counts={2,100,3};String[] quantities={"1.00","1.00","0.33"};String[] prices={"0.03","0.03","0.05"};
        String[] totals={"0.07","3.45","0.06"};String[] sums={"0.06","3.00","0.06"};
        for(int c=0;c<counts.length;c++) {
            var invoice=new TenantInvoice();var quotation=new TenantQuotation();
            for(int i=0;i<counts[c];i++) {
                var a=new TenantInvoiceItem();a.setQuantity(new BigDecimal(quantities[c]));a.setUnitPrice(new BigDecimal(prices[c]));invoice.addItem(a);
                var b=new TenantQuotationItem();b.setQuantity(new BigDecimal(quantities[c]));b.setUnitPrice(new BigDecimal(prices[c]));quotation.addItem(b);
            }
            var sum=invoice.getItems().stream().map(TenantInvoiceItem::getTotalAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
            assertEquals(new BigDecimal(totals[c]),invoice.getTotalAmount());assertEquals(invoice.getTotalAmount(),quotation.getTotalAmount());
            assertEquals(new BigDecimal(sums[c]),sum);
            System.out.println("EXISTING ROUNDING: "+counts[c]+" lines of "+quantities[c]+" x "+prices[c]+": line sum="+sum+", document="+invoice.getTotalAmount());
        }
    }
}
