package com.propertize.payroll.service;

import com.propertize.payroll.dto.department.request.DepartmentCreateRequest;
import com.propertize.payroll.dto.department.response.DepartmentResponse;
import com.propertize.payroll.entity.Client;
import com.propertize.payroll.entity.DepartmentEntity;
import com.propertize.payroll.entity.EmployeeEntity;
import com.propertize.payroll.entity.embedded.ContactInfo;
import com.propertize.payroll.repository.ClientRepository;
import com.propertize.payroll.repository.DepartmentRepository;
import com.propertize.payroll.repository.EmployeeEntityRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    DepartmentRepository departmentRepository;
    @Mock
    ClientRepository clientRepository;
    @Mock
    EmployeeEntityRepository employeeRepository;

    @InjectMocks
    DepartmentService departmentService;

    UUID clientId;
    UUID departmentId;
    UUID managerId;
    UUID parentDeptId;
    Client client;
    EmployeeEntity manager;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        departmentId = UUID.randomUUID();
        managerId = UUID.randomUUID();
        parentDeptId = UUID.randomUUID();

        client = new Client();
        client.setId(clientId);
        client.setCompanyName("Propertize Corp");

        ContactInfo contactInfo = new ContactInfo();
        contactInfo.setEmail("manager@propertize.com");

        manager = new EmployeeEntity();
        manager.setId(managerId);
        manager.setFirstName("Alice");
        manager.setLastName("Smith");
        manager.setContactInfo(contactInfo);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private DepartmentEntity buildDepartment() {
        DepartmentEntity dept = new DepartmentEntity();
        dept.setId(departmentId);
        dept.setClient(client);
        dept.setName("Engineering");
        dept.setDepartmentCode("ENG-01");
        dept.setDescription("Engineering team");
        dept.setCostCenter("CC-200");
        dept.setGlAccountCode("GL-400");
        dept.setDefaultWorkLocation("New York");
        dept.setStateCode("NY");
        dept.setIsActive(true);
        dept.setCreatedAt(LocalDateTime.now());
        dept.setUpdatedAt(LocalDateTime.now());
        return dept;
    }

    private DepartmentCreateRequest buildCreateRequest() {
        return DepartmentCreateRequest.builder()
                .clientId(clientId)
                .name("Engineering")
                .departmentCode("ENG-01")
                .description("Engineering team")
                .costCenter("CC-200")
                .glAccountCode("GL-400")
                .defaultWorkLocation("New York")
                .stateCode("NY")
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // createDepartment
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void createDepartment_happyPath_returnsMappedResponse() {
        DepartmentCreateRequest request = buildCreateRequest();
        DepartmentEntity savedDept = buildDepartment();

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(departmentRepository.existsByClientIdAndDepartmentCode(clientId, "ENG-01")).thenReturn(false);
        when(departmentRepository.save(any(DepartmentEntity.class))).thenReturn(savedDept);

        DepartmentResponse result = departmentService.createDepartment(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(departmentId);
        assertThat(result.getName()).isEqualTo("Engineering");
        assertThat(result.getDepartmentCode()).isEqualTo("ENG-01");
        assertThat(result.getIsActive()).isTrue();
        verify(departmentRepository).save(any(DepartmentEntity.class));
    }

    @Test
    void createDepartment_setsIsActiveTrue() {
        DepartmentCreateRequest request = buildCreateRequest();
        DepartmentEntity savedDept = buildDepartment();

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(departmentRepository.existsByClientIdAndDepartmentCode(clientId, "ENG-01")).thenReturn(false);
        when(departmentRepository.save(any(DepartmentEntity.class))).thenReturn(savedDept);

        departmentService.createDepartment(request);

        ArgumentCaptor<DepartmentEntity> captor = ArgumentCaptor.forClass(DepartmentEntity.class);
        verify(departmentRepository).save(captor.capture());
        assertThat(captor.getValue().getIsActive()).isTrue();
    }

    @Test
    void createDepartment_withParentDepartment_setsParent() {
        DepartmentEntity parentDept = buildDepartment();
        parentDept.setId(parentDeptId);
        parentDept.setDepartmentCode("ENG-PARENT");

        DepartmentCreateRequest request = DepartmentCreateRequest.builder()
                .clientId(clientId)
                .name("Engineering Sub")
                .departmentCode("ENG-SUB")
                .parentDepartmentId(parentDeptId)
                .build();

        DepartmentEntity savedDept = buildDepartment();
        savedDept.setParentDepartment(parentDept);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(departmentRepository.existsByClientIdAndDepartmentCode(clientId, "ENG-SUB")).thenReturn(false);
        when(departmentRepository.findById(parentDeptId)).thenReturn(Optional.of(parentDept));
        when(departmentRepository.save(any(DepartmentEntity.class))).thenReturn(savedDept);

        DepartmentResponse result = departmentService.createDepartment(request);

        assertThat(result.getParentDepartment()).isNotNull();
        assertThat(result.getParentDepartment().getId()).isEqualTo(parentDeptId);
    }

    @Test
    void createDepartment_withManager_setsManager() {
        DepartmentCreateRequest request = DepartmentCreateRequest.builder()
                .clientId(clientId)
                .name("Engineering")
                .departmentCode("ENG-01")
                .managerId(managerId)
                .build();

        DepartmentEntity savedDept = buildDepartment();
        savedDept.setManager(manager);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(departmentRepository.existsByClientIdAndDepartmentCode(clientId, "ENG-01")).thenReturn(false);
        when(employeeRepository.findById(managerId)).thenReturn(Optional.of(manager));
        when(departmentRepository.save(any(DepartmentEntity.class))).thenReturn(savedDept);

        DepartmentResponse result = departmentService.createDepartment(request);

        assertThat(result.getManager()).isNotNull();
        assertThat(result.getManager().getId()).isEqualTo(managerId);
        assertThat(result.getManager().getFirstName()).isEqualTo("Alice");
        assertThat(result.getManager().getEmail()).isEqualTo("manager@propertize.com");
    }

    @Test
    void createDepartment_clientNotFound_throws() {
        DepartmentCreateRequest request = buildCreateRequest();
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(clientId.toString());

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void createDepartment_duplicateDepartmentCode_throws() {
        DepartmentCreateRequest request = buildCreateRequest();
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(departmentRepository.existsByClientIdAndDepartmentCode(clientId, "ENG-01")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ENG-01");

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void createDepartment_parentDepartmentNotFound_throws() {
        DepartmentCreateRequest request = DepartmentCreateRequest.builder()
                .clientId(clientId)
                .name("Sub")
                .departmentCode("SUB-01")
                .parentDepartmentId(parentDeptId)
                .build();

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(departmentRepository.existsByClientIdAndDepartmentCode(clientId, "SUB-01")).thenReturn(false);
        when(departmentRepository.findById(parentDeptId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Parent department not found");

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void createDepartment_managerNotFound_throws() {
        DepartmentCreateRequest request = DepartmentCreateRequest.builder()
                .clientId(clientId)
                .name("Engineering")
                .departmentCode("ENG-01")
                .managerId(managerId)
                .build();

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(departmentRepository.existsByClientIdAndDepartmentCode(clientId, "ENG-01")).thenReturn(false);
        when(employeeRepository.findById(managerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Manager not found");

        verify(departmentRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getDepartment
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getDepartment_returnsResponse() {
        DepartmentEntity dept = buildDepartment();
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(dept));

        DepartmentResponse result = departmentService.getDepartment(departmentId);

        assertThat(result.getId()).isEqualTo(departmentId);
        assertThat(result.getName()).isEqualTo("Engineering");
        assertThat(result.getDepartmentCode()).isEqualTo("ENG-01");
        assertThat(result.getCostCenter()).isEqualTo("CC-200");
        assertThat(result.getStateCode()).isEqualTo("NY");
    }

    @Test
    void getDepartment_notFound_throws() {
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.getDepartment(departmentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(departmentId.toString());
    }

    @Test
    void getDepartment_withManagerAndNoContactInfo_managerEmailIsNull() {
        EmployeeEntity managerNoContact = new EmployeeEntity();
        managerNoContact.setId(managerId);
        managerNoContact.setFirstName("Bob");
        managerNoContact.setLastName("Jones");
        managerNoContact.setContactInfo(null);

        DepartmentEntity dept = buildDepartment();
        dept.setManager(managerNoContact);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(dept));

        DepartmentResponse result = departmentService.getDepartment(departmentId);

        assertThat(result.getManager()).isNotNull();
        assertThat(result.getManager().getEmail()).isNull();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getAllDepartments
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getAllDepartments_returnsPage() {
        DepartmentEntity dept = buildDepartment();
        Page<DepartmentEntity> page = new PageImpl<>(List.of(dept));
        Pageable pageable = PageRequest.of(0, 10);

        when(departmentRepository.findByClientId(eq(clientId), any(Pageable.class))).thenReturn(page);

        Page<DepartmentResponse> result = departmentService.getAllDepartments(clientId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Engineering");
    }

    @Test
    void getAllDepartments_emptyPage_returnsEmpty() {
        Page<DepartmentEntity> emptyPage = new PageImpl<>(List.of());
        when(departmentRepository.findByClientId(eq(clientId), any(Pageable.class))).thenReturn(emptyPage);

        Page<DepartmentResponse> result = departmentService.getAllDepartments(clientId, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getActiveDepartments
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getActiveDepartments_returnsOnlyActiveDepartments() {
        DepartmentEntity activeDept = buildDepartment();
        when(departmentRepository.findByClientIdAndIsActiveTrue(clientId)).thenReturn(List.of(activeDept));

        List<DepartmentResponse> result = departmentService.getActiveDepartments(clientId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsActive()).isTrue();
    }

    @Test
    void getActiveDepartments_noActive_returnsEmptyList() {
        when(departmentRepository.findByClientIdAndIsActiveTrue(clientId)).thenReturn(List.of());

        List<DepartmentResponse> result = departmentService.getActiveDepartments(clientId);

        assertThat(result).isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // updateDepartment
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void updateDepartment_updatesFieldsAndReturnsResponse() {
        DepartmentEntity existingDept = buildDepartment();
        DepartmentCreateRequest updateRequest = DepartmentCreateRequest.builder()
                .clientId(clientId)
                .name("Engineering Updated")
                .departmentCode("ENG-01")
                .description("Updated description")
                .costCenter("CC-300")
                .glAccountCode("GL-500")
                .defaultWorkLocation("Boston")
                .stateCode("MA")
                .build();

        DepartmentEntity updatedDept = buildDepartment();
        updatedDept.setName("Engineering Updated");
        updatedDept.setCostCenter("CC-300");
        updatedDept.setStateCode("MA");

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDept));
        when(departmentRepository.save(any(DepartmentEntity.class))).thenReturn(updatedDept);

        DepartmentResponse result = departmentService.updateDepartment(departmentId, updateRequest);

        assertThat(result).isNotNull();
        verify(departmentRepository).save(existingDept);
    }

    @Test
    void updateDepartment_notFound_throws() {
        DepartmentCreateRequest updateRequest = buildCreateRequest();
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.updateDepartment(departmentId, updateRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(departmentId.toString());

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void updateDepartment_updatesAllMutableFields() {
        DepartmentEntity existingDept = buildDepartment();
        DepartmentCreateRequest updateRequest = DepartmentCreateRequest.builder()
                .clientId(clientId)
                .name("Ops")
                .departmentCode("OPS-01")
                .description("Operations")
                .costCenter("CC-999")
                .glAccountCode("GL-999")
                .defaultWorkLocation("Chicago")
                .stateCode("IL")
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDept));
        when(departmentRepository.save(any(DepartmentEntity.class))).thenReturn(existingDept);

        departmentService.updateDepartment(departmentId, updateRequest);

        ArgumentCaptor<DepartmentEntity> captor = ArgumentCaptor.forClass(DepartmentEntity.class);
        verify(departmentRepository).save(captor.capture());
        DepartmentEntity saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Ops");
        assertThat(saved.getDescription()).isEqualTo("Operations");
        assertThat(saved.getCostCenter()).isEqualTo("CC-999");
        assertThat(saved.getGlAccountCode()).isEqualTo("GL-999");
        assertThat(saved.getDefaultWorkLocation()).isEqualTo("Chicago");
        assertThat(saved.getStateCode()).isEqualTo("IL");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // deactivateDepartment
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void deactivateDepartment_setsIsActiveFalse() {
        DepartmentEntity dept = buildDepartment();
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(dept));
        when(departmentRepository.save(any(DepartmentEntity.class))).thenReturn(dept);

        departmentService.deactivateDepartment(departmentId);

        ArgumentCaptor<DepartmentEntity> captor = ArgumentCaptor.forClass(DepartmentEntity.class);
        verify(departmentRepository).save(captor.capture());
        assertThat(captor.getValue().getIsActive()).isFalse();
    }

    @Test
    void deactivateDepartment_notFound_throws() {
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.deactivateDepartment(departmentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(departmentId.toString());

        verify(departmentRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // deleteDepartment
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void deleteDepartment_existingDepartment_deletesSuccessfully() {
        when(departmentRepository.existsById(departmentId)).thenReturn(true);

        departmentService.deleteDepartment(departmentId);

        verify(departmentRepository).deleteById(departmentId);
    }

    @Test
    void deleteDepartment_notFound_throws() {
        when(departmentRepository.existsById(departmentId)).thenReturn(false);

        assertThatThrownBy(() -> departmentService.deleteDepartment(departmentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(departmentId.toString());

        verify(departmentRepository, never()).deleteById(any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DTO mapping — parent and manager null cases
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getDepartment_noParentAndNoManager_nullFieldsInResponse() {
        DepartmentEntity dept = buildDepartment();
        dept.setParentDepartment(null);
        dept.setManager(null);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(dept));

        DepartmentResponse result = departmentService.getDepartment(departmentId);

        assertThat(result.getParentDepartment()).isNull();
        assertThat(result.getManager()).isNull();
    }

    @Test
    void getDepartment_withParentDepartment_mapsParentSummary() {
        DepartmentEntity parentDept = new DepartmentEntity();
        parentDept.setId(parentDeptId);
        parentDept.setName("Parent Dept");
        parentDept.setDepartmentCode("PAR-01");

        DepartmentEntity dept = buildDepartment();
        dept.setParentDepartment(parentDept);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(dept));

        DepartmentResponse result = departmentService.getDepartment(departmentId);

        assertThat(result.getParentDepartment()).isNotNull();
        assertThat(result.getParentDepartment().getId()).isEqualTo(parentDeptId);
        assertThat(result.getParentDepartment().getName()).isEqualTo("Parent Dept");
        assertThat(result.getParentDepartment().getDepartmentCode()).isEqualTo("PAR-01");
    }
}
