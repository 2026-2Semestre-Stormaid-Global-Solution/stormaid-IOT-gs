package br.com.stormaid.service;

import br.com.stormaid.dto.request.AddressRequestDto;
import br.com.stormaid.dto.request.CitizenProfileRequest;
import br.com.stormaid.dto.request.EmergencyContactRequestDto;
import br.com.stormaid.dto.request.VulnerabilityRequestDto;
import br.com.stormaid.entity.CitizenProfile;
import br.com.stormaid.entity.User;
import br.com.stormaid.enums.MobilityStatus;
import br.com.stormaid.repository.CitizenProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitizenProfileServiceTest {

    @Mock
    private CitizenProfileRepository citizenProfileRepository;

    @InjectMocks
    private CitizenProfileService citizenProfileService;

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("cidadao@stormaid.example");
        return user;
    }

    @Test
    void shouldReturnDefaultProfileWhenUserHasNoProfileSaved() {
        User user = buildUser();
        when(citizenProfileRepository.findByUser(user)).thenReturn(Optional.empty());

        CitizenProfile profile = citizenProfileService.getProfileByUser(user);

        assertNotNull(profile);
        assertEquals(user, profile.getUser());
        assertNotNull(profile.getAddress());
        assertNotNull(profile.getVulnerability());
        assertEquals(MobilityStatus.AUTONOMOUS, profile.getVulnerability().getMobilityStatus());
    }

    @Test
    void shouldUpdateProfileAddressVulnerabilityAndEmergencyContact() {
        User user = buildUser();
        when(citizenProfileRepository.findByUser(user)).thenReturn(Optional.empty());
        when(citizenProfileRepository.save(any(CitizenProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        AddressRequestDto address = new AddressRequestDto();
        address.setPostalCode("01001-000");
        address.setStreet("Praca da Se");
        address.setCity("Sao Paulo");
        address.setState("sp");

        VulnerabilityRequestDto vulnerability = new VulnerabilityRequestDto();
        vulnerability.setMobilityStatus(MobilityStatus.ASSISTED);
        vulnerability.setHasVisualImpairment(true);
        vulnerability.setAccessibilityNotes("  Precisa de apoio na evacuacao  ");

        EmergencyContactRequestDto contact = new EmergencyContactRequestDto();
        contact.setName("Ana Paula");
        contact.setPhoneNumber("11999999999");
        contact.setRelationship("Irma");

        CitizenProfileRequest request = new CitizenProfileRequest();
        request.setAddress(address);
        request.setVulnerability(vulnerability);
        request.setEmergencyContact(contact);

        CitizenProfile updated = citizenProfileService.updateProfile(user, request);

        assertEquals("01001-000", updated.getAddress().getPostalCode());
        assertEquals("SP", updated.getAddress().getState());
        assertEquals(MobilityStatus.ASSISTED, updated.getVulnerability().getMobilityStatus());
        assertEquals(Boolean.TRUE, updated.getVulnerability().getHasVisualImpairment());
        assertEquals("  Precisa de apoio na evacuacao  ", updated.getVulnerability().getAccessibilityNotes());
        assertEquals("Ana Paula", updated.getEmergencyContact().getName());
        assertEquals("11999999999", updated.getEmergencyContact().getPhoneNumber());
        verify(citizenProfileRepository).save(any(CitizenProfile.class));
    }

    @Test
    void shouldClearEmergencyContactWhenAllFieldsAreBlank() {
        User user = buildUser();
        when(citizenProfileRepository.findByUser(user)).thenReturn(Optional.empty());
        when(citizenProfileRepository.save(any(CitizenProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        EmergencyContactRequestDto blankContact = new EmergencyContactRequestDto();
        blankContact.setName("  ");
        blankContact.setPhoneNumber("");
        blankContact.setRelationship(null);

        CitizenProfileRequest request = new CitizenProfileRequest();
        request.setEmergencyContact(blankContact);

        CitizenProfile updated = citizenProfileService.updateProfile(user, request);

        assertNull(updated.getEmergencyContact().getName());
        assertNull(updated.getEmergencyContact().getPhoneNumber());
        assertNull(updated.getEmergencyContact().getRelationship());
    }
}