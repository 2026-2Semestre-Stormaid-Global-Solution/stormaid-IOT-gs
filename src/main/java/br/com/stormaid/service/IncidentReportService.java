package br.com.stormaid.service;

import br.com.stormaid.ai.RagAnalysisService;
import br.com.stormaid.dto.request.IncidentCreateRequest;
import br.com.stormaid.dto.response.IncidentRegulationViewResponse;
import br.com.stormaid.dto.response.IncidentResponse;
import br.com.stormaid.embeddable.IncidentLocation;
import br.com.stormaid.entity.Dependent;
import br.com.stormaid.entity.IncidentReport;
import br.com.stormaid.entity.User;
import br.com.stormaid.enums.ChannelType;
import br.com.stormaid.enums.IncidentPublicStatus;
import br.com.stormaid.enums.TranscriptionStatus;
import br.com.stormaid.messaging.IncidentEventPublisher;
import br.com.stormaid.repository.DependentRepository;
import br.com.stormaid.repository.IncidentReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class IncidentReportService {

    private final IncidentReportRepository incidentReportRepository;
    private final DependentRepository dependentRepository;
    private final IncidentEventPublisher eventPublisher;
    private final RagAnalysisService ragAnalysisService;

    public IncidentReportService(
            IncidentReportRepository incidentReportRepository,
            DependentRepository dependentRepository,
            IncidentEventPublisher eventPublisher,
            RagAnalysisService ragAnalysisService
    ) {
        this.incidentReportRepository = incidentReportRepository;
        this.dependentRepository = dependentRepository;
        this.eventPublisher = eventPublisher;
        this.ragAnalysisService = ragAnalysisService;
    }

    @Transactional
    public IncidentResponse createIncident(
            User requester,
            IncidentCreateRequest request,
            String idempotencyKey
    ) {
        if (request.channelType() == ChannelType.TEXT_INPUT
                && (request.textDescription() == null
                || request.textDescription().isBlank())) {
            throw new IllegalArgumentException(
                    "A descrição é obrigatória quando o canal for TEXT_INPUT"
            );
        }

        String effectiveKey;

        if (idempotencyKey == null) {
            effectiveKey = "auto-" + UUID.randomUUID();
        } else {
            effectiveKey = idempotencyKey.trim();

            if (effectiveKey.isBlank() || effectiveKey.length() > 100) {
                throw new IllegalArgumentException(
                        "Idempotency-Key deve conter entre 1 e 100 caracteres"
                );
            }

            if (effectiveKey.startsWith("auto-")) {
                throw new IllegalArgumentException(
                        "O prefixo auto- é reservado para chaves internas"
                );
            }

            var existing = incidentReportRepository
                    .findByRequesterIdAndIdempotencyKey(
                            requester.getId(),
                            effectiveKey
                    );

            if (existing.isPresent()) {
                IncidentReport previous = existing.get();

                if (!isSameRequest(previous, request)) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Idempotency-Key já utilizada com dados diferentes"
                    );
                }

                return mapToResponse(previous);
            }
        }

        IncidentReport incident = new IncidentReport();

        incident.setIdempotencyKey(effectiveKey);
        incident.setRequester(requester);
        incident.setRecipientType(request.recipientType());

        if (request.dependentId() != null) {
            Dependent dependent = dependentRepository
                    .findById(request.dependentId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Dependente não encontrado"
                    ));

            incident.setDependent(dependent);
        }

        incident.setThirdPartyName(request.thirdPartyName());
        incident.setRequesterOnSite(request.requesterOnSite());
        incident.setChannelType(request.channelType());
        incident.setEmergencyCategory(request.emergencyCategory());
        incident.setTextDescription(request.textDescription());
        incident.setLocation(request.location());

        incident.setPublicStatus(IncidentPublicStatus.RECEIVED);
        incident.setStatusVersion(0L);
        incident.setCreatedAt(Instant.now());
        incident.setUpdatedAt(Instant.now());

        if (incident.getTranscriptionStatus() == null) {
            incident.setTranscriptionStatus(
                    TranscriptionStatus.NOT_APPLICABLE
            );
        }

        IncidentReport saved = incidentReportRepository.save(incident);

        eventPublisher.publishIncidentCreated(saved);

        try {
            ragAnalysisService.analyzeIncident(saved);
        } catch (Exception ignored) {
        }

        return mapToResponse(saved);
    }

    private boolean isSameRequest(
            IncidentReport previous,
            IncidentCreateRequest request
    ) {
        Long previousDependentId = previous.getDependent() == null
                ? null
                : previous.getDependent().getId();

        return previous.getRecipientType() == request.recipientType()
                && Objects.equals(
                previousDependentId,
                request.dependentId()
        )
                && Objects.equals(
                previous.getThirdPartyName(),
                request.thirdPartyName()
        )
                && Objects.equals(
                previous.getRequesterOnSite(),
                request.requesterOnSite()
        )
                && previous.getChannelType() == request.channelType()
                && previous.getEmergencyCategory() == request.emergencyCategory()
                && Objects.equals(
                previous.getTextDescription(),
                request.textDescription()
        )
                && isSameLocation(
                previous.getLocation(),
                request.location()
        );
    }

    private boolean isSameLocation(
            IncidentLocation previous,
            IncidentLocation incoming
    ) {
        if (previous == null || incoming == null) {
            return previous == incoming;
        }

        return previous.getSource() == incoming.getSource()
                && sameDecimal(
                previous.getLatitude(),
                incoming.getLatitude()
        )
                && sameDecimal(
                previous.getLongitude(),
                incoming.getLongitude()
        )
                && Objects.equals(
                previous.getAddressText(),
                incoming.getAddressText()
        )
                && Objects.equals(
                previous.getReferencePoint(),
                incoming.getReferencePoint()
        )
                && sameDecimal(
                previous.getAccuracyMeters(),
                incoming.getAccuracyMeters()
        )
                && Objects.equals(
                previous.getLocationCapturedAt(),
                incoming.getLocationCapturedAt()
        );
    }

    private boolean sameDecimal(
            BigDecimal previous,
            BigDecimal incoming
    ) {
        if (previous == null || incoming == null) {
            return previous == incoming;
        }

        return previous.compareTo(incoming) == 0;
    }

    public Page<IncidentResponse> listByRequester(
            User requester,
            Pageable pageable
    ) {
        return incidentReportRepository
                .findByRequester(requester, pageable)
                .map(this::mapToResponse);
    }

    public IncidentResponse findByIdAndRequester(
            Long id,
            User requester
    ) {
        IncidentReport incident = incidentReportRepository
                .findById(id)
                .filter(inc -> inc.getRequester()
                        .getId()
                        .equals(requester.getId()))
                .orElseThrow(() -> new RuntimeException(
                        "Ocorrência não encontrada ou não autorizada"
                ));

        return mapToResponse(incident);
    }

    private IncidentResponse mapToResponse(IncidentReport incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getRequester() != null
                        ? incident.getRequester().getId()
                        : null,
                incident.getRecipientType(),
                incident.getChannelType(),
                incident.getEmergencyCategory(),
                incident.getPublicStatus(),
                incident.getLocation(),
                incident.getCreatedAt()
        );
    }

    public IncidentRegulationViewResponse getRegulationView(Long id) {
        IncidentReport incident = incidentReportRepository
                .findById(id)
                .orElseThrow(() ->
                        new jakarta.persistence.EntityNotFoundException(
                                "Ocorrência não encontrada com ID: " + id
                        )
                );

        IncidentRegulationViewResponse response =
                new IncidentRegulationViewResponse();

        response.setIncidentId(incident.getId());
        response.setRecipientType(incident.getRecipientType());
        response.setChannelType(incident.getChannelType());
        response.setEmergencyCategory(incident.getEmergencyCategory());
        response.setTranscriptionStatus(incident.getTranscriptionStatus());

        if (incident.getLocation() != null) {
            IncidentRegulationViewResponse.LocationDto loc =
                    new IncidentRegulationViewResponse.LocationDto();

            if (incident.getLocation().getLatitude() != null) {
                loc.setLatitude(
                        incident.getLocation()
                                .getLatitude()
                                .doubleValue()
                );
            }

            if (incident.getLocation().getLongitude() != null) {
                loc.setLongitude(
                        incident.getLocation()
                                .getLongitude()
                                .doubleValue()
                );
            }

            loc.setAddressText(
                    incident.getLocation().getAddressText()
            );

            response.setLocation(loc);
        }

        if (incident.getChannelType() == ChannelType.TEXT_INPUT) {
            response.setReport(incident.getTextDescription());
        } else if (incident.getChannelType() == ChannelType.VOICE_AUDIO) {
            response.setReport(incident.getAudioTranscription());
        }

        return response;
    }
}