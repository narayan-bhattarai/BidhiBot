package com.ragdesk.controllers;

import com.ragdesk.models.Document;
import com.ragdesk.models.DocumentChunk;
import com.ragdesk.services.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.ragdesk.repositories.DocumentRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rag")
public class RagController {
    private final IPdfService pdfService;
    private final IEmbeddingService embeddingService;
    private final IVectorDbService vectorDbService;
    private final ILlmService llmService;
    private final RecursiveChunkingService chunkingService;
    private final DocumentRepository documentRepository;
    private final String uploadDir = "uploads";

    public RagController(IPdfService pdfService,
            IEmbeddingService embeddingService,
            IVectorDbService vectorDbService,
            ILlmService llmService,
            RecursiveChunkingService chunkingService,
            DocumentRepository documentRepository) {
        this.pdfService = pdfService;
        this.embeddingService = embeddingService;
        this.vectorDbService = vectorDbService;
        this.llmService = llmService;
        this.chunkingService = chunkingService;
        this.documentRepository = documentRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("Starting upload for: " + file.getOriginalFilename());
            Document doc = new Document();
            doc.setFileName(file.getOriginalFilename());
            doc.setUploadDate(OffsetDateTime.now());

            Path path = Paths.get(uploadDir, UUID.randomUUID() + "_" + file.getOriginalFilename());
            if (!Files.exists(Paths.get(uploadDir))) {
                Files.createDirectories(Paths.get(uploadDir));
            }

            doc.setStoragePath(path.toString());
            Files.write(path, file.getBytes());
            System.out.println("File saved to: " + path);

            List<PageContent> pages = pdfService.extractText(path.toString());
            doc.setTotalPages(pages.size());
            doc.setProcessed(false);
            Document savedDoc = documentRepository.save(doc);

            System.out.println("Processing " + pages.size() + " pages...");

            List<DocumentChunk> chunksToSave = new ArrayList<>();
            int chunkIdx = 0;

            for (PageContent page : pages) {
                List<String> textChunks = chunkingService.splitText(page.text(), 1000, 150);

                for (String content : textChunks) {
                    DocumentChunk chunk = new DocumentChunk();
                    chunk.setDocument(savedDoc);
                    chunk.setContent(content);
                    chunk.setPageNumber(page.pageNumber());
                    chunk.setChunkIndex(chunkIdx++);
                    chunk.setEmbedding(embeddingService.generateEmbedding(content));
                    chunksToSave.add(chunk);
                }
            }

            System.out.println("Saving " + chunksToSave.size() + " chunks to Vector database...");
            vectorDbService.saveChunks(chunksToSave);

            savedDoc.setProcessed(true);
            documentRepository.save(savedDoc);

            System.out.println("Upload complete for: " + file.getOriginalFilename());
            return ResponseEntity.ok(String.format("Successfully processed %d chunks from document: %s",
                    chunksToSave.size(), file.getOriginalFilename()));
        } catch (Exception e) {
            System.err.println("UPLOAD FAILED: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<List<Document>> listDocuments() {
        return ResponseEntity.ok(documentRepository.findAll());
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<String> deleteDocument(@PathVariable UUID id) {
        return documentRepository.findById(id).map(doc -> {
            try {
                Path filePath = Paths.get(doc.getStoragePath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log error but continue with DB deletion
                System.err.println("Could not delete file: " + e.getMessage());
            }
            documentRepository.delete(doc);
            return ResponseEntity.ok("Document deleted successfully");
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/chat")
    public ResponseEntity<String> chat(@RequestParam String question, @RequestParam(required = false) UUID documentId) {
        float[] queryVector = embeddingService.generateEmbedding(question);
        List<DocumentChunk> relevantChunks = vectorDbService.searchSimilarChunks(queryVector, 10, documentId);

        // Guard clause: return immediately if no relevant context found
        if (relevantChunks == null || relevantChunks.isEmpty()) {
            return ResponseEntity.ok("I could not find the answer in the uploaded documents.");
        }

        String answer = llmService.askQuestion(question, relevantChunks);
        return ResponseEntity.ok(answer);
    }
}
