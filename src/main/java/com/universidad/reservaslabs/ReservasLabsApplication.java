package com.universidad.reservaslabs;

import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.service.ReservaService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

@SpringBootApplication
public class ReservasLabsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReservasLabsApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(LaboratorioRepository labRepo, ReservaService reservaService) {
        return args -> {
            if (labRepo.count() == 0) {
                Laboratorio lab1 = labRepo.save(new Laboratorio(null, "Laboratorio de Cómputo 1", "Bloque A - Piso 2", 35, "COMPUTO"));
                Laboratorio lab2 = labRepo.save(new Laboratorio(null, "Laboratorio de Redes y Conectividad", "Bloque B - Piso 1", 25, "REDES"));
                Laboratorio lab3 = labRepo.save(new Laboratorio(null, "Laboratorio de Inteligencia Artificial", "Bloque C - Piso 3", 20, "INVESTIGACION"));

                LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);
                reservaService.crear(new Reserva(null, lab1, "Carlos Pérez", "carlos.perez@ufps.edu.co", inicio, inicio.plusHours(2), "Clase Algoritmos y Patrones", null));
            }
        };
    }
}