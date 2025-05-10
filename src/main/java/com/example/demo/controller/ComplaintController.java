package com.example.demo.controller;


import com.example.demo.dto.ComplaintRequest;
import com.example.demo.dto.ContentUpdateRequest;
import com.example.demo.model.Complaint;
import com.example.demo.service.ComplaintService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/complaints")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ComplaintController {
    private final ComplaintService service;

    @PostMapping
    public ResponseEntity<Complaint> createComplaint(
            @Valid @RequestBody ComplaintRequest complaintRequest, HttpServletRequest httpServletRequest) {
        String ip = getClientIp(httpServletRequest);
        Complaint saved = service.addOrIncrement(
                complaintRequest.productId(),
                complaintRequest.content(),
                complaintRequest.reporter(),
                ip);
        log.info("New complaint with id {} saved successfully!", saved.getId());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Complaint> updateComplaint(
            @PathVariable Long id,
            @Valid @RequestBody ContentUpdateRequest contentUpdateRequest) {
        Complaint updated = service.updateContent(id, contentUpdateRequest.content());
        return ResponseEntity.ok(updated);
    }

    @GetMapping
    public List<Complaint> getAllComplaints() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Complaint getCompliant(@PathVariable Long id) {
        return service.findById(id);
    }

    public static String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}