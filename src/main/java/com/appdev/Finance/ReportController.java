package com.appdev.Finance;

import com.appdev.Finance.DTO.ReportSummaryDTO;
import com.appdev.Finance.Repository.TransactionRepository; // If needed directly, or through ReportService
import com.appdev.Finance.Service.ReportService;
import com.appdev.Finance.model.Transaction;
import com.appdev.Finance.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class ReportController {

    private final ReportService reportService;
    private final TransactionRepository transactionRepository; // For all-time chart data
    private final ObjectMapper objectMapper;

    @Autowired
    public ReportController(ReportService reportService,
                            TransactionRepository transactionRepository,
                            ObjectMapper objectMapper) {
        this.reportService = reportService;
        this.transactionRepository = transactionRepository;
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
    }

    @GetMapping("/report")
    public String showReportPage(
            @RequestParam(value = "selectedMonth", required = false) String selectedMonthParam,
            HttpSession session,
            Model model) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        String selectedMonth;
        if (selectedMonthParam == null || selectedMonthParam.trim().isEmpty()) {
            selectedMonth = LocalDate.now().getMonth()
                               .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        } else {
            selectedMonth = selectedMonthParam;
        }
        model.addAttribute("selectedMonth", selectedMonth);

        List<Transaction> filteredTransactions = Collections.emptyList();
        ReportSummaryDTO summary = new ReportSummaryDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        String categoryDataJson = "{}";
        String monthlyDataJson = "{}";
        List<Transaction> allTransactions = new ArrayList<>();

        try {
            filteredTransactions = reportService.getTransactionsForUserByMonth(loggedInUser, selectedMonth);
            summary = reportService.getReportSummaryForMonth(loggedInUser, selectedMonth);

            allTransactions = transactionRepository.findByUserId(loggedInUser.getId());
            if (allTransactions == null) {
                allTransactions = new ArrayList<>();
            }

            Map<String, Double> categoryTotals = allTransactions.stream()
                .filter(t -> "Expense".equalsIgnoreCase(t.getType()))
                .collect(Collectors.groupingBy(Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)));

            Map<String, Double> monthlyTotals = allTransactions.stream()
                .filter(t -> "Expense".equalsIgnoreCase(t.getType()))
                .collect(Collectors.groupingBy(Transaction::getMonth,
                        Collectors.summingDouble(Transaction::getAmount)));

            try {
                 categoryDataJson = objectMapper.writeValueAsString(categoryTotals);
                 monthlyDataJson = objectMapper.writeValueAsString(monthlyTotals);
            } catch (JsonProcessingException e) {
                 System.err.println("Error converting chart data to JSON for report: " + e.getMessage());
                 model.addAttribute("errorMessage", "Error processing chart data.");
            }

        } catch (IllegalArgumentException e) {
             model.addAttribute("errorMessage", e.getMessage());
             System.err.println("Illegal argument fetching report data: " + e.getMessage());
        } catch (Exception e) {
             System.err.println("Error fetching report data: " + e.getMessage());
             e.printStackTrace();
             model.addAttribute("errorMessage", "Could not load report data for " + selectedMonth + ". Please try again.");
        }

        List<String> monthNames = Arrays.stream(Month.values())
                                       .map(m -> m.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                                       .collect(Collectors.toList());

        model.addAttribute("transactions", filteredTransactions);
        model.addAttribute("availableMonths", monthNames);
        model.addAttribute("totalIncome", summary.getTotalIncome());
        model.addAttribute("totalExpenses", summary.getTotalExpenses());
        model.addAttribute("netTotal", summary.getNetTotal());
        model.addAttribute("categoryData", categoryDataJson);
        model.addAttribute("monthlyData", monthlyDataJson);

        return "report";
    }

    @GetMapping("/report/download/csv")
    public ResponseEntity<String> downloadReportCsv(
            @RequestParam("selectedMonth") String selectedMonth,
            HttpSession session) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");
        }

        List<Transaction> transactions;
        ReportSummaryDTO summary;
        try {
            transactions = reportService.getTransactionsForUserByMonth(loggedInUser, selectedMonth);
            summary = reportService.getReportSummaryForMonth(loggedInUser, selectedMonth);
        } catch (Exception e) {
            System.err.println("Error fetching data for CSV download: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error generating report data.");
        }

        StringBuilder csvData = new StringBuilder();
        csvData.append("Report Summary for:,").append(selectedMonth).append("\n");
        csvData.append("Category,Amount (PHP)\n");
        csvData.append("Total Income,").append(summary.getTotalIncome() != null ? summary.getTotalIncome().toPlainString() : "0.00").append("\n");
        csvData.append("Total Expenses,").append(summary.getTotalExpenses() != null ? summary.getTotalExpenses().toPlainString() : "0.00").append("\n");
        csvData.append("Net Total,").append(summary.getNetTotal() != null ? summary.getNetTotal().toPlainString() : "0.00").append("\n\n");
        csvData.append("Transaction Details\n");
        csvData.append("Description,Category,Type,Amount (PHP)\n");
        if (transactions != null && !transactions.isEmpty()) {
            for (Transaction tx : transactions) {
                csvData.append(escapeCsvField(tx.getDescription())).append(",");
                csvData.append(escapeCsvField(tx.getCategory())).append(",");
                csvData.append(escapeCsvField(tx.getType())).append(",");
                csvData.append(tx.getAmount() != null ? String.format("%.2f", tx.getAmount()) : "0.00").append("\n");
            }
        } else {
            csvData.append("No transactions found for this month.\n");
        }

        HttpHeaders headers = new HttpHeaders();
        String currentYear = String.valueOf(Year.now().getValue());
        String safeMonth = selectedMonth.replaceAll("[^a-zA-Z0-9.-]", "_");
        String filename = String.format("report_%s_%s.csv", safeMonth, currentYear);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentType(MediaType.parseMediaType("text/csv"));

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData.toString());
    }

    @GetMapping("/report/download/pdf")
    public ResponseEntity<byte[]> downloadReportPdf(
            @RequestParam("selectedMonth") String selectedMonth,
            HttpSession session) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        try {
            byte[] pdfBytes = reportService.generatePdfReport(loggedInUser, selectedMonth);

            HttpHeaders headers = new HttpHeaders();
            String currentYear = String.valueOf(Year.now().getValue());
            String safeMonth = selectedMonth.replaceAll("[^a-zA-Z0-9.-]", "_");
            String filename = String.format("report_%s_%s.pdf", safeMonth, currentYear);

            headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
            headers.setContentType(MediaType.APPLICATION_PDF);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            System.err.println("Error generating PDF report: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } catch (Exception e) {
             System.err.println("Unexpected error during PDF report generation: " + e.getMessage());
             e.printStackTrace();
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private String escapeCsvField(String field) {
        if (field == null) return "\"\"";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}