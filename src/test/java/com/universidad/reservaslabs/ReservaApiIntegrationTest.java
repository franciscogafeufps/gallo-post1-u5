package com.universidad.reservaslabs;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReservaApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LaboratorioRepository labRepo;

    @Autowired
    private ReservaRepository reservaRepo;

    private Laboratorio lab;

    @BeforeEach
    void setUp() {
        reservaRepo.deleteAll();
        labRepo.deleteAll();
        lab = labRepo.save(new Laboratorio(null, "Lab. Cómputo 3", "Bloque B, piso 2", 36, "COMPUTO"));
    }

    @Test
    void crearReservaValidaRetorna201() throws Exception {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fin = inicio.plusHours(2);

        Reserva reserva = new Reserva(null, lab, "Ana Torres", "ana@udes.edu.co", inicio, fin, "Práctica SO", EstadoReserva.PENDIENTE);

        mockMvc.perform(post("/api/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reserva)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.estado", is("CONFIRMADA")));
    }

    @Test
    void crearReservaSolapadaRetorna409() throws Exception {
        LocalDateTime inicio1 = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fin1 = inicio1.plusHours(2);
        reservaRepo.save(new Reserva(null, lab, "Ana Torres", "ana@udes.edu.co", inicio1, fin1, "Práctica SO", EstadoReserva.CONFIRMADA));

        // Intento de solapamiento: 09:00 a 11:00
        LocalDateTime inicio2 = inicio1.plusHours(1);
        LocalDateTime fin2 = inicio2.plusHours(2);
        Reserva solapada = new Reserva(null, lab, "Luis Gómez", "luis@udes.edu.co", inicio2, fin2, "Taller Redes", EstadoReserva.PENDIENTE);

        mockMvc.perform(post("/api/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(solapada)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("ya tiene una reserva")));
    }

    @Test
    void crearReservaFueraDeHorarioRetorna409() throws Exception {
        // Horario fuera de atención: 05:00 a 07:00
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(5).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fin = inicio.plusHours(2);
        Reserva fueraDeHorario = new Reserva(null, lab, "Carlos Ruiz", "carlos@udes.edu.co", inicio, fin, "Madrugón", EstadoReserva.PENDIENTE);

        mockMvc.perform(post("/api/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fueraDeHorario)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("horario de atención")));
    }

    @Test
    void cancelarReservaExitosamente() throws Exception {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fin = inicio.plusHours(1);
        Reserva r = reservaRepo.save(new Reserva(null, lab, "Marta Mora", "marta@udes.edu.co", inicio, fin, "Reunión", EstadoReserva.CONFIRMADA));

        mockMvc.perform(delete("/api/reservas/" + r.getId()))
                .andExpect(status().isNoContent());

        Reserva cancelada = reservaRepo.findById(r.getId()).orElseThrow();
        assertEquals(EstadoReserva.CANCELADA, cancelada.getEstado());
    }
}