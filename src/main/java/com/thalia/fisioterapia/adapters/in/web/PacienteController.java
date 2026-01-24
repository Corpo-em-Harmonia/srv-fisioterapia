//package com.thalia.fisioterapia.adapters.in.web;
//
//
//import com.thalia.fisioterapia.adapters.in.dto.PacienteDTO;
//import com.thalia.fisioterapia.application.ports.in.CadastrarPacienteUseCase;
//import com.thalia.fisioterapia.model.Paciente;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/pacientes")
//public class PacienteController {
//
//    private final CadastrarPacienteUseCase useCase;
//
//    public PacienteController(CadastrarPacienteUseCase useCase) {
//        this.useCase = useCase;
//    }
//
//    @PostMapping
//    public ResponseEntity<Paciente> cadastrar(@RequestBody PacienteDTO dto) {
//        return ResponseEntity.ok(useCase.cadastrar(dto.nome(), dto.telefone()));
//    }
//}
