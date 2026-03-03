package com.ragdesk.services;

import com.ragdesk.models.DocumentChunk;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OllamaServiceImpl implements IEmbeddingService, ILlmService {

    private final RestTemplate restTemplate;
    private final String baseUrl = "http://localhost:11434";

    public static final int TOP_K = 10;

    public OllamaServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ─────────────────────────────────────────────────────────────
    // EMBEDDING
    // ─────────────────────────────────────────────────────────────
    @Override
    public float[] generateEmbedding(String text) {

        String url = baseUrl + "/api/embeddings";
        Map<String, String> requestBody = Map.of(
                "model", "nomic-embed-text",
                "prompt", text);

        Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

        if (response == null || !response.containsKey("embedding")) {
            throw new RuntimeException("Embedding generation failed.");
        }

        List<Object> embedding = (List<Object>) response.get("embedding");
        float[] vector = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            vector[i] = ((Number) embedding.get(i)).floatValue();
        }

        return vector;
    }

    // ─────────────────────────────────────────────────────────────
    // LLM ANSWER
    // ─────────────────────────────────────────────────────────────
    @Override
    public String askQuestion(String question, List<DocumentChunk> context) {

        if (context == null || context.isEmpty()) {
            return "I could not find the answer in the uploaded documents.";
        }

        String url = baseUrl + "/api/generate";

        // Structured context formatting
        String contextBlock = context.stream()
                .map(c -> String.format(
                        """
                                ====================
                                SOURCE BLOCK
                                File: %s
                                Page: %d

                                %s
                                ====================
                                """,
                        c.getDocument().getFileName(),
                        c.getPageNumber(),
                        c.getContent()))
                .collect(Collectors.joining("\n\n"));

        String prompt = """
                You are a legal document reader. Your job is to find and present relevant text from the CONTEXT.

                ABSOLUTE RULES:
                1. ONLY use information that appears in the CONTEXT below.
                2. Do NOT add any information not found in the CONTEXT.
                3. Do NOT mix up Articles, clauses, or page numbers.
                4. Do NOT provide legal advice or procedural steps not in the document.
                5. Respond in the SAME language as the user's question. NEVER use Chinese.

                FORMATTING RULES (MANDATORY):
                - Start with a **bold heading** summarizing the topic.
                - Use bullet points (•) for listing provisions, roles, or conditions.
                - Use numbered lists (1. 2. 3.) for sequential steps or articles.
                - Group related information under **bold sub-headings** if there are multiple aspects.
                - Keep each point concise — one idea per bullet.
                - End every point with a citation: [Source: FileName, Page X]
                - Add an empty line between sections for readability.
                - If quoting a specific article, mention the Article number in bold.

                If no relevant text exists in CONTEXT, respond EXACTLY:
                "I could not find the answer in the uploaded documents."

                CONTEXT:
                """
                + contextBlock + """

                        USER QUESTION:
                        """ + question + """

                        ANSWER (formatted with headings and bullet points):
                        """;

        Map<String, Object> options = new HashMap<>();
        options.put("temperature", 0.0); // fully deterministic
        options.put("top_p", 0.85);
        options.put("repeat_penalty", 1.2);
        options.put("num_predict", 600);
        options.put("num_ctx", 4096);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "qwen3:1.7b");
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);
        requestBody.put("options", options);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

            if (response == null || !response.containsKey("response")) {
                return "I could not find the answer in the uploaded documents.";
            }

            String answer = ((String) response.get("response")).trim();

            if (answer.isBlank()) {
                return "I could not find the answer in the uploaded documents.";
            }

            // Soft citation validation — log warning but don't reject
            boolean hasCitation = answer.contains("Source:")
                    || answer.contains("[Source")
                    || answer.contains("Page ")
                    || answer.contains("Page:")
                    || answer.contains(".pdf");
            if (!hasCitation) {
                System.err.println("RAG WARNING: Model response may lack citations. Passing through.");
            }

            return answer;

        } catch (Exception e) {
            System.err.println("LLM call failed: " + e.getMessage());
            return "I could not find the answer in the uploaded documents.";
        }
    }
}