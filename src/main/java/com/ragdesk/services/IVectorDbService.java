package com.ragdesk.services;

import com.ragdesk.models.DocumentChunk;
import java.util.List;
import java.util.UUID;

public interface IVectorDbService {
    void saveChunks(List<DocumentChunk> chunks);

    List<DocumentChunk> searchSimilarChunks(float[] queryEmbedding, int topK, UUID documentId);
}
