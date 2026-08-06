package com.mbhoni_creative.adminservice.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import com.mbhoni_creative.adminentity.Payslip;
import com.mbhoni_creative.adminservice.PayslipWordService;

@Service
public class PayslipWordServiceImpl implements PayslipWordService {

    @Override
    public byte[] generateSampleWordTemplate() {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Title Banner Paragraph
            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText("${tenantName} - SALARY PAYSLIP");
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setColor("1B1E15");

            // Subtitle
            XWPFParagraph subPara = doc.createParagraph();
            subPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun subRun = subPara.createRun();
            subRun.setText("Pay Period: ${payPeriod} | Payment Date: ${payDate}");
            subRun.setItalic(true);
            subRun.setFontSize(10);
            subRun.setColor("6C757D");

            // Spacing
            doc.createParagraph();

            // Employee Summary Table
            XWPFTable empTable = doc.createTable(3, 2);
            empTable.setWidth("100%");

            setCellContent(empTable.getRow(0).getCell(0), "Employee Name:", "${employeeName}", true);
            setCellContent(empTable.getRow(0).getCell(1), "Employee Number:", "${employeeNumber}", true);

            setCellContent(empTable.getRow(1).getCell(0), "Job Title:", "${jobTitle}", false);
            setCellContent(empTable.getRow(1).getCell(1), "Payslip Ref #:", "${payslipNumber}", false);

            setCellContent(empTable.getRow(2).getCell(0), "Bank Name:", "${bankName}", false);
            setCellContent(empTable.getRow(2).getCell(1), "Account Number:", "${accountNumber}", false);

            doc.createParagraph();

            // Financial Summary Table
            XWPFTable finTable = doc.createTable(5, 4);
            finTable.setWidth("100%");

            // Header Row
            setHeaderCell(finTable.getRow(0).getCell(0), "GROSS EARNINGS");
            setHeaderCell(finTable.getRow(0).getCell(1), "AMOUNT");
            setHeaderCell(finTable.getRow(0).getCell(2), "DEDUCTIONS & TAXES");
            setHeaderCell(finTable.getRow(0).getCell(3), "AMOUNT");

            // Item Rows
            setFinCell(finTable.getRow(1), 0, "Basic Salary", 1, "${basicSalary}", 2, "PAYE Income Tax (18%)", 3, "${taxPaye}");
            setFinCell(finTable.getRow(2), 0, "Allowances", 1, "${allowances}", 2, "UIF Contribution (1%)", 3, "${uifDeduction}");
            setFinCell(finTable.getRow(3), 0, "Overtime", 1, "${overtime}", 2, "Other Deductions", 3, "${otherDeductions}");

            // Totals Row
            setFinCellBold(finTable.getRow(4), 0, "TOTAL GROSS EARNINGS", 1, "${grossPay}", 2, "TOTAL DEDUCTIONS", 3, "${totalDeductions}");

            doc.createParagraph();

            // Net Take Home Pay Banner Paragraph
            XWPFParagraph netPara = doc.createParagraph();
            netPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun netRun1 = netPara.createRun();
            netRun1.setText("TOTAL NET REMUNERATION: ");
            netRun1.setBold(true);
            netRun1.setFontSize(14);

            XWPFRun netRun2 = netPara.createRun();
            netRun2.setText("${netPay}");
            netRun2.setBold(true);
            netRun2.setFontSize(16);
            netRun2.setColor("2E7D32");

            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating sample Word template", e);
        }
    }

    @Override
    public String renderPayslipFromWord(byte[] wordBytes, Payslip payslip) {
        if (wordBytes == null || wordBytes.length == 0 || payslip == null) {
            return null;
        }

        try (ByteArrayInputStream in = new ByteArrayInputStream(wordBytes);
             XWPFDocument doc = new XWPFDocument(in)) {

            Map<String, String> replacements = buildReplacements(payslip);

            // 1. Substitute placeholders in all paragraphs
            for (XWPFParagraph p : doc.getParagraphs()) {
                replaceInParagraph(p, replacements);
            }

            // 2. Substitute placeholders in all tables
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph p : cell.getParagraphs()) {
                            replaceInParagraph(p, replacements);
                        }
                    }
                }
            }

            // 3. Convert Word document elements to HTML
            StringBuilder html = new StringBuilder();
            html.append("<div class=\"word-payslip-wrapper p-4 bg-white rounded shadow-sm border mx-auto\" style=\"max-width: 860px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\">");

            for (IBodyElement elem : doc.getBodyElements()) {
                if (elem instanceof XWPFParagraph p) {
                    html.append(renderParagraphToHtml(p));
                } else if (elem instanceof XWPFTable table) {
                    html.append(renderTableToHtml(table));
                }
            }

            html.append("</div>");
            return html.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void replaceInParagraph(XWPFParagraph p, Map<String, String> replacements) {
        if (p == null) return;
        String pText = p.getText();
        if (pText == null || !pText.contains("${")) return;

        // Substitute placeholders across the paragraph's complete text
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            if (pText.contains(entry.getKey())) {
                pText = pText.replace(entry.getKey(), entry.getValue());
            }
        }

        List<XWPFRun> runs = p.getRuns();
        if (runs == null || runs.isEmpty()) return;

        // Preserve formatting from the first text run
        XWPFRun firstRun = runs.get(0);
        boolean bold = firstRun.isBold();
        boolean italic = firstRun.isItalic();
        int fontSize = firstRun.getFontSize();
        String color = firstRun.getColor();

        // Clear text from text runs while preserving picture runs
        for (int i = 0; i < runs.size(); i++) {
            XWPFRun r = runs.get(i);
            if (r.getEmbeddedPictures() == null || r.getEmbeddedPictures().isEmpty()) {
                try {
                    r.setText("", 0);
                } catch (Exception ignored) {}
            }
        }

        // Set the fully substituted text on the first run
        firstRun.setText(pText, 0);
        if (bold) firstRun.setBold(true);
        if (italic) firstRun.setItalic(true);
        if (fontSize > 0) firstRun.setFontSize(fontSize);
        if (color != null) firstRun.setColor(color);
    }

    private String renderParagraphToHtml(XWPFParagraph p) {
        if (p == null) return "";

        StringBuilder sb = new StringBuilder();
        String align = switch (p.getAlignment()) {
            case CENTER -> "center";
            case RIGHT -> "right";
            default -> "left";
        };

        sb.append("<p style=\"text-align: ").append(align).append("; margin-bottom: 8px;\">");

        boolean hasContent = false;
        for (XWPFRun run : p.getRuns()) {
            // 1. Render embedded pictures in run
            List<XWPFPicture> pictures = run.getEmbeddedPictures();
            if (pictures != null && !pictures.isEmpty()) {
                for (XWPFPicture pic : pictures) {
                    XWPFPictureData picData = pic.getPictureData();
                    if (picData != null && picData.getData() != null) {
                        byte[] bytes = picData.getData();
                        String mime = (picData.getPackagePart() != null) ? picData.getPackagePart().getContentType() : "image/png";
                        String base64 = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
                        sb.append("<img src=\"").append(base64).append("\" style=\"max-height: 80px; max-width: 100%; object-fit: contain; display: inline-block; margin: 4px;\" alt=\"Logo\">");
                        hasContent = true;
                    }
                }
            }

            // 2. Render text content in run
            String runText = run.getText(0);
            if (runText != null && !runText.isBlank()) {
                StringBuilder rStyle = new StringBuilder();
                if (run.isBold()) rStyle.append("font-weight: bold; ");
                if (run.isItalic()) rStyle.append("font-style: italic; ");
                if (run.getFontSize() > 0) rStyle.append("font-size: ").append(run.getFontSize()).append("pt; ");
                if (run.getColor() != null) rStyle.append("color: #").append(run.getColor()).append("; ");

                sb.append("<span style=\"").append(rStyle).append("\">").append(runText).append("</span>");
                hasContent = true;
            }
        }

        if (!hasContent && p.getText() != null && !p.getText().isBlank()) {
            sb.append(p.getText());
        }

        sb.append("</p>");
        return sb.toString();
    }

    private String renderTableToHtml(XWPFTable table) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"table table-bordered align-middle mb-3\" style=\"width: 100%; border-collapse: collapse; margin-bottom: 16px;\">");

        for (XWPFTableRow row : table.getRows()) {
            sb.append("<tr>");
            for (XWPFTableCell cell : row.getTableCells()) {
                String bg = cell.getColor() != null ? ("background-color: #" + cell.getColor() + "; ") : "";
                sb.append("<td style=\"padding: 8px 12px; border: 1px solid #dee2e6; ").append(bg).append("\">");
                for (XWPFParagraph p : cell.getParagraphs()) {
                    sb.append(renderParagraphToHtml(p));
                }
                sb.append("</td>");
            }
            sb.append("</tr>");
        }

        sb.append("</table>");
        return sb.toString();
    }

    private Map<String, String> buildReplacements(Payslip payslip) {
        Map<String, String> map = new HashMap<>();

        String empName = payslip.getEmployee() != null ? (payslip.getEmployee().getFirstName() + " " + payslip.getEmployee().getLastName()) : "Employee";
        String empNum = payslip.getEmployee() != null ? payslip.getEmployee().getEmployeeNumber() : "EMP-001";
        String jobTitle = payslip.getEmployee() != null ? payslip.getEmployee().getJobTitle() : "Specialist";
        String tenantName = payslip.getTenant() != null ? payslip.getTenant().getName() : "Enterprise";

        map.put("${tenantName}", tenantName);
        map.put("${payslipNumber}", payslip.getPayslipNumber() != null ? payslip.getPayslipNumber() : "");
        map.put("${payPeriod}", payslip.getPayPeriod() != null ? payslip.getPayPeriod() : "");
        map.put("${payDate}", payslip.getPayDate() != null ? payslip.getPayDate().toString() : "");
        map.put("${employeeName}", empName);
        map.put("${employeeNumber}", empNum);
        map.put("${jobTitle}", jobTitle);
        map.put("${basicSalary}", formatMoney(payslip.getBasicSalary()));
        map.put("${allowances}", formatMoney(payslip.getAllowances()));
        map.put("${overtime}", formatMoney(payslip.getOvertime()));
        map.put("${grossPay}", formatMoney(payslip.getGrossPay()));
        map.put("${taxPaye}", formatMoney(payslip.getTaxPaye()));
        map.put("${uifDeduction}", formatMoney(payslip.getUifDeduction()));
        map.put("${otherDeductions}", formatMoney(payslip.getOtherDeductions()));
        map.put("${totalDeductions}", formatMoney(payslip.getTotalDeductions()));
        map.put("${netPay}", formatMoney(payslip.getNetPay()));
        map.put("${bankName}", payslip.getBankName() != null ? payslip.getBankName() : "Commercial Bank");
        map.put("${accountNumber}", payslip.getAccountNumber() != null ? payslip.getAccountNumber() : "**** 0000");

        return map;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "R 0.00";
        return String.format("R %,.2f", amount);
    }

    private void setCellContent(XWPFTableCell cell, String label, String value, boolean bold) {
        XWPFParagraph p = cell.getParagraphs().get(0);
        XWPFRun r1 = p.createRun();
        r1.setText(label + " ");
        r1.setBold(true);

        XWPFRun r2 = p.createRun();
        r2.setText(value);
        r2.setBold(bold);
    }

    private void setHeaderCell(XWPFTableCell cell, String title) {
        cell.setColor("4361EE");
        XWPFParagraph p = cell.getParagraphs().get(0);
        XWPFRun r = p.createRun();
        r.setText(title);
        r.setBold(true);
        r.setColor("FFFFFF");
    }

    private void setFinCell(XWPFTableRow row, int c1, String l1, int c2, String v1, int c3, String l2, int c4, String v2) {
        setCellContent(row.getCell(c1), l1, "", false);
        setCellContent(row.getCell(c2), "", v1, false);
        setCellContent(row.getCell(c3), l2, "", false);
        setCellContent(row.getCell(c4), "", v2, false);
    }

    private void setFinCellBold(XWPFTableRow row, int c1, String l1, int c2, String v1, int c3, String l2, int c4, String v2) {
        setCellContent(row.getCell(c1), l1, "", true);
        setCellContent(row.getCell(c2), "", v1, true);
        setCellContent(row.getCell(c3), l2, "", true);
        setCellContent(row.getCell(c4), "", v2, true);
    }
}
