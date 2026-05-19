package com.taskapp.task_scheduler.controller;

import com.taskapp.task_scheduler.service.ExcelExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payrolls")
@RequiredArgsConstructor
public class PayrollExportController {

    private final ExcelExportService excelExportService;

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadExcel(@PathVariable Long id) {
        
        // Llamamos al nuevo servicio de Excel
        byte[] excelData = excelExportService.generateExcelForPayroll(id);

        // Configuramos la respuesta para descargar un archivo .xlsx real
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Matriz_Turnos_Pasteleria.xlsx");
        
        // Este es el tipo MIME oficial para archivos de Microsoft Excel
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }
}