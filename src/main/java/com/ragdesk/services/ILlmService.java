package com.ragdesk.services;

import com.ragdesk.models.DocumentChunk;
import java.util.List;

public interface ILlmService {
    String askQuestion(String question, List<DocumentChunk> context);
}
