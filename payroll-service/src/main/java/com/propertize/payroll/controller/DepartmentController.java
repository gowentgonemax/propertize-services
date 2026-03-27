package com.propertize.payroll.controller;

import com.propertize.payroll.dto.department.request.DepartmentCreateRequest;
import com.propertize.payroll.dto.department.response.DepartmentResponse;
import com.propertize.payroll.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Slf4j
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(
            @Valid @RequestBody DepartmentCreateRequest request) {
        log.info("Creating department: {}", request.getDepartmentCode());
        DepartmentResponse response = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getDepartment(@PathVariable UUID id) {
        return ResponseEntity.ok(departmentService.getDepartment(id));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<Page<DepartmentResponse>> getAllDepartments(
            @PathVariable UUID clientId,
            Pageable pageable) {
        return ResponseEntity.ok(departmentService.getAllDepartments(clientId, pageable));
    }

    @GetMapping("/client/{clientId}/active")
    public ResponseEntity<List<DepartmentResponse>> getActiveDepartments(
            @PathVariable UUID clientId) {
        return ResponseEntity.ok(departmentService.getActiveDepartments(clientId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable UUID id,
            @Valid @RequestBody DepartmentCreateRequest request) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateDepartment(@PathVariable UUID id) {
        departmentService.deactivateDepartment(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable UUID id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}
