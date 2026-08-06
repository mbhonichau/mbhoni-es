package com.mbhoni_creative.adminservice;

import com.mbhoni_creative.adminentity.Payslip;

public interface PayslipWordService {

    /**
     * Generates a default, pre-formatted sample Microsoft Word payslip (.docx) template.
     */
    byte[] generateSampleWordTemplate();

    /**
     * Reads an uploaded Word template (.docx), substitutes placeholders with employee payslip figures,
     * and renders a clean printable HTML view.
     */
    String renderPayslipFromWord(byte[] wordBytes, Payslip payslip);
}
