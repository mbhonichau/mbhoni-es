package com.mbhoni_creative.adminentity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;

@Entity
@Table(name = "payslips")
public class Payslip extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "payslip_number", nullable = false, unique = true, length = 80)
    private String payslipNumber;

    @Column(name = "pay_period", nullable = false, length = 80)
    private String payPeriod;

    @Column(name = "pay_date", nullable = false)
    private LocalDate payDate;

    @Column(name = "basic_salary", nullable = false)
    private BigDecimal basicSalary = BigDecimal.ZERO;

    @Column(name = "allowances")
    private BigDecimal allowances = BigDecimal.ZERO;

    @Column(name = "overtime")
    private BigDecimal overtime = BigDecimal.ZERO;

    @Column(name = "gross_pay", nullable = false)
    private BigDecimal grossPay = BigDecimal.ZERO;

    @Column(name = "tax_paye")
    private BigDecimal taxPaye = BigDecimal.ZERO;

    @Column(name = "uif_deduction")
    private BigDecimal uifDeduction = BigDecimal.ZERO;

    @Column(name = "other_deductions")
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    @Column(name = "total_deductions", nullable = false)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(name = "net_pay", nullable = false)
    private BigDecimal netPay = BigDecimal.ZERO;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_number", length = 60)
    private String accountNumber;

    @Column(length = 40)
    private String status = "PAID";

    public Long getId() { return id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public String getPayslipNumber() { return payslipNumber; }
    public void setPayslipNumber(String payslipNumber) { this.payslipNumber = payslipNumber; }

    public String getPayPeriod() { return payPeriod; }
    public void setPayPeriod(String payPeriod) { this.payPeriod = payPeriod; }

    public LocalDate getPayDate() { return payDate; }
    public void setPayDate(LocalDate payDate) { this.payDate = payDate; }

    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary != null ? basicSalary : BigDecimal.ZERO; }

    public BigDecimal getAllowances() { return allowances; }
    public void setAllowances(BigDecimal allowances) { this.allowances = allowances != null ? allowances : BigDecimal.ZERO; }

    public BigDecimal getOvertime() { return overtime; }
    public void setOvertime(BigDecimal overtime) { this.overtime = overtime != null ? overtime : BigDecimal.ZERO; }

    public BigDecimal getGrossPay() { return grossPay; }
    public void setGrossPay(BigDecimal grossPay) { this.grossPay = grossPay != null ? grossPay : BigDecimal.ZERO; }

    public BigDecimal getTaxPaye() { return taxPaye; }
    public void setTaxPaye(BigDecimal taxPaye) { this.taxPaye = taxPaye != null ? taxPaye : BigDecimal.ZERO; }

    public BigDecimal getUifDeduction() { return uifDeduction; }
    public void setUifDeduction(BigDecimal uifDeduction) { this.uifDeduction = uifDeduction != null ? uifDeduction : BigDecimal.ZERO; }

    public BigDecimal getOtherDeductions() { return otherDeductions; }
    public void setOtherDeductions(BigDecimal otherDeductions) { this.otherDeductions = otherDeductions != null ? otherDeductions : BigDecimal.ZERO; }

    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions != null ? totalDeductions : BigDecimal.ZERO; }

    public BigDecimal getNetPay() { return netPay; }
    public void setNetPay(BigDecimal netPay) { this.netPay = netPay != null ? netPay : BigDecimal.ZERO; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public void calculateTotals() {
        this.grossPay = this.basicSalary.add(this.allowances).add(this.overtime);
        this.totalDeductions = this.taxPaye.add(this.uifDeduction).add(this.otherDeductions);
        this.netPay = this.grossPay.subtract(this.totalDeductions);
    }
}
