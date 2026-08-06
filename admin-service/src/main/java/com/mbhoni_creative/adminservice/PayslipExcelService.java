package com.mbhoni_creative.adminservice;

import com.mbhoni_creative.adminentity.Payslip;

public interface PayslipExcelService {

    /**
     * Generates a default, pre-formatted sample Excel payslip (.xlsx) template.
     */
    byte[] generateSampleExcelTemplate();

    /**
     * Reads an uploaded Excel template (.xlsx), substitutes placeholders with employee payslip figures,
     * evaluates formulas, and renders a clean printable HTML view.
     */
    String renderPayslipFromExcel(byte[] excelBytes, Payslip payslip);
}
