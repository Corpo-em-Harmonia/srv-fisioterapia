package com.thalia.fisioterapia.application.ports.in;

import com.thalia.fisioterapia.model.Paciente;

public interface CadastrarPacienteUseCase {

    Paciente cadastrar(String nome, String telefone);
}
