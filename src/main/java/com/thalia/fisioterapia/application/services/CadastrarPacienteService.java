//package com.thalia.fisioterapia.application.services;
//
//import com.thalia.fisioterapia.application.ports.in.CadastrarPacienteUseCase;
//import com.thalia.fisioterapia.application.ports.out.PacienteRepositoryPort;
//import com.thalia.fisioterapia.model.Paciente;
//import org.springframework.stereotype.Service;
//
//@Service
//public class CadastrarPacienteService implements CadastrarPacienteUseCase {
//
//    private final PacienteRepositoryPort repository;
//
//
//    public CadastrarPacienteService(PacienteRepositoryPort repository) {
//        this.repository = repository;
//    }
//
//    @Override
//    public Paciente cadastrar(String nome, String telefone) {
//        Paciente paciente = new Paciente(nome, telefone);
//        return repository.salvar(paciente);
//    }
//}
