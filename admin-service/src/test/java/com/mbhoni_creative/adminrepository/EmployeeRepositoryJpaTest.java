package com.mbhoni_creative.adminrepository;

import com.mbhoni_creative.adminentity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import static org.junit.jupiter.api.Assertions.*;

// Real repository initialization catches derived-query errors that Mockito cannot.
// Flyway is tested separately against MySQL; this slice uses an isolated embedded DB.
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // VALUE is an H2 keyword, but is a valid existing MySQL column name.
        "spring.datasource.url=jdbc:h2:mem:employee-repository-test;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
}, showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EmployeeRepositoryJpaTest {
    @Autowired TestEntityManager entities;
    @Autowired EmployeeRepository employees;

    @Test void managerAndUserQueriesTraverseMappedRelationshipsDespiteJsonIdGetters() {
        Tenant tenant = new Tenant(); tenant.setName("Repository test"); entities.persist(tenant);
        User user = new User(); user.setTenant(tenant); user.setUsername("member");
        user.setEmail("member@example.com"); user.setPassword("test-only-hash"); entities.persist(user);
        Employee manager = employee(tenant, "MANAGER"); entities.persist(manager);
        Employee report = employee(tenant, "REPORT"); report.setManager(manager); report.setUser(user); entities.persist(report);
        Employee unrelated = employee(tenant, "OTHER"); entities.persist(unrelated);
        entities.flush();
        Long managerId = manager.getId(), userId = user.getId(), reportId = report.getId();
        entities.clear();

        var reports = employees.findByManagerId(managerId);
        assertEquals(1, reports.size()); assertEquals(reportId, reports.get(0).getId());
        assertEquals(managerId, reports.get(0).getManagerId());
        assertEquals(reportId, employees.findByUserId(userId).orElseThrow().getId());
        assertTrue(employees.findByManagerId(Long.MAX_VALUE).isEmpty());
        assertTrue(employees.findByUserId(Long.MAX_VALUE).isEmpty());
    }

    private Employee employee(Tenant tenant, String number) {
        Employee employee = new Employee(); employee.setTenant(tenant); employee.setEmployeeNumber(number);
        employee.setFirstName("Test"); employee.setLastName(number); employee.setEmail(number.toLowerCase() + "@example.com");
        return employee;
    }
}
