package com.appdev.Finance.Service;

import com.appdev.Finance.Repository.ReportRepository;
import com.appdev.Finance.DTO.ReportSummaryDTO;
import com.appdev.Finance.model.Transaction;
import com.appdev.Finance.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

// ... (other imports remain the same)
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Year;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;


@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "PH")); // PHP Currency

    @Autowired
    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    // --- Existing Methods ---

    public List<Transaction> getTransactionsForUserByMonth(User user, String month) {
        if (user == null || !StringUtils.hasText(month)) {
            // Consider throwing IllegalArgumentException or returning Collections.emptyList()
            // For consistency with other methods, let's return empty list if inputs are invalid
            // Or rely on the controller to handle invalid user.
            // throw new IllegalArgumentException("User and month must not be null or empty.");
            return Collections.emptyList();
        }
        return reportRepository.findByUserIdAndMonth(user.getId(), month);
    }

    // New service method
    public List<Transaction> getTransactionsForUserByMonthAndCategory(User user, String month, String category) {
        if (user == null || !StringUtils.hasText(month) || !StringUtils.hasText(category)) {
            return Collections.emptyList();
        }
        return reportRepository.findByUserIdAndMonthAndCategoryIgnoreCase(user.getId(), month, category);
    }

    // New service method
    public List<String> getDistinctCategoriesForUserByMonth(User user, String month) {
        if (user == null || !StringUtils.hasText(month)) {
            return Collections.emptyList();
        }
        return reportRepository.findDistinctCategoriesByUserIdAndMonth(user.getId(), month);
    }


    public ReportSummaryDTO getReportSummaryForMonth(User user, String month) {
        if (user == null || !StringUtils.hasText(month)) {
             return new ReportSummaryDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        Double incomeSum = reportRepository.findTotalIncomeByUserForMonth(user.getId(), month);
        Double expenseSum = reportRepository.findTotalExpensesByUserForMonth(user.getId(), month);

        BigDecimal totalIncome = (incomeSum != null) ? BigDecimal.valueOf(incomeSum) : BigDecimal.ZERO;
        BigDecimal totalExpenses = (expenseSum != null) ? BigDecimal.valueOf(expenseSum) : BigDecimal.ZERO;
        BigDecimal netTotal = totalIncome.subtract(totalExpenses);

        return new ReportSummaryDTO(totalIncome, totalExpenses, netTotal);
    }

    public Map<String, Object> generateFullReportDataForMonth(User user, String month) {
        List<Transaction> transactions = getTransactionsForUserByMonth(user, month);
        ReportSummaryDTO summary = getReportSummaryForMonth(user, month);
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("transactions", transactions);
        reportData.put("summary", summary);
        return reportData;
    }

    // --- PDF Generation Method (remains the same) ---
    public byte[] generatePdfReport(User user, String selectedMonth) throws IOException {
        // ... (existing PDF generation code) ...
        List<Transaction> transactions;
        // If filtering is applied on the report page too, this might need adjustment
        // For now, assuming PDF uses all transactions for the month
        transactions = getTransactionsForUserByMonth(user, selectedMonth);
        ReportSummaryDTO summary = getReportSummaryForMonth(user, selectedMonth);
        String currentYear = String.valueOf(Year.now().getValue());
        String reportTitle = String.format("Financial Report - %s %s", selectedMonth, currentYear);

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float margin = 50;
            float yStart = page.getMediaBox().getHeight() - margin;
            float tableTop; 
            float yPosition = yStart;
            float tableWidth = page.getMediaBox().getWidth() - 2 * margin;
            float bottomMargin = margin;

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {

                // Title
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                contentStream.newLineAtOffset(margin, yStart);
                contentStream.showText(reportTitle);
                contentStream.endText();
                yPosition -= 30;

                // Summary Section
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Summary");
                contentStream.endText();
                yPosition -= 15;

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Total Income: " + formatCurrency(summary.getTotalIncome()));
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("Total Expenses: " + formatCurrency(summary.getTotalExpenses()));
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText("Net Total: " + formatCurrency(summary.getNetTotal()));
                contentStream.endText();
                yPosition -= 45;

                // Transaction Table Section
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Transactions");
                contentStream.endText();
                yPosition -= 20;
                tableTop = yPosition; 

                contentStream.setLineWidth(1f);
                contentStream.moveTo(margin, tableTop + 5);
                contentStream.lineTo(margin + tableWidth, tableTop + 5);
                contentStream.stroke();

                float tableXPosition = margin;
                float headerYPosition = tableTop;
                float[] colWidths = { 200f, 100f, 80f, 100f };

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
                contentStream.newLineAtOffset(tableXPosition + 5, headerYPosition);
                contentStream.showText("Description");
                contentStream.newLineAtOffset(colWidths[0], 0); 
                contentStream.showText("Category");
                contentStream.newLineAtOffset(colWidths[1], 0);
                contentStream.showText("Type");
                contentStream.newLineAtOffset(colWidths[2], 0);
                contentStream.showText("Amount");
                contentStream.endText();
                yPosition -= 15;

                contentStream.moveTo(margin, yPosition + 5);
                contentStream.lineTo(margin + tableWidth, yPosition + 5);
                contentStream.stroke();

                contentStream.setFont(PDType1Font.HELVETICA, 9);
                float rowHeight = 15f;
                PDPage currentPage = page; 
                PDPageContentStream currentContentStream = contentStream; 


                for (Transaction tx : transactions) {
                    if (yPosition < bottomMargin) {
                         currentContentStream.close();

                        currentPage = new PDPage(PDRectangle.A4);
                        document.addPage(currentPage);
                        currentContentStream = new PDPageContentStream(document, currentPage); 
                        yPosition = currentPage.getMediaBox().getHeight() - margin; 
                        currentContentStream.setFont(PDType1Font.HELVETICA, 9); 
                    }

                    tableXPosition = margin;
                    currentContentStream.beginText();
                    currentContentStream.newLineAtOffset(tableXPosition + 5, yPosition);
                    currentContentStream.showText(truncate(tx.getDescription(), 35));
                    currentContentStream.newLineAtOffset(colWidths[0], 0);
                    currentContentStream.showText(truncate(tx.getCategory(), 18));
                    currentContentStream.newLineAtOffset(colWidths[1], 0);
                    currentContentStream.showText(truncate(tx.getType(), 10));
                    currentContentStream.newLineAtOffset(colWidths[2], 0);
                    currentContentStream.showText(formatCurrency(tx.getAmount()));
                    currentContentStream.endText();
                    yPosition -= rowHeight; 
                }
                 if (currentContentStream != contentStream) { 
                     currentContentStream.close();
                 }
            } 

            document.save(baos);
            return baos.toByteArray();
        }
    }
    
    private String formatCurrency(BigDecimal value) {
        if (value == null) return currencyFormatter.format(BigDecimal.ZERO);
        return currencyFormatter.format(value);
    }

    private String formatCurrency(Double value) {
        if (value == null) return currencyFormatter.format(0.0);
        return currencyFormatter.format(value);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}