package com.school.backend.finance.service;

import com.school.backend.finance.dto.DailyCashDashboardDto;
import com.school.backend.expense.dto.ExpenseVoucherDto;
import com.school.backend.expense.service.ExpenseService;
import com.school.backend.finance.dto.FinancialOverviewDto;
import com.school.backend.fee.dto.FeeTypeHeadSummaryDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class FinanceExportService {

    private final FinanceOverviewService financeOverviewService;
    private final ExpenseService expenseService;

    public FinanceExportService(
            FinanceOverviewService financeOverviewService,
            ExpenseService expenseService) {
        this.financeOverviewService = financeOverviewService;
        this.expenseService = expenseService;
    }

    public byte[] exportDailyCash(LocalDate date) {
        DailyCashDashboardDto dto = financeOverviewService.getDailyOverview(date);
        List<ExpenseVoucherDto> expenses = expenseService.getExpensesByDate(date);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Daily Cash");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle numericStyle = createNumericStyle(workbook);
            Set<Integer> usedColumns = new LinkedHashSet<>();
            int rowIdx = 0;

            rowIdx = writeLabelValueDate(sheet, rowIdx, "Date", date, usedColumns);
            rowIdx = writeLabelValueNumeric(sheet, rowIdx, "Total Revenue", dto.getTotalFeeCollected(), numericStyle,
                    usedColumns);
            rowIdx = writeLabelValueNumeric(sheet, rowIdx, "Total Expense", dto.getTotalExpense(), numericStyle,
                    usedColumns);
            rowIdx = writeLabelValueNumeric(sheet, rowIdx, "Net", dto.getNetAmount(), numericStyle, usedColumns);
            rowIdx++;
            rowIdx = writeLabelValueNumeric(sheet, rowIdx, "Cash Revenue", dto.getCashRevenue(), numericStyle,
                    usedColumns);
            rowIdx = writeLabelValueNumeric(sheet, rowIdx, "Cash Expense", dto.getCashExpense(), numericStyle,
                    usedColumns);
            rowIdx = writeLabelValueNumeric(sheet, rowIdx, "Net Cash", dto.getNetCash(), numericStyle, usedColumns);
            rowIdx++;
            rowIdx = writeLabelValueNumeric(sheet, rowIdx, "Bank Revenue", dto.getBankRevenue(), numericStyle,
                    usedColumns);
            rowIdx = writeLabelValueNumeric(sheet, rowIdx, "Bank Expense", dto.getBankExpense(), numericStyle,
                    usedColumns);
            rowIdx = writeLabelValueNumeric(sheet, rowIdx, "Net Bank", dto.getNetBank(), numericStyle, usedColumns);

            rowIdx += 2;
            Row h1 = sheet.createRow(rowIdx++);
            createHeaderCell(h1, 0, "Fee Type", headerStyle, usedColumns);
            createHeaderCell(h1, 1, "Principal", headerStyle, usedColumns);
            createHeaderCell(h1, 2, "Late Fee", headerStyle, usedColumns);
            createHeaderCell(h1, 3, "Total", headerStyle, usedColumns);
            for (FeeTypeHeadSummaryDto row : dto.getHeadWiseCollection()) {
                Row r = sheet.createRow(rowIdx++);
                createTextCell(r, 0, row.getFeeTypeName(), usedColumns);
                createNumericCell(r, 1, row.getTotalPrincipal(), numericStyle, usedColumns);
                createNumericCell(r, 2, row.getTotalLateFee(), numericStyle, usedColumns);
                createNumericCell(r, 3, row.getTotalCollected(), numericStyle, usedColumns);
            }

            rowIdx += 2;
            Row h2 = sheet.createRow(rowIdx++);
            createHeaderCell(h2, 0, "Expense Head", headerStyle, usedColumns);
            createHeaderCell(h2, 1, "Amount", headerStyle, usedColumns);
            createHeaderCell(h2, 2, "Payment Mode", headerStyle, usedColumns);
            for (ExpenseVoucherDto expense : expenses) {
                Row r = sheet.createRow(rowIdx++);
                createTextCell(r, 0, expense.getExpenseHeadName(), usedColumns);
                createNumericCell(r, 1, expense.getAmount(), numericStyle, usedColumns);
                createTextCell(r, 2, expense.getPaymentMode() != null ? expense.getPaymentMode().name() : "",
                        usedColumns);
            }

            autosize(sheet, usedColumns);
            return toBytes(workbook);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export daily cash Excel", e);
        }
    }

    public byte[] exportRangePL(LocalDate start, LocalDate end) {
        FinancialOverviewDto dto = financeOverviewService.getRangeOverview(start, end);
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("P&L Report");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle numericStyle = createNumericStyle(workbook);
            Set<Integer> usedColumns = new LinkedHashSet<>();
            int rowIdx = 0;

            Row header = sheet.createRow(rowIdx++);
            createHeaderCell(header, 0, "Period", headerStyle, usedColumns);
            Row headerValues = sheet.createRow(rowIdx++);
            createTextCell(headerValues, 0, dto.getPeriodName(), usedColumns);

            rowIdx++;
            rowIdx = writeLabelValueNumeric(sheet, rowIdx, "Total Revenue", dto.getTotalRevenue(), numericStyle,
                    usedColumns);
            rowIdx = writeLabelValueNumeric(sheet, rowIdx, "Total Expense", dto.getTotalExpense(), numericStyle,
                    usedColumns);
            rowIdx = writeLabelValueNumeric(sheet, rowIdx, "Net Profit", dto.getNetProfit(), numericStyle, usedColumns);
            rowIdx++;
            rowIdx = writeLabelValueNumeric(sheet, rowIdx, "Cash Revenue", dto.getCashRevenue(), numericStyle,
                    usedColumns);
            rowIdx = writeLabelValueNumeric(sheet, rowIdx, "Cash Expense", dto.getCashExpense(), numericStyle,
                    usedColumns);
            rowIdx++;
            rowIdx = writeLabelValueNumeric(sheet, rowIdx, "Bank Revenue", dto.getBankRevenue(), numericStyle,
                    usedColumns);
            rowIdx = writeLabelValueNumeric(sheet, rowIdx, "Bank Expense", dto.getBankExpense(), numericStyle,
                    usedColumns);

            autosize(sheet, usedColumns);
            return toBytes(workbook);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export P&L Excel", e);
        }
    }

    public byte[] exportExpenses(LocalDate date) {
        List<ExpenseVoucherDto> expenses = expenseService.getExpensesByDate(date);
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Expenses");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle numericStyle = createNumericStyle(workbook);
            Set<Integer> usedColumns = new LinkedHashSet<>();
            int rowIdx = 0;

            Row header = sheet.createRow(rowIdx++);
            createHeaderCell(header, 0, "Voucher No", headerStyle, usedColumns);
            createHeaderCell(header, 1, "Date", headerStyle, usedColumns);
            createHeaderCell(header, 2, "Expense Head", headerStyle, usedColumns);
            createHeaderCell(header, 3, "Amount", headerStyle, usedColumns);
            createHeaderCell(header, 4, "Payment Mode", headerStyle, usedColumns);
            createHeaderCell(header, 5, "Created By", headerStyle, usedColumns);

            BigDecimal total = BigDecimal.ZERO;
            for (ExpenseVoucherDto expense : expenses) {
                Row r = sheet.createRow(rowIdx++);
                createTextCell(r, 0, expense.getVoucherNumber(), usedColumns);
                createTextCell(r, 1, expense.getExpenseDate() != null ? expense.getExpenseDate().toString() : "",
                        usedColumns);
                createTextCell(r, 2, expense.getExpenseHeadName(), usedColumns);
                createNumericCell(r, 3, expense.getAmount(), numericStyle, usedColumns);
                createTextCell(r, 4, expense.getPaymentMode() != null ? expense.getPaymentMode().name() : "",
                        usedColumns);
                createTextCell(r, 5, expense.getCreatedBy() != null ? String.valueOf(expense.getCreatedBy()) : "",
                        usedColumns);
                total = total.add(nz(expense.getAmount()));
            }

            Row totalRow = sheet.createRow(rowIdx);
            createTextCell(totalRow, 2, "Total", usedColumns);
            createNumericCell(totalRow, 3, total, numericStyle, usedColumns);

            autosize(sheet, usedColumns);
            return toBytes(workbook);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export expenses Excel", e);
        }
    }

    private int writeLabelValueDate(Sheet sheet, int rowIdx, String label, LocalDate value, Set<Integer> usedColumns) {
        Row row = sheet.createRow(rowIdx);
        createTextCell(row, 0, label, usedColumns);
        createTextCell(row, 1, value != null ? value.toString() : "", usedColumns);
        return rowIdx + 1;
    }

    private int writeLabelValueNumeric(Sheet sheet, int rowIdx, String label, BigDecimal value, CellStyle numericStyle,
            Set<Integer> usedColumns) {
        Row row = sheet.createRow(rowIdx);
        createTextCell(row, 0, label, usedColumns);
        createNumericCell(row, 1, value, numericStyle, usedColumns);
        return rowIdx + 1;
    }

    private void createHeaderCell(Row row, int col, String value, CellStyle style, Set<Integer> usedColumns) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
        usedColumns.add(col);
    }

    private void createTextCell(Row row, int col, String value, Set<Integer> usedColumns) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        usedColumns.add(col);
    }

    private void createNumericCell(Row row, int col, BigDecimal value, CellStyle numericStyle,
            Set<Integer> usedColumns) {
        Cell cell = row.createCell(col);
        cell.setCellValue(nz(value).doubleValue());
        cell.setCellStyle(numericStyle);
        usedColumns.add(col);
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createNumericStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        return style;
    }

    private void autosize(Sheet sheet, Set<Integer> cols) {
        for (Integer col : cols) {
            sheet.autoSizeColumn(col);
        }
    }

    private byte[] toBytes(XSSFWorkbook workbook) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
