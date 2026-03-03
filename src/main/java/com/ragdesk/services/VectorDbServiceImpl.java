package com.ragdesk.services;

import com.ragdesk.models.DocumentChunk;
import com.ragdesk.repositories.DocumentChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VectorDbServiceImpl implements IVectorDbService {
    private final DocumentChunkRepository repository;

    public VectorDbServiceImpl(DocumentChunkRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void saveChunks(List<DocumentChunk> chunks) {
        repository.saveAll(chunks);
    }

    @Override
    public List<DocumentChunk> searchSimilarChunks(float[] queryEmbedding, int topK, UUID documentId) {
        List<DocumentChunk> chunks = (documentId != null) ? repository.findByDocumentId(documentId)
                : repository.findAll();

        chunks.sort(
                Comparator.comparingDouble((DocumentChunk c) -> -cosineSimilarity(queryEmbedding, c.getEmbedding())));

        return chunks.stream().limit(topK).collect(Collectors.toList());
    }

    private double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }
        if (normA == 0.0 || normB == 0.0)
            return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
