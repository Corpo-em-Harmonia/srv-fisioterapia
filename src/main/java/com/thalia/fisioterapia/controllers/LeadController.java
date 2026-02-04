package com.thalia.fisioterapia.controllers;

import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.dto.CriarLeadRequest;
import com.thalia.fisioterapia.services.LeadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/leads")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }



    @PostMapping
    public ResponseEntity<Lead> criaLead (@RequestBody CriarLeadRequest request) {
        return ResponseEntity.ok(leadService.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<Lead>> allLeads () {
        return ResponseEntity.ok(leadService.allLead());
    }
}
