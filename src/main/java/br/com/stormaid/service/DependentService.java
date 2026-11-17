package br.com.stormaid.service;

import br.com.stormaid.dto.request.DependentRequest;
import br.com.stormaid.entity.Dependent;
import br.com.stormaid.entity.User;
import br.com.stormaid.repository.DependentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DependentService {

    private final DependentRepository dependentRepository;

    public DependentService(DependentRepository dependentRepository) {
        this.dependentRepository = dependentRepository;
    }

    @Transactional
    public Dependent createDependent(User guardian, DependentRequest request) {
        Dependent dependent = new Dependent();
        dependent.setFullName(request.getFullName());

        if (request.getRelationship() != null) {
            dependent.setRelationship(br.com.stormaid.enums.RelationshipType.valueOf(request.getRelationship()));
        }

        dependent.setBirthDate(request.getBirthDate());
        dependent.setGuardian(guardian);
        dependent.setActive(true);
        return dependentRepository.save(dependent);
    }

    public Page<Dependent> listByGuardian(User guardian, Pageable pageable) {
        return dependentRepository.findByGuardian(guardian, pageable);
    }

    public Dependent findByIdAndGuardian(Long id, User guardian) {
        return dependentRepository.findById(id)
                .filter(dep -> dep.getGuardian().getId().equals(guardian.getId()))
                .orElseThrow(() -> new RuntimeException("Dependente não encontrado ou não autorizado"));
    }

    @Transactional
    public Dependent updateDependent(Long id, User guardian, DependentRequest request) {
        Dependent dependent = findByIdAndGuardian(id, guardian);

        dependent.setFullName(request.getFullName());

        if (request.getRelationship() != null) {
            dependent.setRelationship(br.com.stormaid.enums.RelationshipType.valueOf(request.getRelationship()));
        }

        dependent.setBirthDate(request.getBirthDate());

        return dependentRepository.save(dependent);
    }

    @Transactional
    public void deleteDependent(Long id, User guardian) {
        Dependent dependent = findByIdAndGuardian(id, guardian);
        dependentRepository.delete(dependent);
    }
}