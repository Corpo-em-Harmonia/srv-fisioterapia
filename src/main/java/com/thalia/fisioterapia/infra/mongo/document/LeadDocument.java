package com.thalia.fisioterapia.infra.mongo.document;

import com.thalia.fisioterapia.domain.lead.Lead;
import com.thalia.fisioterapia.domain.lead.LeadId;
import com.thalia.fisioterapia.domain.lead.LeadStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "leads")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeadDocument {

    @Id
    private UUID id;

    private String nome;
    private String sobrenome;
    private String email;
    private String celular;
    private String status;
    private Instant createdAt;

    public static LeadDocument fromDomain(Lead lead) {
        LeadDocument doc = new LeadDocument();
        doc.id = lead.getId().getValue();
        doc.nome = lead.getNome();
        doc.sobrenome = lead.getSobrenome();
        doc.email = lead.getEmail();
        doc.celular = lead.getCelular();
        doc.status = lead.getStatus().name();
        doc.createdAt = Instant.now();
        return doc;
    }

    public Lead toDomain() {
        return new Lead(
                LeadId.from(id),
                nome,
                sobrenome,
                email,
                celular,
                LeadStatus.valueOf(status)
        );
    }
}
