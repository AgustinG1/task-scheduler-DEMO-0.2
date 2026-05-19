package com.taskapp.task_scheduler.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.taskapp.task_scheduler.model.*;
import com.taskapp.task_scheduler.repository.*;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private final AssignmentRepository assignmentRepository;
    private final TaskRepository taskRepository;

    public byte[] generateExcelForPayroll(Long payrollId) {
        List<Assignment> assignments = assignmentRepository.findByPayrollId(payrollId);
        
        // Extraer dinámicamente solo las tareas del equipo activo
        List<Task> tasks = assignments.stream()
                .map(Assignment::getTask)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (tasks.isEmpty()) {
            tasks = taskRepository.findAll();
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Matriz de Turnos");
            
            // Forzar a que se muestren las líneas de cuadrícula nativas de Excel
            sheet.setDisplayGridlines(true);

            // --- 1. PALETA DE COLORES Y FUENTES ---
            // Azul Slate Elegante para la cabecera (#2C3E50)
            XSSFColor colorCabecera = new XSSFColor(new java.awt.Color(44, 62, 80), null);
            // Gris sutil para filas intercaladas de datos (#F8F9FA)
            XSSFColor colorCebra = new XSSFColor(new java.awt.Color(248, 249, 250), null);
            
            // Tonos personalizados y sutiles para la columna de Semanas
            XSSFColor colorSemanaNormal = new XSSFColor(new java.awt.Color(235, 241, 245), null); 
            XSSFColor colorSemanaCebra = new XSSFColor(new java.awt.Color(220, 228, 236), null);  

            // Fuente para Cabeceras
            XSSFFont headerFont = (XSSFFont) workbook.createFont();
            headerFont.setFontName("Segoe UI");
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            // Fuente para Datos
            XSSFFont dataFont = (XSSFFont) workbook.createFont();
            dataFont.setFontName("Segoe UI");
            dataFont.setFontHeightInPoints((short) 10);

            // Fuente destacada para la columna de Semanas
            XSSFFont semanaFont = (XSSFFont) workbook.createFont();
            semanaFont.setFontName("Segoe UI");
            semanaFont.setFontHeightInPoints((short) 10);
            semanaFont.setBold(true);
            semanaFont.setColor(colorCabecera); // Reutiliza el tono oscuro de la cabecera para el texto

            // --- 2. CONFIGURACIÓN DE ESTILOS ---
            // Estilo Cabecera
            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            headerStyle.setFillForegroundColor(colorCabecera);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER); 
            setSoftBorders(headerStyle);
            headerStyle.setFont(headerFont);

            // Estilo Fila Datos Normal
            XSSFCellStyle normalStyle = (XSSFCellStyle) workbook.createCellStyle();
            normalStyle.setAlignment(HorizontalAlignment.CENTER);
            normalStyle.setVerticalAlignment(VerticalAlignment.CENTER); 
            setSoftBorders(normalStyle);
            normalStyle.setFont(dataFont);

            // Estilo Fila Datos Cebra (Intercalada)
            XSSFCellStyle zebraStyle = (XSSFCellStyle) workbook.createCellStyle();
            zebraStyle.cloneStyleFrom(normalStyle);
            zebraStyle.setFillForegroundColor(colorCebra);
            zebraStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Estilo Columna Semana Normal
            XSSFCellStyle semanaNormalStyle = (XSSFCellStyle) workbook.createCellStyle();
            semanaNormalStyle.cloneStyleFrom(normalStyle);
            semanaNormalStyle.setFillForegroundColor(colorSemanaNormal);
            semanaNormalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            semanaNormalStyle.setFont(semanaFont);

            // Estilo Columna Semana Cebra
            XSSFCellStyle semanaZebraStyle = (XSSFCellStyle) workbook.createCellStyle();
            semanaZebraStyle.cloneStyleFrom(semanaNormalStyle);
            semanaZebraStyle.setFillForegroundColor(colorSemanaCebra);
            semanaZebraStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // --- 3. CREAR CABECERA ---
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(26); 
            
            int colIdx = 0;
            
            Cell cellSemana = headerRow.createCell(colIdx++);
            cellSemana.setCellValue("Semana");
            cellSemana.setCellStyle(headerStyle);

            for (Task task : tasks) {
                Cell cellTarea = headerRow.createCell(colIdx++);
                cellTarea.setCellValue(task.getName());
                cellTarea.setCellStyle(headerStyle);
            }

            Cell cellRest = headerRow.createCell(colIdx);
            cellRest.setCellValue("En Descanso (REST)");
            cellRest.setCellStyle(headerStyle);

            // --- 4. LLENAR DATOS ---
            int totalWeeks = assignments.stream()
                    .mapToInt(Assignment::getWeekNumber)
                    .max()
                    .orElse(16);

            int rowIdx = 1;
            for (int semana = 1; semana <= totalWeeks; semana++) {
                Row row = sheet.createRow(rowIdx);
                row.setHeightInPoints(20); 
                
                boolean esFilaPar = (rowIdx % 2 == 0);
                CellStyle estiloDatosActual = esFilaPar ? zebraStyle : normalStyle;
                CellStyle estiloSemanaActual = esFilaPar ? semanaZebraStyle : semanaNormalStyle;
                
                final int currentSemana = semana;
                List<Assignment> weekAssignments = assignments.stream()
                        .filter(a -> a.getWeekNumber() == currentSemana)
                        .collect(Collectors.toList());

                int dataColIdx = 0;
                
                // Celda Semana (Usa la paleta de color diferenciada)
                Cell sCell = row.createCell(dataColIdx++);
                sCell.setCellValue("Semana " + semana);
                sCell.setCellStyle(estiloSemanaActual);

                // Celdas de Empleados en las Tareas
                for (Task task : tasks) {
                    String empName = weekAssignments.stream()
                            .filter(a -> a.getTask() != null && a.getTask().getId().equals(task.getId()))
                            .map(a -> a.getEmployee().getName())
                            .findFirst()
                            .orElse("-");
                    
                    Cell cell = row.createCell(dataColIdx++);
                    cell.setCellValue(empName);
                    cell.setCellStyle(estiloDatosActual);
                }

                // Celda de Personal en Descanso
                String descansan = weekAssignments.stream()
                        .filter(a -> a.getStatus() == AssignmentStatus.REST)
                        .map(a -> a.getEmployee().getName())
                        .collect(Collectors.joining(" - "));
                
                Cell restCell = row.createCell(dataColIdx);
                restCell.setCellValue(descansan.isEmpty() ? "-" : descansan);
                restCell.setCellStyle(estiloDatosActual);

                rowIdx++;
            }

            // --- 5. AUTO-AJUSTE CON MARGEN DE SEGURIDAD ---
            int totalColumns = tasks.size() + 2;
            for (int i = 0; i < totalColumns; i++) {
                sheet.autoSizeColumn(i);
                int widthActual = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, widthActual + 1200); 
            }

            workbook.write(out);
            return out.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el archivo Excel", e);
        }
    }

    private void setSoftBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        
        style.setBorderTop(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        
        style.setBorderLeft(BorderStyle.THIN);
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        
        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    }
}