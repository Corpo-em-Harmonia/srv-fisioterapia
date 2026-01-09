package com.thalia.fisioterapia.application.ports.out;

import com.thalia.fisioterapia.model.Paciente;
import org.springframework.stereotype.Repository;


public interface PacienteRepositoryPort {

    Paciente salvar(Paciente paciente);
}
