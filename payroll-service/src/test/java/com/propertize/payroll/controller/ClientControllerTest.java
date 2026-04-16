package com.propertize.payroll.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertize.payroll.entity.Client;
import com.propertize.payroll.enums.ClientStatusEnum;
import com.propertize.payroll.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ClientController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClientService clientService;

    private UUID clientId;
    private Client client;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        client = new Client();
        client.setId(clientId);
        client.setCompanyName("Acme Corp");
        client.setStatus(ClientStatusEnum.ACTIVE);
    }

    @Test
    void getAllClients_withoutStatus_returnsPage() throws Exception {
        when(clientService.getAllClients(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(client)));

        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(clientId.toString()));
    }

    @Test
    void getAllClients_withStatus_filtersResults() throws Exception {
        when(clientService.getClientsByStatus(eq(ClientStatusEnum.ACTIVE), any()))
                .thenReturn(new PageImpl<>(List.of(client)));

        mockMvc.perform(get("/api/v1/clients").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].companyName").value("Acme Corp"));
    }

    @Test
    void getClientById_returns200() throws Exception {
        when(clientService.getClientById(clientId)).thenReturn(client);

        mockMvc.perform(get("/api/v1/clients/{id}", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clientId.toString()));
    }

    @Test
    void createClient_returns201() throws Exception {
        when(clientService.createClient(any(Client.class))).thenReturn(client);

        mockMvc.perform(post("/api/v1/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(client)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(clientId.toString()));
    }

    @Test
    void updateClient_returns200() throws Exception {
        client.setCompanyName("Acme Updated");
        when(clientService.updateClient(eq(clientId), any(Client.class))).thenReturn(client);

        mockMvc.perform(put("/api/v1/clients/{id}", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Acme Updated"));
    }

    @Test
    void deleteClient_returns204() throws Exception {
        doNothing().when(clientService).deleteClient(clientId);

        mockMvc.perform(delete("/api/v1/clients/{id}", clientId))
                .andExpect(status().isNoContent());
    }
}
