package com.qandding.domain.ai.controller;

import com.qandding.domain.ai.dto.GeneratedProblemDto;
import com.qandding.domain.ai.service.OpenAiProblemService;
import com.qandding.domain.ai.service.TesseractOcrService;
import com.qandding.domain.pdf.service.PdfGenerateService;
import com.qandding.domain.subject.entity.Subject;
import com.qandding.domain.subject.repository.SubjectRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Problem Generation (AI)", description = "AI를 이용한 문제 생성 관련 API")
@RestController
@RequestMapping("/api/v1/problems")
@Slf4j
public class ProblemGenerationController {

    private final TesseractOcrService tesseractOcrService;
    private final OpenAiProblemService openAiProblemService;
    private final PdfGenerateService pdfGenerateService;
    private final SubjectRepository subjectRepository;

    public ProblemGenerationController(TesseractOcrService tesseractOcrService, OpenAiProblemService openAiProblemService, PdfGenerateService pdfGenerateService, SubjectRepository subjectRepository) {
        this.tesseractOcrService = tesseractOcrService;
        this.openAiProblemService = openAiProblemService;
        this.pdfGenerateService = pdfGenerateService;
        this.subjectRepository = subjectRepository;
    }

    @Operation(summary = "이미지로부터 유사 문제 PDF 생성", description = "문제 사진 이미지를 업로드하고 과목 ID를 지정하면, AI가 유사 문제를 생성하여 정답/풀이가 포함된 PDF 파일로 반환합니다.(이미지만 가능합니다.)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF 생성 성공", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 과목 ID일 경우", content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류 발생", content = @Content)
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> generateProblemsFromImage(
            @Parameter(description = "문제가 포함된 이미지 파일", required = true)
            @RequestParam("imageFile") MultipartFile imageFile,
            @Parameter(description = "데이터베이스에 저장된 과목의 고유 ID", required = true)
            @RequestParam("subjectId") Long subjectId) {
        try {
            // 1. Find Subject by ID from the database
            Subject subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found with id: " + subjectId));

            // 2. Extract text from image using Tesseract OCR
            String ocrText = tesseractOcrService.extractTextFromImage(imageFile);

            // 3. Generate problems and solutions using OpenAI
            GeneratedProblemDto generatedProblems = openAiProblemService.generateProblemsAndSolutions(ocrText);

            // 4. Generate PDF from the problems
            byte[] pdfBytes = pdfGenerateService.generatePdf(generatedProblems, subject.getName());

            // 5. Return the PDF as a file download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "generated_problems.pdf");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (ResponseStatusException e) {
            throw e; // Re-throw client-side errors (like 404)
        } catch (Exception e) {
            log.error("Error generating problems from image", e); // Log the exception
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
