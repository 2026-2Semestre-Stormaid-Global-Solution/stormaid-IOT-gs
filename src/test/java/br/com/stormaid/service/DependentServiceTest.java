package br.com.stormaid.service;

import br.com.stormaid.dto.request.AddressRequestDto;
import br.com.stormaid.dto.request.DependentRequest;
import br.com.stormaid.dto.request.VulnerabilityRequestDto;
import br.com.stormaid.embeddable.Address;
import br.com.stormaid.entity.CitizenProfile;
import br.com.stormaid.entity.Dependent;
import br.com.stormaid.entity.User;
import br.com.stormaid.enums.MobilityStatus;
import br.com.stormaid.enums.RelationshipType;
import br.com.stormaid.exception.BadRequestException;
import br.com.stormaid.exception.ForbiddenException;
import br.com.stormaid.exception.ResourceNotFoundException;
import br.com.stormaid.repository.CitizenProfileRepository;
import br.com.stormaid.repository.DependentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DependentServiceTest {

    @Mock
    private DependentRepository dependentRepository;

    @Mock
    private CitizenProfileRepository citizenProfileRepository;

    @InjectMocks
    private DependentService dependentService;

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@stormaid.example");
        return user;
    }

    @Test
    void shouldCreateDependentCopyingGuardianAddressWhenSameAddressIsDefault() {
        User guardian = buildUser(1L);

        Address guardianAddress = new Address();
        guardianAddress.setPostalCode("01310-100");
        guardianAddress.setStreet("Avenida Paulista");
        guardianAddress.setNumber("1000");
        guardianAddress.setCity("Sao Paulo");
        guardianAddress.setState("sp");

        CitizenProfile profile = new CitizenProfile();
        profile.setUser(guardian);
        profile.setAddress(guardianAddress);

        when(citizenProfileRepository.findByUser(guardian)).thenReturn(Optional.of(profile));
        when(dependentRepository.save(any(Dependent.class))).thenAnswer(inv -> inv.getArgument(0));

        DependentRequest request = new DependentRequest();
        request.setFullName("  Maria Silva  ");
        request.setRelationship("CHILD");
        request.setBirthDate(LocalDate.of(2018, 5, 10));

        Dependent created = dependentService.createDependent(guardian, request);

        assertEquals("Maria Silva", created.getFullName());
        assertEquals(RelationshipType.CHILD, created.getRelationship());
        assertEquals(Boolean.TRUE, created.getSameAddressAsGuardian());
        assertEquals(Boolean.TRUE, created.getActive());
        assertEquals("01310-100", created.getAddress().getPostalCode());
        assertEquals("Avenida Paulista", created.getAddress().getStreet());
        assertEquals(MobilityStatus.AUTONOMOUS, created.getVulnerability().getMobilityStatus());
        verify(dependentRepository).save(any(Dependent.class));
    }

    @Test
    void shouldThrowBadRequestWhenRelationshipIsInvalid() {
        User guardian = buildUser(1L);
        DependentRequest request = new DependentRequest();
        request.setFullName("Joao Silva");
        request.setRelationship("INVALID_REL");

        assertThrows(BadRequestException.class, () -> dependentService.createDependent(guardian, request));
        verify(dependentRepository, never()).save(any());
    }

    @Test
    void shouldThrowForbiddenWhenGuardianDoesNotOwnDependent() {
        User owner = buildUser(1L);
        User otherUser = buildUser(2L);

        Dependent dependent = new Dependent();
        dependent.setId(10L);
        dependent.setGuardian(owner);
        dependent.setActive(true);

        when(dependentRepository.findById(10L)).thenReturn(Optional.of(dependent));

        assertThrows(ForbiddenException.class, () -> dependentService.findByIdAndGuardian(10L, otherUser));
    }

    @Test
    void shouldThrowNotFoundWhenDependentIsInactive() {
        User guardian = buildUser(1L);

        Dependent dependent = new Dependent();
        dependent.setId(10L);
        dependent.setGuardian(guardian);
        dependent.setActive(false);

        when(dependentRepository.findById(10L)).thenReturn(Optional.of(dependent));

        assertThrows(ResourceNotFoundException.class, () -> dependentService.findByIdAndGuardian(10L, guardian));
    }

    @Test
    void shouldUpdateAndSoftDeleteDependent() {
        User guardian = buildUser(1L);

        Dependent dependent = new Dependent();
        dependent.setId(10L);
        dependent.setGuardian(guardian);
        dependent.setFullName("Antigo Nome");
        dependent.setRelationship(RelationshipType.CHILD);
        dependent.setSameAddressAsGuardian(false);
        dependent.setAddress(new Address());
        dependent.setActive(true);

        when(dependentRepository.findById(10L)).thenReturn(Optional.of(dependent));
        when(dependentRepository.save(any(Dependent.class))).thenAnswer(inv -> inv.getArgument(0));

        AddressRequestDto addressDto = new AddressRequestDto();
        addressDto.setCity("Sao Paulo");
        addressDto.setState("sp");

        VulnerabilityRequestDto vulnDto = new VulnerabilityRequestDto();
        vulnDto.setMobilityStatus(MobilityStatus.WHEELCHAIR_USER);
        vulnDto.setEquipmentDescription("   ");

        DependentRequest updateRequest = new DependentRequest();
        updateRequest.setFullName("Novo Nome");
        updateRequest.setRelationship("PARENT");
        updateRequest.setBirthDate(LocalDate.of(1950, 1, 15));
        updateRequest.setSameAddressAsGuardian(false);
        updateRequest.setAddress(addressDto);
        updateRequest.setVulnerability(vulnDto);

        Dependent updated = dependentService.updateDependent(10L, guardian, updateRequest);
        assertEquals("Novo Nome", updated.getFullName());
        assertEquals(RelationshipType.PARENT, updated.getRelationship());
        assertEquals("SP", updated.getAddress().getState());
        assertEquals(MobilityStatus.WHEELCHAIR_USER, updated.getVulnerability().getMobilityStatus());
        assertNull(updated.getVulnerability().getEquipmentDescription());

        dependentService.deleteDependent(10L, guardian);
        assertNotEquals(Boolean.TRUE, dependent.getActive());
        verify(dependentRepository, times(2)).save(dependent);
    }
}