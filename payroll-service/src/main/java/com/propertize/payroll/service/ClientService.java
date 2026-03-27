package com.propertize.payroll.service;

import com.propertize.payroll.entity.Client;
import com.propertize.payroll.enums.ClientStatusEnum;
import com.propertize.payroll.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientService {
    private final ClientRepository clientRepository;

    public Page<Client> getAllClients(Pageable pageable) {
        return clientRepository.findAll(pageable);
    }

    public Page<Client> getClientsByStatus(ClientStatusEnum status, Pageable pageable) {
        return clientRepository.findByStatus(status, pageable);
    }

    @Cacheable(value = "clients", key = "#id")
    public Client getClientById(UUID id) {
        return clientRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Client not found with id: " + id));
    }

    @Transactional
    @CacheEvict(value = "clients", allEntries = true)
    public Client createClient(Client client) {
        if (clientRepository.existsByTaxId(client.getTaxId())) {
            throw new IllegalArgumentException("Client already exists with tax ID: " + client.getTaxId());
        }
        return clientRepository.save(client);
    }

    @Transactional
    @CachePut(value = "clients", key = "#id")
    public Client updateClient(UUID id, Client clientDetails) {
        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Client not found with id: " + id));

        // Enhanced functional approach to update only changed fields
        Optional.ofNullable(clientDetails.getCompanyName()).ifPresent(client::setCompanyName);

        // Special handling for taxId with validation
        Optional.ofNullable(clientDetails.getTaxId())
            .filter(taxId -> !taxId.equals(client.getTaxId()))
            .ifPresent(taxId -> {
                if (clientRepository.existsByTaxId(taxId)) {
                    throw new IllegalArgumentException("Client already exists with tax ID: " + taxId);
                }
                client.setTaxId(taxId);
            });

        Optional.ofNullable(clientDetails.getCompanyAddress()).ifPresent(client::setCompanyAddress);
        Optional.ofNullable(clientDetails.getContactInfo()).ifPresent(client::setContactInfo);
        Optional.ofNullable(clientDetails.getStatus()).ifPresent(client::setStatus);
        Optional.ofNullable(clientDetails.getPayrollSchedule()).ifPresent(client::setPayrollSchedule);
        Optional.ofNullable(clientDetails.getIndustry()).ifPresent(client::setIndustry);
        Optional.ofNullable(clientDetails.getEmployeeCount()).ifPresent(client::setEmployeeCount);

        return client; // No need to call save() - managed entity will auto-update
    }

    @Transactional
    @CacheEvict(value = "clients", key = "#id")
    public void deleteClient(UUID id) {
        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Client not found with id: " + id));
        client.setStatus(ClientStatusEnum.INACTIVE);
        // No need to call save() - managed entity will auto-update
    }
}
