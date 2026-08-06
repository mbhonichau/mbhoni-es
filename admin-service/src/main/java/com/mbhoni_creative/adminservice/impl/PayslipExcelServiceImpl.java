package com.mbhoni_creative.adminservice.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import com.mbhoni_creative.adminentity.Payslip;
import com.mbhoni_creative.adminservice.PayslipExcelService;

@Service
public class PayslipExcelServiceImpl implements PayslipExcelService {

    @Override
    public byte[] generateSampleExcelTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("Payslip Template");
            sheet.setDisplayGridlines(true);

            // Set column widths (characters * 256)
            sheet.setColumnWidth(0, 8000);
            sheet.setColumnWidth(1, 8000);
            sheet.setColumnWidth(2, 8000);
            sheet.setColumnWidth(3, 8000);

            // Title Style (Dark Background, White Bold Text, Center Alignment)
            XSSFCellStyle titleStyle = workbook.createCellStyle();
            XSSFFont titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            titleStyle.setFont(titleFont);
            titleStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 27, (byte) 30, (byte) 21}, null));
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Section Header Style (Blue Accent Fill, White Bold Text)
            XSSFCellStyle sectionStyle = workbook.createCellStyle();
            XSSFFont sectionFont = workbook.createFont();
            sectionFont.setBold(true);
            sectionFont.setFontHeightInPoints((short) 11);
            sectionFont.setColor(IndexedColors.WHITE.getIndex());
            sectionStyle.setFont(sectionFont);
            sectionStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 67, (byte) 97, (byte) 238}, null));
            sectionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sectionStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Bold Label Style
            XSSFCellStyle boldStyle = workbook.createCellStyle();
            XSSFFont boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            // Green Total Accent Style
            XSSFCellStyle totalStyle = workbook.createCellStyle();
            XSSFFont totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalFont.setFontHeightInPoints((short) 14);
            totalFont.setColor(new XSSFColor(new byte[]{(byte) 46, (byte) 125, (byte) 50}, null));
            totalStyle.setFont(totalFont);

            // Row 0: Title Banner Merged Across A1:D1
            Row r0 = sheet.createRow(0);
            r0.setHeightInPoints(40);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("${tenantName} - SALARY PAYSLIP");
            c0.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            // Row 2: Header Info
            Row r2 = sheet.createRow(2);
            r2.setHeightInPoints(24);
            r2.createCell(0).setCellValue("Payslip Reference:");
            r2.createCell(1).setCellValue("${payslipNumber}");
            r2.createCell(2).setCellValue("Pay Period:");
            r2.createCell(3).setCellValue("${payPeriod}");

            Row r3 = sheet.createRow(3);
            r3.setHeightInPoints(24);
            r3.createCell(0).setCellValue("Payment Date:");
            r3.createCell(1).setCellValue("${payDate}");

            // Row 5: Employee Info Section Banner Merged A6:D6
            Row r5 = sheet.createRow(5);
            r5.setHeightInPoints(26);
            Cell c5 = r5.createCell(0);
            c5.setCellValue("EMPLOYEE INFORMATION");
            c5.setCellStyle(sectionStyle);
            sheet.addMergedRegion(new CellRangeAddress(5, 5, 0, 3));

            Row r6 = sheet.createRow(6);
            r6.setHeightInPoints(24);
            r6.createCell(0).setCellValue("Employee Name:");
            r6.createCell(1).setCellValue("${employeeName}");
            r6.createCell(2).setCellValue("Employee Number:");
            r6.createCell(3).setCellValue("${employeeNumber}");

            Row r7 = sheet.createRow(7);
            r7.setHeightInPoints(24);
            r7.createCell(0).setCellValue("Job Title / Designation:");
            r7.createCell(1).setCellValue("${jobTitle}");

            // Row 9: Financial Section Banners
            Row r9 = sheet.createRow(9);
            r9.setHeightInPoints(26);
            Cell c9a = r9.createCell(0);
            c9a.setCellValue("GROSS EARNINGS");
            c9a.setCellStyle(sectionStyle);
            sheet.addMergedRegion(new CellRangeAddress(9, 9, 0, 1));

            Cell c9b = r9.createCell(2);
            c9b.setCellValue("DEDUCTIONS & TAXES");
            c9b.setCellStyle(sectionStyle);
            sheet.addMergedRegion(new CellRangeAddress(9, 9, 2, 3));

            // Financial Rows
            Row r10 = sheet.createRow(10);
            r10.setHeightInPoints(24);
            r10.createCell(0).setCellValue("Basic Salary");
            r10.createCell(1).setCellValue("${basicSalary}");
            r10.createCell(2).setCellValue("PAYE Income Tax (18%)");
            r10.createCell(3).setCellValue("${taxPaye}");

            Row r11 = sheet.createRow(11);
            r11.setHeightInPoints(24);
            r11.createCell(0).setCellValue("Allowances");
            r11.createCell(1).setCellValue("${allowances}");
            r11.createCell(2).setCellValue("UIF Statutory Contribution (1%)");
            r11.createCell(3).setCellValue("${uifDeduction}");

            Row r12 = sheet.createRow(12);
            r12.setHeightInPoints(24);
            r12.createCell(0).setCellValue("Overtime Remuneration");
            r12.createCell(1).setCellValue("${overtime}");
            r12.createCell(2).setCellValue("Other Voluntary Deductions");
            r12.createCell(3).setCellValue("${otherDeductions}");

            Row r13 = sheet.createRow(13);
            r13.setHeightInPoints(26);
            Cell c13a = r13.createCell(0);
            c13a.setCellValue("TOTAL GROSS EARNINGS");
            c13a.setCellStyle(boldStyle);
            r13.createCell(1).setCellValue("${grossPay}");

            Cell c13b = r13.createCell(2);
            c13b.setCellValue("TOTAL DEDUCTIONS");
            c13b.setCellStyle(boldStyle);
            r13.createCell(3).setCellValue("${totalDeductions}");

            // Row 15: Net Take Home Pay Banner
            Row r15 = sheet.createRow(15);
            r15.setHeightInPoints(32);
            Cell c15 = r15.createCell(0);
            c15.setCellValue("TOTAL NET REMUNERATION:");
            c15.setCellStyle(boldStyle);

            Cell c15b = r15.createCell(1);
            c15b.setCellValue("${netPay}");
            c15b.setCellStyle(totalStyle);

            // Row 17: Bank Details
            Row r17 = sheet.createRow(17);
            r17.setHeightInPoints(24);
            r17.createCell(0).setCellValue("Bank Disbursement Details:");
            r17.createCell(1).setCellValue("${bankName} (Acc #: ${accountNumber})");
            sheet.addMergedRegion(new CellRangeAddress(17, 17, 1, 3));

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating sample Excel template", e);
        }
    }

    @Override
    public String renderPayslipFromExcel(byte[] excelBytes, Payslip payslip) {
        if (excelBytes == null || excelBytes.length == 0 || payslip == null) {
            return null;
        }

        try (ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
             Workbook workbook = WorkbookFactory.create(in)) {

            Map<String, String> replacements = buildReplacements(payslip);
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter dataFormatter = new DataFormatter();

            // 1. Substitute Placeholders in all Cells
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.STRING) {
                        String text = cell.getStringCellValue();
                        if (text != null && text.contains("${")) {
                            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                                text = text.replace(entry.getKey(), entry.getValue());
                            }
                            cell.setCellValue(text);
                        }
                    }
                }
            }

            // 2. Evaluate Formulas
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            evaluator.evaluateAll();

            // 3. Extract Embedded Pictures / Logos (if present)
            Map<String, String> pictureMap = extractEmbeddedPictures(sheet);

            // 4. Map Merged Cell Regions (colspan & rowspan)
            List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();

            boolean displayGridlines = sheet.isDisplayGridlines();

            // 5. Build Crisp, Faithful HTML Grid Output
            StringBuilder html = new StringBuilder();
            html.append("<div class=\"excel-payslip-wrapper p-4 bg-white rounded shadow-sm border mx-auto\" style=\"max-width: 900px;\">");
            html.append("<table class=\"excel-payslip-table align-middle mb-0\" style=\"border-collapse: collapse; width: 100%; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;\">");

            // Column Width Specs
            html.append("<colgroup>");
            int maxColumns = getSheetMaxColumns(sheet);
            for (int col = 0; col < maxColumns; col++) {
                int poiWidth = sheet.getColumnWidth(col);
                int pixelWidth = Math.max(Math.round(poiWidth / 32f), 120);
                html.append("<col style=\"width: ").append(pixelWidth).append("px;\">");
            }
            html.append("</colgroup>");

            // Render Rows
            for (int rowIdx = 0; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) {
                    html.append("<tr style=\"height: 24px;\"><td colspan=\"").append(maxColumns).append("\" style=\"border: none;\">&nbsp;</td></tr>");
                    continue;
                }

                float rowHeight = row.getHeightInPoints();
                String heightStyle = rowHeight > 0 ? ("height: " + Math.round(rowHeight * 1.33) + "px;") : "height: 28px;";
                html.append("<tr style=\"").append(heightStyle).append("\">");

                for (int colIdx = 0; colIdx < maxColumns; colIdx++) {
                    // Check Merged Regions
                    CellRangeAddress mergeRegion = getMergedRegionAt(mergedRegions, rowIdx, colIdx);
                    if (mergeRegion != null) {
                        // If this is NOT the top-left cell of the merged region, skip rendering
                        if (rowIdx != mergeRegion.getFirstRow() || colIdx != mergeRegion.getFirstColumn()) {
                            continue;
                        }
                    }

                    Cell cell = row.getCell(colIdx);
                    int colspan = mergeRegion != null ? (mergeRegion.getLastColumn() - mergeRegion.getFirstColumn() + 1) : 1;
                    int rowspan = mergeRegion != null ? (mergeRegion.getLastRow() - mergeRegion.getFirstRow() + 1) : 1;

                    String cellKey = rowIdx + "_" + colIdx;
                    String cellContent;
                    if (pictureMap.containsKey(cellKey)) {
                        cellContent = "<img src=\"" + pictureMap.get(cellKey) + "\" style=\"max-height: 60px; max-width: 100%; object-fit: contain;\">";
                    } else {
                        cellContent = getCellValueAsString(cell, evaluator, dataFormatter);
                    }

                    String style = buildCellStyle(cell, displayGridlines);

                    html.append("<td");
                    if (colspan > 1) html.append(" colspan=\"").append(colspan).append("\"");
                    if (rowspan > 1) html.append(" rowspan=\"").append(rowspan).append("\"");
                    html.append(" style=\"").append(style).append("\">")
                        .append(cellContent.isBlank() ? "&nbsp;" : cellContent)
                        .append("</td>");
                }
                html.append("</tr>");
            }

            html.append("</table>");
            html.append("</div>");

            return html.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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

    private int getSheetMaxColumns(Sheet sheet) {
        int max = 4;
        for (Row r : sheet) {
            if (r != null && r.getLastCellNum() > max) {
                max = r.getLastCellNum();
            }
        }
        return Math.min(max, 12);
    }

    private CellRangeAddress getMergedRegionAt(List<CellRangeAddress> regions, int row, int col) {
        if (regions == null) return null;
        for (CellRangeAddress reg : regions) {
            if (reg.isInRange(row, col)) {
                return reg;
            }
        }
        return null;
    }

    private Map<String, String> extractEmbeddedPictures(Sheet sheet) {
        Map<String, String> map = new HashMap<>();
        try {
            Drawing<?> drawing = sheet.getDrawingPatriarch();
            if (drawing instanceof XSSFDrawing xssfDrawing) {
                for (XSSFShape shape : xssfDrawing.getShapes()) {
                    if (shape instanceof XSSFPicture picture) {
                        XSSFClientAnchor anchor = picture.getClientAnchor();
                        XSSFPictureData pictureData = picture.getPictureData();
                        byte[] data = pictureData.getData();
                        String mime = pictureData.getMimeType();
                        String base64 = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(data);
                        String key = anchor.getRow1() + "_" + anchor.getCol1();
                        map.put(key, base64);
                    }
                }
            }
        } catch (Exception ignored) {}
        return map;
    }

    private String getCellValueAsString(Cell cell, FormulaEvaluator evaluator, DataFormatter dataFormatter) {
        if (cell == null) return "";
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            try {
                type = evaluator.evaluate(cell).getCellType();
            } catch (Exception e) {
                return cell.getCellFormula();
            }
        }

        switch (type) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return dataFormatter.formatCellValue(cell, evaluator);
                }
                return dataFormatter.formatCellValue(cell, evaluator);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private String buildCellStyle(Cell cell, boolean displayGridlines) {
        if (cell == null) {
            return displayGridlines ? "padding: 6px 10px; font-size: 13px; border: 1px solid #e9ecef;" : "padding: 6px 10px; font-size: 13px; border: none;";
        }

        StringBuilder sb = new StringBuilder("padding: 6px 10px; font-size: 13px; ");
        CellStyle cs = cell.getCellStyle();

        if (cs != null) {
            // Font Name, Size, Bold, Italic, Color
            Font font = cell.getSheet().getWorkbook().getFontAt(cs.getFontIndex());
            if (font != null) {
                if (font.getBold()) sb.append("font-weight: bold; ");
                if (font.getItalic()) sb.append("font-style: italic; ");
                if (font.getFontHeightInPoints() > 0) {
                    sb.append("font-size: ").append(font.getFontHeightInPoints()).append("pt; ");
                }
                if (font instanceof XSSFFont xssfFont) {
                    XSSFColor fontColor = xssfFont.getXSSFColor();
                    if (fontColor != null) {
                        byte[] rgb = fontColor.getRGB();
                        if (rgb != null && rgb.length == 3) {
                            sb.append("color: ").append(String.format("#%02x%02x%02x", rgb[0], rgb[1], rgb[2])).append("; ");
                        }
                    }
                }
            }

            // Alignment
            HorizontalAlignment align = cs.getAlignment();
            if (align == HorizontalAlignment.CENTER) {
                sb.append("text-align: center; ");
            } else if (align == HorizontalAlignment.RIGHT) {
                sb.append("text-align: right; ");
            } else {
                sb.append("text-align: left; ");
            }

            VerticalAlignment vAlign = cs.getVerticalAlignment();
            if (vAlign == VerticalAlignment.TOP) {
                sb.append("vertical-align: top; ");
            } else if (vAlign == VerticalAlignment.BOTTOM) {
                sb.append("vertical-align: bottom; ");
            } else {
                sb.append("vertical-align: middle; ");
            }

            // Background Fill Color
            FillPatternType fillPattern = cs.getFillPattern();
            if (fillPattern != FillPatternType.NO_FILL) {
                Color fill = cs.getFillForegroundColorColor();
                if (fill instanceof XSSFColor xssfColor) {
                    byte[] rgb = xssfColor.getRGB();
                    if (rgb != null && rgb.length == 3) {
                        String hex = String.format("#%02x%02x%02x", rgb[0], rgb[1], rgb[2]);
                        sb.append("background-color: ").append(hex).append("; ");
                    }
                }
            }

            // Borders
            sb.append(buildSideBorder("border-top", cs.getBorderTop(), displayGridlines));
            sb.append(buildSideBorder("border-bottom", cs.getBorderBottom(), displayGridlines));
            sb.append(buildSideBorder("border-left", cs.getBorderLeft(), displayGridlines));
            sb.append(buildSideBorder("border-right", cs.getBorderRight(), displayGridlines));
        } else {
            sb.append(displayGridlines ? "border: 1px solid #e9ecef; " : "border: none; ");
        }

        return sb.toString();
    }

    private String buildSideBorder(String borderSide, BorderStyle borderStyle, boolean displayGridlines) {
        if (borderStyle == null || borderStyle == BorderStyle.NONE) {
            return displayGridlines ? (borderSide + ": 1px solid #f1f3f5; ") : (borderSide + ": none; ");
        }
        return switch (borderStyle) {
            case THICK -> borderSide + ": 2px solid #212529; ";
            case DOUBLE -> borderSide + ": 3px double #212529; ";
            case DASHED, DOTTED -> borderSide + ": 1px dashed #6c757d; ";
            default -> borderSide + ": 1px solid #212529; ";
        };
    }
}
