package com.thalia.fisioterapia.controllers;

import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.dto.CriarLeadRequest;
import com.thalia.fisioterapia.services.CriarLeadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/leads")
public class CriarLeadController {

    private final CriarLeadService leadService;

    public CriarLeadController(CriarLeadService leadService) {
        this.leadService = leadService;
    }



    @PostMapping
    public ResponseEntity<Lead> criaLead (@RequestBody CriarLeadRequest request) {
        return ResponseEntity.ok(leadService.criar(request));
    }
}
