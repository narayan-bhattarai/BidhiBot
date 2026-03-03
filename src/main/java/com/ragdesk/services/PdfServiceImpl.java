package com.ragdesk.services;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.ITesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.awt.image.BufferedImage;

@Service
public class PdfServiceImpl implements IPdfService {
    private final ITesseract tesseract;

    public PdfServiceImpl() {
        this.tesseract = new Tesseract();
        File tessDataFolder = new File("./tessdata");
        this.tesseract.setDatapath(tessDataFolder.getAbsolutePath());
        this.tesseract.setLanguage("eng+nep");
    }

    @Override
    public List<PageContent> extractText(String filePath) {
        List<PageContent> results = new ArrayList<>();
        File file = new File(filePath);

        try (PDDocument document = PDDocument.load(file)) {
            int pageCount = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            PDFRenderer renderer = new PDFRenderer(document);

            for (int i = 0; i < pageCount; i++) {
                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);
                String text = stripper.getText(document);

                if (text == null || text.trim().length() < 100) {
                    try {
                        BufferedImage bim = renderer.renderImageWithDPI(i, 300);
                        text = tesseract.doOCR(bim);
                        results.add(new PageContent(i + 1, text, true));
                    } catch (Throwable t) {
                        results.add(new PageContent(i + 1, "OCR Failed for scanned page: " + t.getMessage(), true));
                    }
                } else {
                    results.add(new PageContent(i + 1, text, false));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error processing PDF: " + e.getMessage(), e);
        }

        return results;
    }
}
