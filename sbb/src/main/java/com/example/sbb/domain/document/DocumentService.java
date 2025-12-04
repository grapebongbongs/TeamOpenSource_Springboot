package com.example.sbb.domain.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class DocumentService {
    public String extractText(String path) throws Exception {
        return extractText(Path.of(path));
    }

    public String extractText(Path path) throws Exception {
        byte[] data = Files.readAllBytes(path);
        return extractText(data);
    }

    public String extractText(byte[] data) throws Exception {
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(data))) {
            return new PDFTextStripper().getText(doc);
        }
    }
}
