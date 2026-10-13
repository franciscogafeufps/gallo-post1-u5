package com.universidad.reservaslabs;

import com.universidad.reservaslabs.model.EstadoReserva;
import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReservaMvcIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LaboratorioRepository labRepo;

    @Autowired
    private ReservaRepository reservaRepo;

    private Laboratorio lab;

    @BeforeEach
    void setUp() {
        reservaRepo.deleteAll();
        labRepo.deleteAll();
        lab = labRepo.save(new Laboratorio(null, "Lab. Cómputo 1", "Bloque A, piso 1", 30, "COMPUTO"));
    }

    @Test
    void vistaListarReservasRetorna200YModelo() throws Exception {
        mockMvc.perform(get("/reservas"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservas/lista"))
                .andExpect(model().attributeExists("reservas"));
    }

    @Test
    void vistaFormularioNuevaRetorna200YCombos() throws Exception {
        mockMvc.perform(get("/reservas/nueva"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservas/nueva"))
                .andExpect(model().attributeExists("reserva"))
                .andExpect(model().attributeExists("laboratorios"));
    }

    @Test
    void crearReservaWebValidaRedirigeConMensaje() throws Exception {
        LocalDateTime inicio = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fin = inicio.plusHours(1);

        mockMvc.perform(post("/reservas")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("laboratorio.id", String.valueOf(lab.getId()))
                .param("nombreSolicitante", "Carlos Pérez")
                .param("correoSolicitante", "carlos@udes.edu.co")
                .param("inicio", inicio.toString())
                .param("fin", fin.toString())
                .param("motivo", "Clase Algoritmos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservas"))
                .andExpect(flash().attributeExists("mensaje"));
    }

    @Test
    void crearReservaWebSolapadaRedirigeANuevaConError() throws Exception {
        LocalDateTime inicio1 = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fin1 = inicio1.plusHours(2);
        reservaRepo.save(new Reserva(null, lab, "Existente", "existente@udes.edu.co", inicio1, fin1, "Clase", EstadoReserva.CONFIRMADA));

        // Intento solapado de 10:30 a 11:30
        LocalDateTime inicio2 = inicio1.plusMinutes(30);
        LocalDateTime fin2 = inicio2.plusHours(1);

        mockMvc.perform(post("/reservas")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("laboratorio.id", String.valueOf(lab.getId()))
                .param("nombreSolicitante", "Segundo Solicitante")
                .param("correoSolicitante", "segundo@udes.edu.co")
                .param("inicio", inicio2.toString())
                .param("fin", fin2.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservas/nueva"))
                .andExpect(flash().attributeExists("error"));
    }
}