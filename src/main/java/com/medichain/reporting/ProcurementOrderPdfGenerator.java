package com.medichain.reporting;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.medichain.domain.entity.ProcurementOrder;
import com.medichain.domain.entity.ProcurementLineItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcurementOrderPdfGenerator {

    public byte[] generatePdf(ProcurementOrder order) {
        var baos = new ByteArrayOutputStream();
        var writer = new PdfWriter(baos);
        var pdf = new PdfDocument(writer);
        var document = new Document(pdf);

        try {
            var boldFont = PdfFontFactory.createRegisteredFont("Helvetica-Bold");
            var normalFont = PdfFontFactory.createRegisteredFont("Helvetica");

            document.add(new Paragraph("PROCUREMENT ORDER")
                .setFont(boldFont).setFontSize(18).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
            document.add(new Paragraph(" ")
                .setFont(normalFont).setFontSize(10));

            document.add(new Paragraph("Order No: " + order.getOrderNumber())
                .setFont(boldFont).setFontSize(11));
            document.add(new Paragraph("Date: " + order.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .setFont(normalFont).setFontSize(10));
            document.add(new Paragraph("Status: " + order.getStatus().name())
                .setFont(normalFont).setFontSize(10));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Vendor Details:")
                .setFont(boldFont).setFontSize(11));
            document.add(new Paragraph(order.getVendor().getName())
                .setFont(normalFont).setFontSize(10));
            document.add(new Paragraph("GSTIN: " + (order.getVendor().getGstin() != null ? order.getVendor().getGstin() : "N/A"))
                .setFont(normalFont).setFontSize(10));
            document.add(new Paragraph("Drug License: " + (order.getVendor().getDrugLicenseNumber() != null ? order.getVendor().getDrugLicenseNumber() : "N/A"))
                .setFont(normalFont).setFontSize(10));
            document.add(new Paragraph(" "));

            if (order.getLineItems() != null && !order.getLineItems().isEmpty()) {
                var table = new Table(UnitValue.createPercentArray(new float[]{5, 25, 15, 15, 15, 25}));
                table.setWidth(UnitValue.createPercentValue(100));

                addHeaderCell(table, "#", boldFont);
                addHeaderCell(table, "Drug", boldFont);
                addHeaderCell(table, "HSN Code", boldFont);
                addHeaderCell(table, "Qty", boldFont);
                addHeaderCell(table, "Unit Price", boldFont);
                addHeaderCell(table, "Total", boldFont);

                int lineNo = 1;
                for (var item : order.getLineItems()) {
                    addCell(table, String.valueOf(lineNo++), normalFont);
                    addCell(table, item.getDrugSku().getGenericName() + " " + item.getDrugSku().getStrength(), normalFont);
                    addCell(table, item.getDrugSku().getHsnCode() != null ? item.getDrugSku().getHsnCode() : "N/A", normalFont);
                    addCell(table, String.valueOf(item.getRequestedQuantity()), normalFont);
                    addCell(table, "₹" + (item.getUnitPrice() != null ? item.getUnitPrice().toString() : "0"), normalFont);
                    addCell(table, "₹" + (item.getLineTotal() != null ? item.getLineTotal().toString() : "0"), normalFont);
                }

                document.add(table);
            } else {
                document.add(new Paragraph("No line items in this order.")
                    .setFont(normalFont).setFontSize(10).setItalic());
            }
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Total Amount (excl. GST): ₹" + formatAmount(order.getTotalAmount()))
                .setFont(normalFont).setFontSize(10));
            document.add(new Paragraph("GST (12%): ₹" + formatAmount(order.getGstAmount()))
                .setFont(normalFont).setFontSize(10));
            document.add(new Paragraph("Grand Total: ₹" + formatAmount(order.getGrandTotal()))
                .setFont(boldFont).setFontSize(12).setFontColor(ColorConstants.RED));

            if (order.getNotes() != null && !order.getNotes().isBlank()) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Notes: " + order.getNotes())
                    .setFont(normalFont).setFontSize(10).setItalic());
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Authorized Signatory")
                .setFont(boldFont).setFontSize(11));
            document.add(new Paragraph("____________________________")
                .setFont(normalFont).setFontSize(10));
            document.add(new Paragraph(order.getGeneratedBy().getFullName())
                .setFont(normalFont).setFontSize(10));
        } catch (Exception e) {
            log.error("Failed to generate procurement PDF: {}", e.getMessage());
            throw new RuntimeException("PDF generation failed", e);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    private void addHeaderCell(Table table, String text, com.itextpdf.kernel.font.PdfFont font) {
        var cell = new Cell().add(new Paragraph(text).setFont(font).setFontSize(9));
        cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        table.addCell(cell);
    }

    private void addCell(Table table, String text, com.itextpdf.kernel.font.PdfFont font) {
        table.addCell(new Cell().add(new Paragraph(text).setFont(font).setFontSize(9)));
    }

    private String formatAmount(BigDecimal amount) {
        return amount != null ? String.format("%.2f", amount) : "0.00";
    }
}
