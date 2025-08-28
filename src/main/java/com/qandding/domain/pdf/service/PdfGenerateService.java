package com.qandding.domain.pdf.service;

import com.qandding.domain.ai.dto.GeneratedProblemDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfGenerateService {

    public byte[] generatePdf(GeneratedProblemDto generatedProblems, String subject) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            // 한글 폰트 로드
            InputStream fontStream = new ClassPathResource("static/fonts/NanumGothic.ttf").getInputStream();
            PDType0Font font = PDType0Font.load(document, fontStream);

            // Problems Section
            try (PDPageContentStream problemContentStream = new PDPageContentStream(document, page)) {
                float margin = 50;
                float yStart = page.getMediaBox().getHeight() - margin;
                float yPosition = yStart;
                float width = page.getMediaBox().getWidth() - 2 * margin;

                // 1. Header
                addHeader(problemContentStream, font, page, subject);
                yPosition -= 60; // 헤더 높이만큼 y위치 조정

                // 2. Problems
                yPosition = addProblems(problemContentStream, font, generatedProblems, yPosition, width, margin);
            } // problemContentStream is closed here

            // Solutions Section (on new page(s))
            float margin = 50;
            float yStart = PDRectangle.A4.getHeight() - margin;
            float yPosition = yStart;
            float width = PDRectangle.A4.getWidth() - 2 * margin;
            float bottomMargin = 70;

            PDPage solutionPage = new PDPage(PDRectangle.A4);
            document.addPage(solutionPage);
            PDPageContentStream solutionContentStream = new PDPageContentStream(document, solutionPage);

            try {
                // Solutions Header for the first solution page
                solutionContentStream.beginText();
                solutionContentStream.setFont(font, 18); // Larger font for section title
                solutionContentStream.newLineAtOffset(margin, yPosition);
                solutionContentStream.showText("풀이 및 정답");
                solutionContentStream.endText();
                yPosition -= 40; // Adjust yPosition after header

                int problemNumber = 1;
                for (GeneratedProblemDto.ProblemDetail problem : generatedProblems.getProblems()) {
                    String solutionTitle = String.format("[%d번 풀이]", problemNumber++);
                    yPosition = addWrappedText(solutionContentStream, font, 12, solutionTitle, margin, yPosition, width, 15);

                    yPosition = addWrappedText(solutionContentStream, font, 11, problem.getSolution(), margin + 10, yPosition, width - 10, 15);

                    String answer = "정답: " + problem.getAnswer();
                    yPosition = addWrappedText(solutionContentStream, font, 11, answer, margin + 10, yPosition, width - 10, 30);

                    // Check if a new page is needed for the next problem's solution
                    // This logic is now handled within generatePdf, not addSolutions
                    if (yPosition < bottomMargin && generatedProblems.getProblems().indexOf(problem) < generatedProblems.getProblems().size() - 1) {
                        solutionContentStream.close(); // Close the current stream
                        solutionPage = new PDPage(PDRectangle.A4);
                        document.addPage(solutionPage);
                        solutionContentStream = new PDPageContentStream(document, solutionPage);
                        yPosition = yStart; // Reset yPosition for the new page
                    }
                }
            } finally {
                // Ensure the last content stream is closed
                if (solutionContentStream != null) {
                    solutionContentStream.close();
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private void addHeader(PDPageContentStream contentStream, PDType0Font font, PDPage page, String subject) throws IOException {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String title = String.format("%s - 유사 문제 (%s)", subject, date);

        float titleWidth = font.getStringWidth(title) / 1000 * 18f;
        float titleX = (page.getMediaBox().getWidth() - titleWidth) / 2;
        float titleY = page.getMediaBox().getHeight() - 60;

        contentStream.beginText();
        contentStream.setFont(font, 18);
        contentStream.newLineAtOffset(titleX, titleY);
        contentStream.showText(title);
        contentStream.endText();
    }

    private float addProblems(PDPageContentStream contentStream, PDType0Font font, GeneratedProblemDto generatedProblems, float yPosition, float width, float margin) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, 16);
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText("문제");
        contentStream.endText();
        yPosition -= 30;

        int problemNumber = 1;
        for (GeneratedProblemDto.ProblemDetail problem : generatedProblems.getProblems()) {
            String problemText = String.format("%d. %s", problemNumber++, problem.getNewProblem());
            yPosition = addWrappedText(contentStream, font, 12, problemText, margin, yPosition, width, 20);
        }
        return yPosition;
    }

    private float addSolutions(PDPageContentStream contentStream, PDType0Font font, GeneratedProblemDto generatedProblems, float yPosition, float width, float margin) throws IOException {
        int problemNumber = 1;
        for (GeneratedProblemDto.ProblemDetail problem : generatedProblems.getProblems()) {
            String solutionTitle = String.format("[%d번 풀이]", problemNumber++);
            yPosition = addWrappedText(contentStream, font, 12, solutionTitle, margin, yPosition, width, 15);

            yPosition = addWrappedText(contentStream, font, 11, problem.getSolution(), margin + 10, yPosition, width - 10, 15);

            String answer = "정답: " + problem.getAnswer();
            yPosition = addWrappedText(contentStream, font, 11, answer, margin + 10, yPosition, width - 10, 30);
        }
        return yPosition; // Return the final yPosition
    }

    private float addWrappedText(PDPageContentStream contentStream, PDType0Font font, float fontSize, String text, float x, float y, float width, float marginBottom) throws IOException {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            float size = font.getStringWidth(line + word) / 1000 * fontSize;
            if (size > width) {
                lines.add(line.toString());
                line = new StringBuilder(word + " ");
            } else {
                line.append(word).append(" ");
            }
        }
        lines.add(line.toString());

        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        for (String l : lines) {
            contentStream.showText(l);
            contentStream.newLineAtOffset(0, -1.5f * fontSize);
        }
        contentStream.endText();

        return y - (lines.size() * 1.5f * fontSize) - marginBottom;
    }
}