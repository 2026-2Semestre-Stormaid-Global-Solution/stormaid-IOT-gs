package br.com.stormaid.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VectorStoreLoader {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreLoader.class);
    private final VectorStore vectorStore;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    public VectorStoreLoader(VectorStore vectorStore,
                             ResourceLoader resourceLoader,
                             ObjectMapper objectMapper) {
        this.vectorStore = vectorStore;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadKnowledgeBase() {
        if (apiKey == null || !apiKey.startsWith("sk-") || apiKey.length() < 20) {
            log.info("Carga do VectorStore ignorada: Chave OpenAI válida não configurada.");
            return;
        }

        try {
            String documentId = "DEFESA-CIVIL-01";
            String title = "Guia Público de Prevenção e Resposta a Emergências - StormAID";
            String sourceUrl = "https://www.gov.br/mdr/pt-br/assuntos/protecao-e-defesa-civil/saiba-como-agir";

            Resource manifestResource = resourceLoader.getResource("classpath:knowledge/manifest.json");
            if (manifestResource.exists()) {
                JsonNode manifestNode = objectMapper.readTree(manifestResource.getInputStream());
                if (manifestNode.isArray() && !manifestNode.isEmpty()) {
                    JsonNode first = manifestNode.get(0);
                    documentId = first.path("documentId").asText(documentId);
                    title = first.path("title").asText(title);
                    sourceUrl = first.path("sourceUrl").asText(sourceUrl);
                }
            }

            Resource resource = resourceLoader.getResource("classpath:knowledge/guia-emergencias.md");
            if (resource.exists()) {
                TextReader textReader = new TextReader(resource);
                List<Document> documents = textReader.get();

                TokenTextSplitter textSplitter = new TokenTextSplitter();
                List<Document> splitDocuments = textSplitter.apply(documents);

                for (Document doc : splitDocuments) {
                    doc.getMetadata().putIfAbsent("documentId", documentId);
                    doc.getMetadata().putIfAbsent("title", title);
                    doc.getMetadata().putIfAbsent("sourceUrl", sourceUrl);
                    doc.getMetadata().putIfAbsent("section", extractSection(doc.getText()));
                }

                vectorStore.add(splitDocuments);
                log.info("Base de conhecimento RAG carregada com sucesso! Total de chunks: {}", splitDocuments.size());
            } else {
                log.warn("Ficheiro guia-emergencias.md não encontrado na pasta knowledge.");
            }
        } catch (Exception e) {
            log.warn("Aviso: Base de conhecimento RAG ignorada. Motivo: {}", e.getMessage());
        }
    }

    private String extractSection(String content) {
        if (content == null || content.isBlank()) {
            return "Geral";
        }
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("## ")) {
                return trimmed.substring(3).trim();
            }
        }
        return "Geral";
    }
}