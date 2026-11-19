package br.com.stormaid.service;

import br.com.stormaid.dto.request.AddressRequestDto;
import br.com.stormaid.dto.request.DependentRequest;
import br.com.stormaid.dto.request.VulnerabilityRequestDto;
import br.com.stormaid.embeddable.Address;
import br.com.stormaid.embeddable.VulnerabilityInfo;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Locale;

@Service
public class DependentService {

    private final DependentRepository dependentRepository;
    private final CitizenProfileRepository citizenProfileRepository;

    public DependentService(
            DependentRepository dependentRepository,
            CitizenProfileRepository citizenProfileRepository
    ) {
        this.dependentRepository = dependentRepository;
        this.citizenProfileRepository = citizenProfileRepository;
    }

    @Transactional
    public Dependent createDependent(User guardian, DependentRequest request) {
        Dependent dependent = new Dependent();
        dependent.setGuardian(guardian);
        dependent.setFullName(request.getFullName().trim());
        dependent.setRelationship(parseRelationship(request.getRelationship()));
        dependent.setBirthDate(request.getBirthDate());

        boolean sameAddress = request.getSameAddressAsGuardian() != null
                ? request.getSameAddressAsGuardian()
                : true;
        dependent.setSameAddressAsGuardian(sameAddress);
        dependent.setAddress(resolveAddressForCreate(guardian, sameAddress, request.getAddress()));
        dependent.setVulnerability(mergeVulnerability(defaultVulnerability(), request.getVulnerability()));
        dependent.setActive(true);

        return dependentRepository.save(dependent);
    }

    @Transactional(readOnly = true)
    public Page<Dependent> listByGuardian(User guardian, Pageable pageable) {
        return dependentRepository.findByGuardianAndActiveTrue(guardian, pageable);
    }

    @Transactional(readOnly = true)
    public Dependent findByIdAndGuardian(Long id, User guardian) {
        Dependent dependent = dependentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dependente não encontrado"));

        if (Boolean.FALSE.equals(dependent.getActive())) {
            throw new ResourceNotFoundException("Dependente não encontrado");
        }

        if (!dependent.getGuardian().getId().equals(guardian.getId())) {
            throw new ForbiddenException("Acesso negado ao dependente informado");
        }

        return dependent;
    }

    @Transactional
    public Dependent updateDependent(Long id, User guardian, DependentRequest request) {
        Dependent dependent = findByIdAndGuardian(id, guardian);

        dependent.setFullName(request.getFullName().trim());
        dependent.setRelationship(parseRelationship(request.getRelationship()));
        dependent.setBirthDate(request.getBirthDate());

        boolean sameAddress = request.getSameAddressAsGuardian() != null
                ? request.getSameAddressAsGuardian()
                : Boolean.TRUE.equals(dependent.getSameAddressAsGuardian());
        dependent.setSameAddressAsGuardian(sameAddress);
        dependent.setAddress(resolveAddressForUpdate(guardian, dependent.getAddress(), sameAddress, request.getAddress()));
        dependent.setVulnerability(mergeVulnerability(
                dependent.getVulnerability() != null ? dependent.getVulnerability() : defaultVulnerability(),
                request.getVulnerability()
        ));

        return dependentRepository.save(dependent);
    }

    @Transactional
    public void deleteDependent(Long id, User guardian) {
        Dependent dependent = findByIdAndGuardian(id, guardian);
        dependent.setActive(false);
        dependentRepository.save(dependent);
    }

    private RelationshipType parseRelationship(String rawRelationship) {
        if (rawRelationship == null || rawRelationship.isBlank()) {
            throw new BadRequestException("O parentesco é obrigatório");
        }
        String normalized = rawRelationship.trim().toUpperCase(Locale.ROOT);
        try {
            return RelationshipType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            String allowed = Arrays.toString(RelationshipType.values());
            throw new BadRequestException("Parentesco inválido: '" + rawRelationship + "'. Valores aceites: " + allowed);
        }
    }

    private Address resolveAddressForCreate(User guardian, boolean sameAddress, AddressRequestDto reqAddress) {
        if (sameAddress) {
            Address copiedFromGuardian = copyGuardianAddress(guardian);
            return reqAddress != null ? mergeAddress(copiedFromGuardian, reqAddress) : copiedFromGuardian;
        }
        return reqAddress != null ? mergeAddress(new Address(), reqAddress) : new Address();
    }

    private Address resolveAddressForUpdate(
            User guardian,
            Address currentAddress,
            boolean sameAddress,
            AddressRequestDto reqAddress
    ) {
        if (sameAddress) {
            Address base = reqAddress != null
                    ? mergeAddress(currentAddress != null ? currentAddress : copyGuardianAddress(guardian), reqAddress)
                    : copyGuardianAddress(guardian);
            return base;
        }
        return reqAddress != null
                ? mergeAddress(currentAddress != null ? currentAddress : new Address(), reqAddress)
                : (currentAddress != null ? currentAddress : new Address());
    }

    private Address copyGuardianAddress(User guardian) {
        Address target = new Address();
        CitizenProfile profile = citizenProfileRepository.findByUser(guardian).orElse(null);
        if (profile == null || profile.getAddress() == null) {
            return target;
        }

        Address source = profile.getAddress();
        target.setPostalCode(source.getPostalCode());
        target.setStreet(source.getStreet());
        target.setNumber(source.getNumber());
        target.setComplement(source.getComplement());
        target.setNeighborhood(source.getNeighborhood());
        target.setCity(source.getCity());
        target.setState(source.getState());
        target.setReferencePoint(source.getReferencePoint());
        return target;
    }

    private Address mergeAddress(Address current, AddressRequestDto req) {
        Address address = current != null ? current : new Address();

        if (req.getPostalCode() != null) {
            address.setPostalCode(req.getPostalCode());
        }
        if (req.getStreet() != null) {
            address.setStreet(req.getStreet());
        }
        if (req.getAddressNumber() != null) {
            address.setNumber(req.getAddressNumber());
        }
        if (req.getComplement() != null) {
            address.setComplement(req.getComplement());
        }
        if (req.getNeighborhood() != null) {
            address.setNeighborhood(req.getNeighborhood());
        }
        if (req.getCity() != null) {
            address.setCity(req.getCity());
        }
        if (req.getState() != null) {
            address.setState(req.getState().toUpperCase(Locale.ROOT));
        }
        if (req.getReferencePoint() != null) {
            address.setReferencePoint(req.getReferencePoint());
        }

        return address;
    }

    private VulnerabilityInfo defaultVulnerability() {
        VulnerabilityInfo vulnerability = new VulnerabilityInfo();
        vulnerability.setMobilityStatus(MobilityStatus.AUTONOMOUS);
        vulnerability.setHasVisualImpairment(false);
        vulnerability.setHasHearingImpairment(false);
        vulnerability.setHasSpeechImpairment(false);
        vulnerability.setHasCognitiveImpairment(false);
        vulnerability.setDependsOnEquipment(false);
        vulnerability.setEquipmentInformationDeclined(false);
        return vulnerability;
    }

    private VulnerabilityInfo mergeVulnerability(VulnerabilityInfo current, VulnerabilityRequestDto req) {
        VulnerabilityInfo vulnerability = current != null ? current : defaultVulnerability();

        if (vulnerability.getMobilityStatus() == null) {
            vulnerability.setMobilityStatus(MobilityStatus.AUTONOMOUS);
        }

        if (req == null) {
            return vulnerability;
        }

        if (req.getMobilityStatus() != null) {
            vulnerability.setMobilityStatus(req.getMobilityStatus());
        }
        if (req.getHasVisualImpairment() != null) {
            vulnerability.setHasVisualImpairment(req.getHasVisualImpairment());
        }
        if (req.getHasHearingImpairment() != null) {
            vulnerability.setHasHearingImpairment(req.getHasHearingImpairment());
        }
        if (req.getHasSpeechImpairment() != null) {
            vulnerability.setHasSpeechImpairment(req.getHasSpeechImpairment());
        }
        if (req.getHasCognitiveImpairment() != null) {
            vulnerability.setHasCognitiveImpairment(req.getHasCognitiveImpairment());
        }
        if (req.getDependsOnEquipment() != null) {
            vulnerability.setDependsOnEquipment(req.getDependsOnEquipment());
        }
        if (req.getEquipmentDescription() != null) {
            vulnerability.setEquipmentDescription(
                    req.getEquipmentDescription().isBlank() ? null : req.getEquipmentDescription()
            );
        }
        if (req.getEquipmentInformationDeclined() != null) {
            vulnerability.setEquipmentInformationDeclined(req.getEquipmentInformationDeclined());
        }
        if (req.getAccessibilityNotes() != null) {
            vulnerability.setAccessibilityNotes(
                    req.getAccessibilityNotes().isBlank() ? null : req.getAccessibilityNotes()
            );
        }

        return vulnerability;
    }
}