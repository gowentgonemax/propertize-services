package com.propertize.payroll.controller;

import com.propertize.payroll.entity.Client;
import com.propertize.payroll.enums.ClientStatusEnum;
import com.propertize.payroll.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {
    private final ClientService clientService;

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_OVERSIGHT','PLATFORM_ADMIN','ORGANIZATION_OWNER','ORGANIZATION_ADMIN','ACCOUNTANT','PAYROLL_MANAGER')")
    public ResponseEntity<Page<Client>> getAllClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) ClientStatusEnum status) {
        PageRequest pageRequest = PageRequest.of(page, limit);
        Page<Client> clients = status != null ? clientService.getClientsByStatus(status, pageRequest)
                : clientService.getAllClients(pageRequest);
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_OVERSIGHT','PLATFORM_ADMIN','ORGANIZATION_OWNER','ORGANIZATION_ADMIN','ACCOUNTANT','PAYROLL_MANAGER')")
    public ResponseEntity<Client> getClientById(@PathVariable UUID id) {
        return ResponseEntity.ok(clientService.getClientById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ORGANIZATION_OWNER','ORGANIZATION_ADMIN')")
    public ResponseEntity<Client> createClient(@Valid @RequestBody Client client) {
        Client created = clientService.createClient(client);
        return ResponseEntity.created(URI.create("/clients/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ORGANIZATION_OWNER','ORGANIZATION_ADMIN')")
    public ResponseEntity<Client> updateClient(@PathVariable UUID id, @Valid @RequestBody Client client) {
        return ResponseEntity.ok(clientService.updateClient(id, client));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<Void> deleteClient(@PathVariable UUID id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }
}
