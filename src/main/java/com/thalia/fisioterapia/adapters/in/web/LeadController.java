package com.thalia.fisioterapia.adapters.in.web;

import com.thalia.fisioterapia.adapters.in.dto.CriarLeadRequestDTO;
import com.thalia.fisioterapia.adapters.in.dto.LeadResponseDTO;
import com.thalia.fisioterapia.application.ports.in.CriarLeadCommand;
import com.thalia.fisioterapia.application.ports.in.CriarLeadUseCase;
import com.thalia.fisioterapia.domain.lead.Lead;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/leads")
public class LeadController {

    private final CriarLeadUseCase useCase;

    public LeadController(CriarLeadUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<LeadResponseDTO> criar(
            @RequestBody CriarLeadRequestDTO dto
    ) {
        Lead lead = useCase.criar(
                new CriarLeadCommand(
                        dto.nome(),
                        dto.sobrenome(),
                        dto.email(),
                        dto.celular()
                )
        );

        return ResponseEntity.ok(
                new LeadResponseDTO(
                        lead.getId().getValue(),
                        lead.getNome(),
                        lead.getEmail()
                )
        );
    }

}
