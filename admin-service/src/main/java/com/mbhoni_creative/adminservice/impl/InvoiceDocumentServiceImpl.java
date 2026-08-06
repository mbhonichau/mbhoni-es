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
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import com.mbhoni_creative.adminentity.Invoice;
import com.mbhoni_creative.adminservice.InvoiceDocumentService;

@Service
public class InvoiceDocumentServiceImpl implements InvoiceDocumentService {

    @Override
    public byte[] generateSampleInvoiceExcelTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("Invoice Template");
            sheet.setDisplayGridlines(true);

            sheet.setColumnWidth(0, 7000);
            sheet.setColumnWidth(1, 10000);
            sheet.setColumnWidth(2, 4000);
            sheet.setColumnWidth(3, 5000);

            // Banner Style
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

            // Section Header Style
            XSSFCellStyle sectionStyle = workbook.createCellStyle();
            XSSFFont sectionFont = workbook.createFont();
            sectionFont.setBold(true);
            sectionFont.setFontHeightInPoints((short) 11);
            sectionFont.setColor(IndexedColors.WHITE.getIndex());
            sectionStyle.setFont(sectionFont);
            sectionStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 67, (byte) 97, (byte) 238}, null));
            sectionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Bold Style
            XSSFCellStyle boldStyle = workbook.createCellStyle();
            XSSFFont boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            // Title Banner Merged A1:D1
            Row r0 = sheet.createRow(0);
            r0.setHeightInPoints(40);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("${tenantName} - OFFICIAL INVOICE / QUOTATION");
            c0.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            // Metadata Rows
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("Invoice Number:");
            r2.createCell(1).setCellValue("${invoiceNumber}");
            r2.createCell(2).setCellValue("Issue Date:");
            r2.createCell(3).setCellValue("${issueDate}");

            Row r3 = sheet.createRow(3);
            r3.createCell(0).setCellValue("Client Name:");
            r3.createCell(1).setCellValue("${clientName}");
            r3.createCell(2).setCellValue("Due Date:");
            r3.createCell(3).setCellValue("${dueDate}");

            Row r4 = sheet.createRow(4);
            r4.createCell(0).setCellValue("Invoice Status:");
            r4.createCell(1).setCellValue("${status}");

            // Itemized Table Banner
            Row r6 = sheet.createRow(6);
            r6.setHeightInPoints(26);
            Cell c6a = r6.createCell(0); c6a.setCellValue("ITEM / SERVICE DESCRIPTION"); c6a.setCellStyle(sectionStyle);
            sheet.addMergedRegion(new CellRangeAddress(6, 6, 0, 1));
            Cell c6b = r6.createCell(2); c6b.setCellValue("TAX (VAT)"); c6b.setCellStyle(sectionStyle);
            Cell c6c = r6.createCell(3); c6c.setCellValue("AMOUNT"); c6c.setCellStyle(sectionStyle);

            // Line Items
            Row r7 = sheet.createRow(7);
            r7.createCell(0).setCellValue("Professional Commercial Services / Enterprise Plan");
            r7.createCell(2).setCellValue("15% Included");
            r7.createCell(3).setCellValue("${subtotal}");
            sheet.addMergedRegion(new CellRangeAddress(7, 7, 0, 1));

            // Totals Section
            Row r9 = sheet.createRow(9);
            r9.createCell(2).setCellValue("Subtotal Amount:");
            r9.createCell(3).setCellValue("${subtotal}");

            Row r10 = sheet.createRow(10);
            r10.createCell(2).setCellValue("VAT Tax (15%):");
            r10.createCell(3).setCellValue("${taxAmount}");

            Row r11 = sheet.createRow(11);
            Cell c11 = r11.createCell(2); c11.setCellValue("TOTAL AMOUNT DUE:"); c11.setCellStyle(boldStyle);
            Cell c11v = r11.createCell(3); c11v.setCellValue("${totalAmount}"); c11v.setCellStyle(titleStyle);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating sample Excel invoice template", e);
        }
    }

    @Override
    public byte[] generateSampleInvoiceWordTemplate() {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Title Paragraph
            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText("${tenantName} - OFFICIAL INVOICE");
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setColor("1B1E15");

            // Subtitle
            XWPFParagraph subPara = doc.createParagraph();
            subPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun subRun = subPara.createRun();
            subRun.setText("Invoice #: ${invoiceNumber} | Issue Date: ${issueDate} | Due Date: ${dueDate}");
            subRun.setItalic(true);
            subRun.setFontSize(10);

            doc.createParagraph();

            // Client Info Table
            XWPFTable infoTable = doc.createTable(2, 2);
            infoTable.setWidth("100%");
            setCell(infoTable.getRow(0).getCell(0), "Billed To Client:", "${clientName}", true);
            setCell(infoTable.getRow(0).getCell(1), "Payment Status:", "${status}", true);
            setCell(infoTable.getRow(1).getCell(0), "Invoice Number:", "${invoiceNumber}", false);
            setCell(infoTable.getRow(1).getCell(1), "Amount Paid:", "${amountPaid}", false);

            doc.createParagraph();

            // Financial Summary Table
            XWPFTable finTable = doc.createTable(4, 2);
            finTable.setWidth("100%");
            setHeaderCell(finTable.getRow(0).getCell(0), "DESCRIPTION");
            setHeaderCell(finTable.getRow(0).getCell(1), "AMOUNT");

            setCell(finTable.getRow(1).getCell(0), "Subtotal Amount", "", false);
            setCell(finTable.getRow(1).getCell(1), "", "${subtotal}", false);

            setCell(finTable.getRow(2).getCell(0), "VAT Tax Amount (15%)", "", false);
            setCell(finTable.getRow(2).getCell(1), "", "${taxAmount}", false);

            setCell(finTable.getRow(3).getCell(0), "TOTAL AMOUNT DUE", "", true);
            setCell(finTable.getRow(3).getCell(1), "", "${totalAmount}", true);

            doc.createParagraph();

            // Balance Due Banner
            XWPFParagraph balPara = doc.createParagraph();
            balPara.setAlignment(ParagraphAlignment.RIGHT);
            XWPFRun balRun = balPara.createRun();
            balRun.setText("BALANCE DUE: ${balanceDue}");
            balRun.setBold(true);
            balRun.setFontSize(14);
            balRun.setColor("2E7D32");

            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating sample Word invoice template", e);
        }
    }

    @Override
    public String renderInvoiceFromExcel(byte[] excelBytes, Invoice invoice) {
        if (excelBytes == null || excelBytes.length == 0 || invoice == null) return null;

        try (ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
             Workbook workbook = WorkbookFactory.create(in)) {

            Map<String, String> replacements = buildReplacements(invoice);
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter dataFormatter = new DataFormatter();

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

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            evaluator.evaluateAll();

            List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
            boolean displayGridlines = sheet.isDisplayGridlines();
            int maxColumns = getSheetMaxColumns(sheet);

            StringBuilder html = new StringBuilder();
            html.append("<div class=\"excel-invoice-wrapper p-4 bg-white rounded shadow-sm border mx-auto\" style=\"max-width: 900px;\">");
            html.append("<table class=\"excel-invoice-table align-middle mb-0\" style=\"border-collapse: collapse; width: 100%; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\">");

            for (int rowIdx = 0; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                float rowHeight = row.getHeightInPoints();
                String heightStyle = rowHeight > 0 ? ("height: " + Math.round(rowHeight * 1.33) + "px;") : "height: 28px;";
                html.append("<tr style=\"").append(heightStyle).append("\">");

                for (int colIdx = 0; colIdx < maxColumns; colIdx++) {
                    CellRangeAddress mergeRegion = getMergedRegionAt(mergedRegions, rowIdx, colIdx);
                    if (mergeRegion != null && (rowIdx != mergeRegion.getFirstRow() || colIdx != mergeRegion.getFirstColumn())) {
                        continue;
                    }

                    Cell cell = row.getCell(colIdx);
                    int colspan = mergeRegion != null ? (mergeRegion.getLastColumn() - mergeRegion.getFirstColumn() + 1) : 1;
                    int rowspan = mergeRegion != null ? (mergeRegion.getLastRow() - mergeRegion.getFirstRow() + 1) : 1;

                    String cellContent = getCellValueAsString(cell, evaluator, dataFormatter);
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

            html.append("</table></div>");
            return html.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String renderInvoiceFromWord(byte[] wordBytes, Invoice invoice) {
        if (wordBytes == null || wordBytes.length == 0 || invoice == null) return null;

        try (ByteArrayInputStream in = new ByteArrayInputStream(wordBytes);
             XWPFDocument doc = new XWPFDocument(in)) {

            Map<String, String> replacements = buildReplacements(invoice);

            for (XWPFParagraph p : doc.getParagraphs()) {
                replaceInParagraph(p, replacements);
            }

            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph p : cell.getParagraphs()) {
                            replaceInParagraph(p, replacements);
                        }
                    }
                }
            }

            StringBuilder html = new StringBuilder();
            html.append("<div class=\"word-invoice-wrapper p-4 bg-white rounded shadow-sm border mx-auto\" style=\"max-width: 860px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\">");

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

    private Map<String, String> buildReplacements(Invoice invoice) {
        Map<String, String> map = new HashMap<>();

        String tenantName = invoice.getTenant() != null ? invoice.getTenant().getName() : "Enterprise Commercial";
        String clientName = invoice.getSubscription() != null && invoice.getSubscription().getTenant() != null 
                ? invoice.getSubscription().getTenant().getName() 
                : "Valued Commercial Client";

        map.put("${tenantName}", tenantName);
        map.put("${clientName}", clientName);
        map.put("${invoiceNumber}", invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "");
        map.put("${status}", invoice.getStatus() != null ? invoice.getStatus().toString() : "DRAFT");
        map.put("${issueDate}", invoice.getIssueDate() != null ? invoice.getIssueDate().toString() : "");
        map.put("${dueDate}", invoice.getDueDate() != null ? invoice.getDueDate().toString() : "");
        map.put("${subtotal}", formatMoney(invoice.getSubtotal()));
        map.put("${taxAmount}", formatMoney(invoice.getTaxAmount()));
        map.put("${totalAmount}", formatMoney(invoice.getTotalAmount()));
        map.put("${amountPaid}", formatMoney(invoice.getAmountPaid()));
        map.put("${balanceDue}", formatMoney(invoice.getBalanceDue()));
        map.put("${notes}", invoice.getNotes() != null ? invoice.getNotes() : "");

        return map;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "R 0.00";
        return String.format("R %,.2f", amount);
    }

    private void replaceInParagraph(XWPFParagraph p, Map<String, String> replacements) {
        if (p == null) return;
        String pText = p.getText();
        if (pText == null || !pText.contains("${")) return;

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            if (pText.contains(entry.getKey())) {
                pText = pText.replace(entry.getKey(), entry.getValue());
            }
        }

        List<XWPFRun> runs = p.getRuns();
        if (runs == null || runs.isEmpty()) return;

        XWPFRun firstRun = runs.get(0);
        boolean bold = firstRun.isBold();
        boolean italic = firstRun.isItalic();
        int fontSize = firstRun.getFontSize();
        String color = firstRun.getColor();

        for (int i = 0; i < runs.size(); i++) {
            XWPFRun r = runs.get(i);
            if (r.getEmbeddedPictures() == null || r.getEmbeddedPictures().isEmpty()) {
                try { r.setText("", 0); } catch (Exception ignored) {}
            }
        }

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

    private int getSheetMaxColumns(Sheet sheet) {
        int max = 4;
        for (Row r : sheet) {
            if (r != null && r.getLastCellNum() > max) max = r.getLastCellNum();
        }
        return Math.min(max, 12);
    }

    private CellRangeAddress getMergedRegionAt(List<CellRangeAddress> regions, int row, int col) {
        if (regions == null) return null;
        for (CellRangeAddress reg : regions) {
            if (reg.isInRange(row, col)) return reg;
        }
        return null;
    }

    private String getCellValueAsString(Cell cell, FormulaEvaluator evaluator, DataFormatter dataFormatter) {
        if (cell == null) return "";
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            try { type = evaluator.evaluate(cell).getCellType(); } catch (Exception e) { return cell.getCellFormula(); }
        }
        switch (type) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return dataFormatter.formatCellValue(cell, evaluator);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return "";
        }
    }

    private String buildCellStyle(Cell cell, boolean displayGridlines) {
        if (cell == null) return displayGridlines ? "padding: 6px 10px; font-size: 13px; border: 1px solid #e9ecef;" : "padding: 6px 10px; font-size: 13px; border: none;";
        StringBuilder sb = new StringBuilder("padding: 6px 10px; font-size: 13px; ");
        CellStyle cs = cell.getCellStyle();

        if (cs != null) {
            Font font = cell.getSheet().getWorkbook().getFontAt(cs.getFontIndex());
            if (font != null) {
                if (font.getBold()) sb.append("font-weight: bold; ");
                if (font.getItalic()) sb.append("font-style: italic; ");
                if (font.getFontHeightInPoints() > 0) sb.append("font-size: ").append(font.getFontHeightInPoints()).append("pt; ");
                if (font instanceof XSSFFont xssfFont && xssfFont.getXSSFColor() != null && xssfFont.getXSSFColor().getRGB() != null) {
                    byte[] rgb = xssfFont.getXSSFColor().getRGB();
                    if (rgb.length == 3) sb.append("color: ").append(String.format("#%02x%02x%02x", rgb[0], rgb[1], rgb[2])).append("; ");
                }
            }

            HorizontalAlignment align = cs.getAlignment();
            if (align == HorizontalAlignment.CENTER) sb.append("text-align: center; ");
            else if (align == HorizontalAlignment.RIGHT) sb.append("text-align: right; ");
            else sb.append("text-align: left; ");

            FillPatternType fillPattern = cs.getFillPattern();
            if (fillPattern != FillPatternType.NO_FILL && cs.getFillForegroundColorColor() instanceof XSSFColor xssfColor && xssfColor.getRGB() != null && xssfColor.getRGB().length == 3) {
                byte[] rgb = xssfColor.getRGB();
                sb.append("background-color: ").append(String.format("#%02x%02x%02x", rgb[0], rgb[1], rgb[2])).append("; ");
            }

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

    private void setCell(XWPFTableCell cell, String l1, String v1, boolean bold) {
        XWPFParagraph p = cell.getParagraphs().get(0);
        XWPFRun r1 = p.createRun(); r1.setText(l1 + " "); r1.setBold(true);
        XWPFRun r2 = p.createRun(); r2.setText(v1); r2.setBold(bold);
    }

    private void setHeaderCell(XWPFTableCell cell, String title) {
        cell.setColor("4361EE");
        XWPFParagraph p = cell.getParagraphs().get(0);
        XWPFRun r = p.createRun(); r.setText(title); r.setBold(true); r.setColor("FFFFFF");
    }

    @Override
    public String renderTenantInvoiceFromExcel(byte[] excelBytes, com.mbhoni_creative.adminentity.TenantInvoice invoice) {
        if (excelBytes == null || excelBytes.length == 0 || invoice == null) return null;

        try (ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
             Workbook workbook = WorkbookFactory.create(in)) {

            Map<String, String> replacements = buildTenantInvoiceReplacements(invoice);
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter dataFormatter = new DataFormatter();

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

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            evaluator.evaluateAll();

            List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
            boolean displayGridlines = sheet.isDisplayGridlines();
            int maxColumns = getSheetMaxColumns(sheet);

            StringBuilder html = new StringBuilder();
            html.append("<div class=\"excel-invoice-wrapper p-4 bg-white rounded shadow-sm border mx-auto\" style=\"max-width: 900px;\">");
            html.append("<table class=\"excel-invoice-table align-middle mb-0\" style=\"border-collapse: collapse; width: 100%; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\">");

            for (int rowIdx = 0; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                float rowHeight = row.getHeightInPoints();
                String heightStyle = rowHeight > 0 ? ("height: " + Math.round(rowHeight * 1.33) + "px;") : "height: 28px;";
                html.append("<tr style=\"").append(heightStyle).append("\">");

                for (int colIdx = 0; colIdx < maxColumns; colIdx++) {
                    CellRangeAddress mergeRegion = getMergedRegionAt(mergedRegions, rowIdx, colIdx);
                    if (mergeRegion != null && (rowIdx != mergeRegion.getFirstRow() || colIdx != mergeRegion.getFirstColumn())) {
                        continue;
                    }

                    Cell cell = row.getCell(colIdx);
                    int colspan = mergeRegion != null ? (mergeRegion.getLastColumn() - mergeRegion.getFirstColumn() + 1) : 1;
                    int rowspan = mergeRegion != null ? (mergeRegion.getLastRow() - mergeRegion.getFirstRow() + 1) : 1;

                    String cellContent = getCellValueAsString(cell, evaluator, dataFormatter);
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

            html.append("</table></div>");
            return html.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String renderTenantInvoiceFromWord(byte[] wordBytes, com.mbhoni_creative.adminentity.TenantInvoice invoice) {
        if (wordBytes == null || wordBytes.length == 0 || invoice == null) return null;

        try (ByteArrayInputStream in = new ByteArrayInputStream(wordBytes);
             XWPFDocument doc = new XWPFDocument(in)) {

            Map<String, String> replacements = buildTenantInvoiceReplacements(invoice);

            for (XWPFParagraph p : doc.getParagraphs()) {
                replaceInParagraph(p, replacements);
            }

            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph p : cell.getParagraphs()) {
                            replaceInParagraph(p, replacements);
                        }
                    }
                }
            }

            StringBuilder html = new StringBuilder();
            html.append("<div class=\"word-invoice-wrapper p-4 bg-white rounded shadow-sm border mx-auto\" style=\"max-width: 860px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\">");

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

    @Override
    public String renderTenantQuotationFromExcel(byte[] excelBytes, com.mbhoni_creative.adminentity.TenantQuotation quotation) {
        if (excelBytes == null || excelBytes.length == 0 || quotation == null) return null;

        try (ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
             Workbook workbook = WorkbookFactory.create(in)) {

            Map<String, String> replacements = buildTenantQuotationReplacements(quotation);
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter dataFormatter = new DataFormatter();

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

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            evaluator.evaluateAll();

            List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
            boolean displayGridlines = sheet.isDisplayGridlines();
            int maxColumns = getSheetMaxColumns(sheet);

            StringBuilder html = new StringBuilder();
            html.append("<div class=\"excel-invoice-wrapper p-4 bg-white rounded shadow-sm border mx-auto\" style=\"max-width: 900px;\">");
            html.append("<table class=\"excel-invoice-table align-middle mb-0\" style=\"border-collapse: collapse; width: 100%; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\">");

            for (int rowIdx = 0; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                float rowHeight = row.getHeightInPoints();
                String heightStyle = rowHeight > 0 ? ("height: " + Math.round(rowHeight * 1.33) + "px;") : "height: 28px;";
                html.append("<tr style=\"").append(heightStyle).append("\">");

                for (int colIdx = 0; colIdx < maxColumns; colIdx++) {
                    CellRangeAddress mergeRegion = getMergedRegionAt(mergedRegions, rowIdx, colIdx);
                    if (mergeRegion != null && (rowIdx != mergeRegion.getFirstRow() || colIdx != mergeRegion.getFirstColumn())) {
                        continue;
                    }

                    Cell cell = row.getCell(colIdx);
                    int colspan = mergeRegion != null ? (mergeRegion.getLastColumn() - mergeRegion.getFirstColumn() + 1) : 1;
                    int rowspan = mergeRegion != null ? (mergeRegion.getLastRow() - mergeRegion.getFirstRow() + 1) : 1;

                    String cellContent = getCellValueAsString(cell, evaluator, dataFormatter);
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

            html.append("</table></div>");
            return html.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String renderTenantQuotationFromWord(byte[] wordBytes, com.mbhoni_creative.adminentity.TenantQuotation quotation) {
        if (wordBytes == null || wordBytes.length == 0 || quotation == null) return null;

        try (ByteArrayInputStream in = new ByteArrayInputStream(wordBytes);
             XWPFDocument doc = new XWPFDocument(in)) {

            Map<String, String> replacements = buildTenantQuotationReplacements(quotation);

            for (XWPFParagraph p : doc.getParagraphs()) {
                replaceInParagraph(p, replacements);
            }

            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph p : cell.getParagraphs()) {
                            replaceInParagraph(p, replacements);
                        }
                    }
                }
            }

            StringBuilder html = new StringBuilder();
            html.append("<div class=\"word-invoice-wrapper p-4 bg-white rounded shadow-sm border mx-auto\" style=\"max-width: 860px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\">");

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

    private Map<String, String> buildTenantInvoiceReplacements(com.mbhoni_creative.adminentity.TenantInvoice invoice) {
        Map<String, String> map = new HashMap<>();

        String tenantName = invoice.getTenant() != null ? invoice.getTenant().getName() : "Enterprise";
        String clientName = invoice.getCustomerName() != null ? invoice.getCustomerName() : "Valued Customer";

        map.put("${tenantName}", tenantName);
        map.put("${clientName}", clientName);
        map.put("${customerName}", clientName);
        map.put("${customerEmail}", invoice.getCustomerEmail() != null ? invoice.getCustomerEmail() : "");
        map.put("${customerPhone}", invoice.getCustomerPhone() != null ? invoice.getCustomerPhone() : "");
        map.put("${customerAddress}", invoice.getCustomerAddress() != null ? invoice.getCustomerAddress() : "");
        map.put("${customerTaxNumber}", invoice.getCustomerTaxNumber() != null ? invoice.getCustomerTaxNumber() : "");
        map.put("${invoiceNumber}", invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "");
        map.put("${status}", invoice.getStatus() != null ? invoice.getStatus().toString() : "DRAFT");
        map.put("${issueDate}", invoice.getIssueDate() != null ? invoice.getIssueDate().toString() : "");
        map.put("${dueDate}", invoice.getDueDate() != null ? invoice.getDueDate().toString() : "");
        map.put("${subtotal}", formatMoney(invoice.getSubtotal()));
        map.put("${taxAmount}", formatMoney(invoice.getTaxAmount()));
        map.put("${totalAmount}", formatMoney(invoice.getTotalAmount()));
        map.put("${amountPaid}", formatMoney(invoice.getAmountPaid()));
        map.put("${balanceDue}", formatMoney(invoice.getBalanceDue()));
        map.put("${notes}", invoice.getNotes() != null ? invoice.getNotes() : "");
        map.put("${paymentTerms}", invoice.getPaymentTerms() != null ? invoice.getPaymentTerms() : "");

        if (invoice.getItems() != null) {
            int idx = 1;
            for (com.mbhoni_creative.adminentity.TenantInvoiceItem item : invoice.getItems()) {
                map.put("${itemDescription_" + idx + "}", item.getDescription());
                map.put("${itemQty_" + idx + "}", item.getQuantity() != null ? item.getQuantity().toString() : "1");
                map.put("${itemPrice_" + idx + "}", formatMoney(item.getUnitPrice()));
                map.put("${itemTotal_" + idx + "}", formatMoney(item.getTotalAmount()));
                idx++;
            }
        }

        return map;
    }

    private Map<String, String> buildTenantQuotationReplacements(com.mbhoni_creative.adminentity.TenantQuotation quotation) {
        Map<String, String> map = new HashMap<>();

        String tenantName = quotation.getTenant() != null ? quotation.getTenant().getName() : "Enterprise";
        String clientName = quotation.getCustomerName() != null ? quotation.getCustomerName() : "Valued Customer";

        map.put("${tenantName}", tenantName);
        map.put("${clientName}", clientName);
        map.put("${customerName}", clientName);
        map.put("${customerEmail}", quotation.getCustomerEmail() != null ? quotation.getCustomerEmail() : "");
        map.put("${customerPhone}", quotation.getCustomerPhone() != null ? quotation.getCustomerPhone() : "");
        map.put("${customerAddress}", quotation.getCustomerAddress() != null ? quotation.getCustomerAddress() : "");
        map.put("${quotationNumber}", quotation.getQuotationNumber() != null ? quotation.getQuotationNumber() : "");
        map.put("${invoiceNumber}", quotation.getQuotationNumber() != null ? quotation.getQuotationNumber() : "");
        map.put("${status}", quotation.getStatus() != null ? quotation.getStatus().toString() : "DRAFT");
        map.put("${issueDate}", quotation.getIssueDate() != null ? quotation.getIssueDate().toString() : "");
        map.put("${dueDate}", quotation.getValidUntilDate() != null ? quotation.getValidUntilDate().toString() : "");
        map.put("${validUntilDate}", quotation.getValidUntilDate() != null ? quotation.getValidUntilDate().toString() : "");
        map.put("${subtotal}", formatMoney(quotation.getSubtotal()));
        map.put("${taxAmount}", formatMoney(quotation.getTaxAmount()));
        map.put("${totalAmount}", formatMoney(quotation.getTotalAmount()));
        map.put("${notes}", quotation.getNotes() != null ? quotation.getNotes() : "");

        if (quotation.getItems() != null) {
            int idx = 1;
            for (com.mbhoni_creative.adminentity.TenantQuotationItem item : quotation.getItems()) {
                map.put("${itemDescription_" + idx + "}", item.getDescription());
                map.put("${itemQty_" + idx + "}", item.getQuantity() != null ? item.getQuantity().toString() : "1");
                map.put("${itemPrice_" + idx + "}", formatMoney(item.getUnitPrice()));
                map.put("${itemTotal_" + idx + "}", formatMoney(item.getTotalAmount()));
                idx++;
            }
        }

        return map;
    }
}
