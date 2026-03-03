# BidhiBot (विधिBot)

**BidhiBot (विधिBot)** is a self-hosted, document-grounded legal AI assistant for Nepalese constitutional and statutory documents.  
It allows users to upload PDFs, ask questions about the content, and receive **strictly grounded answers with page citations** from the uploaded sources.

This project uses a **Retrieval-Augmented Generation (RAG)** architecture with:
- PDF text extraction and chunking
- Semantic embeddings
- Vector search (pgvector)
- Local LLM inference using Ollama

No third-party LLM APIs are used — everything runs locally.

---

## 🧠 Core Features

- 📄 Upload multiple PDF law documents (e.g., Constitution of Nepal 2072)
- 🔍 Semantic search with embeddings
- 🤖 Local AI responses based only on uploaded context
- 📌 Forced citation: Every claim ends with `[Source: DocumentName, Page X]`
- 📝 Bullet point and table output formatting
- 🔐 Strict hallucination prevention
- 🗣️ Language awareness (Nepali or English, based on the question)

---

## 📦 Tech Stack

| Component | Technology |
|-----------|------------|
| Backend API | Spring Boot (Java) |
| Database | PostgreSQL + pgvector |
| Embeddings | nomic-embed-text (Ollama) |
| Language Model | qwen:7b / qwen3:1.7b (Ollama) |
| OCR (optional) | Tesseract |
| Deployment | Local / Cloud |

---

## 🚀 Quick Start

### 🎯 Prerequisites

Make sure you have:

- Java 17+
- Maven
- PostgreSQL with `pgvector` extension
- Ollama installed locally
- Tesseract OCR installed if using scanned PDFs

---

### ⚙️ Install and Configure Ollama

```bash
ollama pull qwen:7b
ollama pull nomic-embed-text
