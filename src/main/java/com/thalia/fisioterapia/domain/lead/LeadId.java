package com.thalia.fisioterapia.domain.lead;

import lombok.Data;

import java.util.UUID;

@Data
public class LeadId {
    private final UUID value;

    private LeadId(UUID value) {
        this.value = value;
    }

    public static LeadId generate() {
        return new LeadId(UUID.randomUUID());
    }

    public static LeadId from(UUID value) {
        return new LeadId(value);
    }


}

