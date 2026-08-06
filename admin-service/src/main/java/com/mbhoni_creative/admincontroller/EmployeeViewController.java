package com.mbhoni_creative.admincontroller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mbhoni_creative.admindto.TenantDto;
import com.mbhoni_creative.adminentity.Employee;
import com.mbhoni_creative.adminentity.EmployeeStatus;
import com.mbhoni_creative.adminentity.Payslip;
import com.mbhoni_creative.adminrepository.PayslipRepository;
import com.mbhoni_creative.adminservice.EmployeeService;
import com.mbhoni_creative.adminservice.OrganizationUnitService;
import com.mbhoni_creative.adminservice.TenantService;
import com.mbhoni_creative.config.TenantSecurityService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.mbhoni_creative.adminentity.TenantCustomization;
import com.mbhoni_creative.adminservice.TenantCustomizationService;
import com.mbhoni_creative.adminservice.PayslipExcelService;
import com.mbhoni_creative.adminservice.PayslipWordService;

@Controller
@RequestMapping("/employees")
public class EmployeeViewController {

    private final EmployeeService employeeService;
    private final OrganizationUnitService orgUnitService;
    private final TenantService tenantService;
    private final TenantSecurityService tenantSecurityService;
    private final PayslipRepository payslipRepository;
    private final TenantCustomizationService customizationService;
    private final PayslipExcelService excelService;
    private final PayslipWordService wordService;

    public EmployeeViewController(
            EmployeeService employeeService,
            OrganizationUnitService orgUnitService,
            TenantService tenantService,
            TenantSecurityService tenantSecurityService,
            PayslipRepository payslipRepository,
            TenantCustomizationService customizationService,
            PayslipExcelService excelService,
            PayslipWordService wordService) {
        this.employeeService = employeeService;
        this.orgUnitService = orgUnitService;
        this.tenantService = tenantService;
        this.tenantSecurityService = tenantSecurityService;
        this.payslipRepository = payslipRepository;
        this.customizationService = customizationService;
        this.excelService = excelService;
        this.wordService = wordService;
    }

    @GetMapping
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('EMPLOYEE') and (hasAuthority('EMPLOYEE_VIEW') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_VIEW'))")
    public String index(Model model) {
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        model.addAttribute("employees", employeeService.getEmployeesByTenant(tenantId));
        model.addAttribute("tenants", getAccessibleTenants());
        model.addAttribute("statuses", EmployeeStatus.values());

        Long effectiveTenantId = tenantId != null ? tenantId : (tenantService.getAllTenants().isEmpty() ? null : tenantService.getAllTenants().get(0).getId());
        if (effectiveTenantId != null) {
            model.addAttribute("orgUnits", orgUnitService.getOrganizationUnitsByTenant(effectiveTenantId));
            model.addAttribute("managers", employeeService.getEmployeesByTenant(effectiveTenantId));
        }

        model.addAttribute("newEmployee", new Employee());
        return "employees/list";
    }

    @PostMapping("/save")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('EMPLOYEE') and (hasAuthority('EMPLOYEE_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_VIEW'))")
    public String saveEmployee(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Long managerId,
            @ModelAttribute Employee employee) {
        Long targetTenantId = tenantId != null ? tenantId : tenantSecurityService.getCurrentTenantId();
        
        if (!tenantSecurityService.isGlobalAdmin()) {
            Long currentTenantId = tenantSecurityService.getCurrentTenantId();
            if (currentTenantId != null) {
                targetTenantId = currentTenantId;
            }
        }
        
        employeeService.saveEmployee(targetTenantId, orgUnitId, managerId, employee);
        return "redirect:/employees";
    }

    @PostMapping("/status")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('EMPLOYEE') and (hasAuthority('EMPLOYEE_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_VIEW'))")
    public String updateStatus(
            @RequestParam Long id,
            @RequestParam EmployeeStatus status) {
        
        Employee employee = employeeService.getEmployeeById(id);
        if (!tenantSecurityService.isGlobalAdmin()) {
            Long currentTenantId = tenantSecurityService.getCurrentTenantId();
            if (employee.getTenant() == null || !employee.getTenant().getId().equals(currentTenantId)) {
                throw new RuntimeException("Access denied");
            }
        }

        employeeService.updateEmployeeStatus(id, status);
        return "redirect:/employees";
    }

    private List<TenantDto> getAccessibleTenants() {
        if (tenantSecurityService.isGlobalAdmin()) {
            return tenantService.getAllTenants();
        }
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        if (tenantId == null) {
            return List.of();
        }
        try {
            return List.of(tenantService.getTenantById(tenantId));
        } catch (Exception e) {
            return List.of();
        }
    }

    // =====================================================
    // EMPLOYEE PAYSLIP MANAGEMENT & PRINTABLE GENERATOR
    // =====================================================

    @GetMapping("/payslips")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('EMPLOYEE') and (hasAuthority('EMPLOYEE_VIEW') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN'))")
    public String listPayslips(Model model) {
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        List<Payslip> payslips;

        if (tenantSecurityService.isGlobalAdmin() && tenantId == null) {
            payslips = payslipRepository.findAll();
        } else if (tenantId != null) {
            payslips = payslipRepository.findByTenantIdOrderByPayDateDesc(tenantId);
        } else {
            payslips = List.of();
        }

        Long effectiveTenantId = tenantId != null ? tenantId : (tenantService.getAllTenants().isEmpty() ? null : tenantService.getAllTenants().get(0).getId());
        List<Employee> employees = effectiveTenantId != null ? employeeService.getEmployeesByTenant(effectiveTenantId) : List.of();

        model.addAttribute("payslips", payslips);
        model.addAttribute("employees", employees);
        model.addAttribute("newPayslip", new Payslip());
        return "employees/payslips";
    }

    @PostMapping("/payslips/generate")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('EMPLOYEE') and (hasAuthority('EMPLOYEE_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN'))")
    public String generatePayslip(
            @ModelAttribute Payslip payslip,
            @RequestParam Long employeeId,
            RedirectAttributes redirectAttributes) {

        Employee emp = employeeService.getEmployeeById(employeeId);
        payslip.setEmployee(emp);
        if (emp != null && emp.getTenant() != null) {
            payslip.setTenant(emp.getTenant());
        }

        if (payslip.getPayslipNumber() == null || payslip.getPayslipNumber().isBlank()) {
            payslip.setPayslipNumber("PAY-" + System.currentTimeMillis() % 1000000);
        }

        if (payslip.getPayDate() == null) {
            payslip.setPayDate(LocalDate.now());
        }

        payslip.calculateTotals();
        payslipRepository.save(payslip);

        redirectAttributes.addFlashAttribute("successMessage", "Employee payslip generated successfully.");
        return "redirect:/employees/payslips";
    }

    @GetMapping("/payslips/{id}/print")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('EMPLOYEE') and (hasAuthority('EMPLOYEE_VIEW') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN'))")
    public String printPayslip(@PathVariable Long id, Model model) {
        Payslip payslip = payslipRepository.findById(id).orElse(null);
        model.addAttribute("payslip", payslip);

        if (payslip != null && payslip.getTenant() != null) {
            TenantCustomization customization = customizationService.getOrCreateForTenant(payslip.getTenant().getId());
            String engineType = customization != null ? customization.getPayslipEngineType() : "EXCEL";

            String customHtml;
            if ("WORD".equalsIgnoreCase(engineType)) {
                byte[] wordBytes = (customization != null && customization.getPayslipWordTemplate() != null && customization.getPayslipWordTemplate().length > 0)
                        ? customization.getPayslipWordTemplate()
                        : wordService.generateSampleWordTemplate();
                customHtml = wordService.renderPayslipFromWord(wordBytes, payslip);
            } else {
                byte[] excelBytes = (customization != null && customization.getPayslipExcelTemplate() != null && customization.getPayslipExcelTemplate().length > 0)
                        ? customization.getPayslipExcelTemplate()
                        : excelService.generateSampleExcelTemplate();
                customHtml = excelService.renderPayslipFromExcel(excelBytes, payslip);
            }

            model.addAttribute("customPayslipHtml", customHtml);
        }

        return "employees/payslip-print";
    }

    // =====================================================
    // EXCEL & WORD TEMPLATE STUDIOS & SAMPLE DOWNLOADS
    // =====================================================

    @GetMapping("/payslips/excel/sample")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('EMPLOYEE') and (hasAuthority('EMPLOYEE_VIEW') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN'))")
    public ResponseEntity<byte[]> downloadSampleExcelTemplate() {
        byte[] bytes = excelService.generateSampleExcelTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Payslip_Sample_Template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/payslips/word/sample")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('EMPLOYEE') and (hasAuthority('EMPLOYEE_VIEW') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN'))")
    public ResponseEntity<byte[]> downloadSampleWordTemplate() {
        byte[] bytes = wordService.generateSampleWordTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Payslip_Sample_Template.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(bytes);
    }

    @GetMapping("/payslips/builder")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('EMPLOYEE') and (hasAuthority('EMPLOYEE_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN'))")
    public String payslipBuilder(Model model) {
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        Long effectiveTenantId = tenantId != null ? tenantId : (tenantService.getAllTenants().isEmpty() ? null : tenantService.getAllTenants().get(0).getId());
        
        TenantCustomization customization = effectiveTenantId != null ? customizationService.getOrCreateForTenant(effectiveTenantId) : null;
        String engineType = customization != null ? customization.getPayslipEngineType() : "EXCEL";
        String uploadedExcelFileName = customization != null ? customization.getPayslipExcelFileName() : null;
        String uploadedWordFileName = customization != null ? customization.getPayslipWordFileName() : null;
        boolean hasCustomExcel = customization != null && customization.getPayslipExcelTemplate() != null && customization.getPayslipExcelTemplate().length > 0;
        boolean hasCustomWord = customization != null && customization.getPayslipWordTemplate() != null && customization.getPayslipWordTemplate().length > 0;

        model.addAttribute("tenantId", effectiveTenantId);
        model.addAttribute("engineType", engineType);
        model.addAttribute("uploadedExcelFileName", uploadedExcelFileName);
        model.addAttribute("uploadedWordFileName", uploadedWordFileName);
        model.addAttribute("hasCustomExcel", hasCustomExcel);
        model.addAttribute("hasCustomWord", hasCustomWord);
        return "employees/payslip-builder";
    }

    @PostMapping("/payslips/builder/save")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('EMPLOYEE') and (hasAuthority('EMPLOYEE_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN'))")
    public String savePayslipBuilder(
            @RequestParam(name = "engineType", defaultValue = "EXCEL") String engineType,
            @RequestParam(name = "excelFile", required = false) MultipartFile excelFile,
            @RequestParam(name = "wordFile", required = false) MultipartFile wordFile,
            RedirectAttributes redirectAttributes) {

        Long tenantId = tenantSecurityService.getCurrentTenantId();
        if (tenantId == null && !tenantService.getAllTenants().isEmpty()) {
            tenantId = tenantService.getAllTenants().get(0).getId();
        }

        if (tenantId != null) {
            try {
                TenantCustomization customization = customizationService.getOrCreateForTenant(tenantId);
                customization.setPayslipEngineType(engineType);

                if ("EXCEL".equalsIgnoreCase(engineType) && excelFile != null && !excelFile.isEmpty()) {
                    customization.setPayslipExcelTemplate(excelFile.getBytes());
                    customization.setPayslipExcelFileName(excelFile.getOriginalFilename());
                    redirectAttributes.addFlashAttribute("successMessage", "Custom Excel template (" + excelFile.getOriginalFilename() + ") uploaded & set as active engine!");
                } else if ("WORD".equalsIgnoreCase(engineType) && wordFile != null && !wordFile.isEmpty()) {
                    customization.setPayslipWordTemplate(wordFile.getBytes());
                    customization.setPayslipWordFileName(wordFile.getOriginalFilename());
                    redirectAttributes.addFlashAttribute("successMessage", "Custom Word template (" + wordFile.getOriginalFilename() + ") uploaded & set as active engine!");
                } else {
                    redirectAttributes.addFlashAttribute("successMessage", "Active payslip engine switched to " + engineType + "!");
                }

                customizationService.saveForTenant(tenantId, customization);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to process template file: " + e.getMessage());
            }
        }

        return "redirect:/employees/payslips/builder";
    }
}
