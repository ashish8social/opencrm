package com.opencrm.sales.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.opencrm.common.exception.EntityNotFoundException;
import com.opencrm.data.model.Record;
import com.opencrm.data.repository.RecordRepository;
import com.opencrm.metadata.model.EntityDef;
import com.opencrm.metadata.service.EntityDefService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class QuotePdfService {

    private final RecordRepository recordRepository;
    private final EntityDefService entityDefService;
    private final ObjectMapper objectMapper;

    public QuotePdfService(RecordRepository recordRepository, EntityDefService entityDefService,
                           ObjectMapper objectMapper) {
        this.recordRepository = recordRepository;
        this.entityDefService = entityDefService;
        this.objectMapper = objectMapper;
    }

    public byte[] generateQuotePdf(UUID quoteId) {
        Record quoteRecord = recordRepository.findByIdAndIsDeletedFalse(quoteId)
                .orElseThrow(() -> new EntityNotFoundException("Quote not found: " + quoteId));

        Map<String, Object> quoteData = parseData(quoteRecord.getData());

        // Get quote line items
        EntityDef quoteLineItemEntity = entityDefService.findByApiName("QuoteLineItem");
        List<Record> lineItems = recordRepository.findByEntityAndFieldValue(
                quoteLineItemEntity.getId(), "QuoteId", quoteId.toString());

        // Resolve account name if present
        String accountName = resolveRecordName(quoteData.get("AccountId"));
        String oppName = resolveRecordName(quoteData.get("OpportunityId"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, new Color(30, 58, 138));
            Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(55, 65, 81));
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(75, 85, 99));
            Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(31, 41, 55));
            Font tableHeaderFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);

            // Title
            Paragraph title = new Paragraph("QUOTE", titleFont);
            title.setAlignment(Element.ALIGN_RIGHT);
            document.add(title);

            Paragraph quoteName = new Paragraph(quoteRecord.getName() != null ? quoteRecord.getName() : "Untitled Quote",
                    new Font(Font.HELVETICA, 14, Font.NORMAL, new Color(107, 114, 128)));
            quoteName.setAlignment(Element.ALIGN_RIGHT);
            document.add(quoteName);
            document.add(new Paragraph(" "));

            // Quote details
            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.setWidths(new float[]{1, 1});

            // Left column - Quote info
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(0);
            leftCell.addElement(new Paragraph("Quote Details", headerFont));
            leftCell.addElement(new Paragraph(" "));
            addDetail(leftCell, "Status", str(quoteData.get("Status")), boldFont, normalFont);
            addDetail(leftCell, "Expiration Date", str(quoteData.get("ExpirationDate")), boldFont, normalFont);
            if (oppName != null) addDetail(leftCell, "Opportunity", oppName, boldFont, normalFont);
            detailsTable.addCell(leftCell);

            // Right column - Account info
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(0);
            rightCell.addElement(new Paragraph("Bill To", headerFont));
            rightCell.addElement(new Paragraph(" "));
            if (accountName != null) addDetail(rightCell, "Account", accountName, boldFont, normalFont);
            addDetail(rightCell, "Billing Address", str(quoteData.get("BillingAddress")), boldFont, normalFont);
            detailsTable.addCell(rightCell);

            document.add(detailsTable);
            document.add(new Paragraph(" "));

            // Line items table
            if (!lineItems.isEmpty()) {
                Paragraph lineItemsHeader = new Paragraph("Line Items", headerFont);
                document.add(lineItemsHeader);
                document.add(new Paragraph(" "));

                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{3, 1, 1.5f, 1.5f, 1.5f});

                Color headerBg = new Color(30, 58, 138);
                addTableHeader(table, "Product", tableHeaderFont, headerBg);
                addTableHeader(table, "Qty", tableHeaderFont, headerBg);
                addTableHeader(table, "Unit Price", tableHeaderFont, headerBg);
                addTableHeader(table, "Discount", tableHeaderFont, headerBg);
                addTableHeader(table, "Total", tableHeaderFont, headerBg);

                double grandTotal = 0;
                boolean alt = false;
                for (Record lineItem : lineItems) {
                    Map<String, Object> liData = parseData(lineItem.getData());
                    String productName = resolveRecordName(liData.get("ProductId"));
                    double qty = toDouble(liData.get("Quantity"));
                    double unitPrice = toDouble(liData.get("UnitPrice"));
                    double discount = toDouble(liData.get("Discount"));
                    double lineTotal = toDouble(liData.get("TotalPrice"));
                    if (lineTotal == 0) lineTotal = qty * unitPrice * (1 - discount / 100);
                    grandTotal += lineTotal;

                    Color rowBg = alt ? new Color(243, 244, 246) : Color.WHITE;
                    addTableCell(table, productName != null ? productName : lineItem.getName(), normalFont, rowBg, Element.ALIGN_LEFT);
                    addTableCell(table, String.valueOf((int) qty), normalFont, rowBg, Element.ALIGN_CENTER);
                    addTableCell(table, formatCurrency(unitPrice), normalFont, rowBg, Element.ALIGN_RIGHT);
                    addTableCell(table, discount > 0 ? String.format("%.0f%%", discount) : "-", normalFont, rowBg, Element.ALIGN_CENTER);
                    addTableCell(table, formatCurrency(lineTotal), normalFont, rowBg, Element.ALIGN_RIGHT);
                    alt = !alt;
                }

                document.add(table);

                // Totals
                document.add(new Paragraph(" "));
                PdfPTable totalsTable = new PdfPTable(2);
                totalsTable.setWidthPercentage(40);
                totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

                double quoteDiscount = toDouble(quoteData.get("Discount"));
                if (quoteDiscount > 0) {
                    addTotalRow(totalsTable, "Subtotal", formatCurrency(grandTotal), normalFont, boldFont);
                    addTotalRow(totalsTable, "Discount", formatCurrency(quoteDiscount), normalFont, boldFont);
                    grandTotal -= quoteDiscount;
                }
                addTotalRow(totalsTable, "Grand Total", formatCurrency(grandTotal),
                        new Font(Font.HELVETICA, 12, Font.BOLD, new Color(30, 58, 138)),
                        new Font(Font.HELVETICA, 12, Font.BOLD, new Color(30, 58, 138)));

                document.add(totalsTable);
            }

            // Description / Terms
            Object desc = quoteData.get("Description");
            if (desc != null && !desc.toString().isBlank()) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Terms & Conditions", headerFont));
                document.add(new Paragraph(desc.toString(), normalFont));
            }

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return baos.toByteArray();
    }

    private String resolveRecordName(Object recordId) {
        if (recordId == null) return null;
        try {
            return recordRepository.findByIdAndIsDeletedFalse(UUID.fromString(recordId.toString()))
                    .map(Record::getName)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void addDetail(PdfPCell cell, String label, String value, Font boldFont, Font normalFont) {
        if (value == null || value.isBlank() || "null".equals(value)) return;
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ", boldFont));
        p.add(new Chunk(value, normalFont));
        cell.addElement(p);
    }

    private void addTableHeader(PdfPTable table, String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(8);
        cell.setBorderWidth(0);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        cell.setBorderWidth(0);
        cell.setBorderWidthBottom(0.5f);
        cell.setBorderColorBottom(new Color(229, 231, 235));
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(0);
        labelCell.setPadding(4);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(0);
        valueCell.setPadding(4);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private String formatCurrency(double amount) {
        return String.format("$%,.2f", amount);
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        try { return Double.parseDouble(val.toString()); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private String str(Object val) {
        return val != null ? val.toString() : null;
    }

    private Map<String, Object> parseData(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}
