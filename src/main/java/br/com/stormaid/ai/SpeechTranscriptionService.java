package br.com.stormaid.ai;

import br.com.stormaid.entity.IncidentReport;
import br.com.stormaid.enums.TranscriptionStatus;
import br.com.stormaid.repository.IncidentReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class SpeechTranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SpeechTranscriptionService.class);

    private final IncidentReportRepository incidentRepository;

    public SpeechTranscriptionService(IncidentReportRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @Async
    @Transactional
    public void transcribeAudioAsync(Long incidentId, String audioStorageKey) {
        IncidentReport incident = incidentRepository.findById(incidentId).orElse(null);
        if (incident == null) {
            return;
        }

        try {
            String transcribedText = "[TRANSCRIÇÃO SIMULADA] Áudio ref: "
                    + audioStorageKey
                    + " - Relato de emergência capturado via canal de voz.";
            incident.setAudioTranscription(transcribedText);
            incident.setTranscriptionStatus(TranscriptionStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Falha na transcrição simulada de áudio para o incidente {}: {}", incidentId, e.getMessage());
            incident.setTranscriptionStatus(TranscriptionStatus.FAILED);
        }

        incident.setUpdatedAt(Instant.now());
        incidentRepository.save(incident);
    }
}