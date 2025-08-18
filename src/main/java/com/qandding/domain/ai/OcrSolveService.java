package com.qandding.domain.ai;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class OcrSolveService {
  private final GeminiVisionClient visionClient;

  private static final int MAX_PAGES = 5;
  private static final int TARGET_MAX_WIDTH = 1600;
  private static final int TARGET_MAX_HEIGHT = 1600;

  public Mono<String> solveFromUpload(String userQuestion, MultipartFile file) throws IOException {
    String contentType = file.getContentType();
    if (contentType == null) contentType = "application/octet-stream";

    List<GeminiVisionClient.ImagePart> images = switch (detectType(contentType, file.getOriginalFilename())) {
      case PDF -> renderPdfToImages(file.getBytes());
      case IMAGE -> List.of(toImagePart(file.getBytes(), contentType));
      default -> throw new IllegalArgumentException("지원하지 않는 파일 형식입니다: " + contentType);
    };

    String instruction = buildInstruction(userQuestion);
    return visionClient.generateWithImages(instruction, images);
  }

  public Mono<String> solveFromUploads(String userQuestion, List<MultipartFile> files) throws IOException {
    if (files == null || files.isEmpty()) {
      throw new IllegalArgumentException("업로드할 파일이 없습니다.");
    }
    List<GeminiVisionClient.ImagePart> all = new ArrayList<>();
    int pageCount = 0;
    for (MultipartFile f : files) {
      if (f == null || f.isEmpty()) continue;
      String ct = f.getContentType();
      if (ct == null) ct = "application/octet-stream";
      switch (detectType(ct, f.getOriginalFilename())) {
        case PDF -> {
          var parts = renderPdfToImages(f.getBytes());
          for (var p : parts) {
            if (pageCount >= MAX_PAGES) break;
            all.add(p);
            pageCount++;
          }
        }
        case IMAGE -> all.add(toImagePart(f.getBytes(), ct));
        default -> throw new IllegalArgumentException("지원하지 않는 파일 형식입니다: " + ct);
      }
      if (pageCount >= MAX_PAGES) break; // 전체 제한
    }
    if (all.isEmpty()) {
      throw new IllegalArgumentException("처리할 유효한 이미지/문서가 없습니다.");
    }
    String instruction = buildInstruction(userQuestion);
    return visionClient.generateWithImages(instruction, all);
  }

  public Mono<String> solveFromUrls(String userQuestion, List<String> urls) throws IOException {
    if (urls == null || urls.isEmpty()) {
      throw new IllegalArgumentException("처리할 URL이 없습니다.");
    }
    List<GeminiVisionClient.ImagePart> all = new ArrayList<>();
    int pageCount = 0;
    for (String url : urls) {
      if (url == null || url.isBlank()) continue;
      byte[] data;
      String ct;
      try {
        var fetched = fetchUrl(url);
        data = fetched.bytes();
        ct = fetched.contentType();
        if (ct == null || ct.isBlank()) ct = guessContentTypeFromName(url);
      } catch (Exception e) {
        log.warn("URL 다운로드 실패, 건너뜀: {} - {}", url, e.getMessage());
        continue;
      }
      FileType type = detectType(ct != null ? ct : "application/octet-stream", url);
      switch (type) {
        case PDF -> {
          var parts = renderPdfToImages(data);
          for (var p : parts) {
            if (pageCount >= MAX_PAGES) break;
            all.add(p);
            pageCount++;
          }
        }
        case IMAGE -> all.add(toImagePart(data, ct != null ? ct : "image/jpeg"));
        default -> log.warn("지원하지 않는 URL 형식, 건너뜀: {} ({})", url, ct);
      }
      if (pageCount >= MAX_PAGES) break;
    }
    if (all.isEmpty()) {
      throw new IllegalArgumentException("유효한 이미지/문서를 URL에서 가져오지 못했습니다.");
    }
    String instruction = buildInstruction(userQuestion);
    return visionClient.generateWithImages(instruction, all);
  }

  private Fetched fetchUrl(String urlStr) throws IOException {
    URL url = new URL(urlStr);
    URLConnection conn = url.openConnection();
    conn.setConnectTimeout(5000);
    conn.setReadTimeout(10000);
    String ct = conn.getContentType();
    try (InputStream is = conn.getInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      byte[] buf = new byte[8192];
      int r;
      while ((r = is.read(buf)) != -1) baos.write(buf, 0, r);
      return new Fetched(ct, baos.toByteArray());
    }
  }

  private String guessContentTypeFromName(String name) {
    if (name == null) return null;
    String lower = name.toLowerCase();
    if (lower.endsWith(".pdf")) return "application/pdf";
    if (lower.endsWith(".png")) return "image/png";
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
    if (lower.endsWith(".webp")) return "image/webp";
    if (lower.endsWith(".gif")) return "image/gif";
    return null;
  }

  private record Fetched(String contentType, byte[] bytes) {}

  private FileType detectType(String contentType, String filename) {
    if (contentType.toLowerCase().contains("pdf") || (filename != null && filename.toLowerCase().endsWith(".pdf"))) {
      return FileType.PDF;
    }
    if (contentType.startsWith("image/")) return FileType.IMAGE;
    return FileType.UNKNOWN;
  }

  private List<GeminiVisionClient.ImagePart> renderPdfToImages(byte[] pdfBytes) throws IOException {
    try (PDDocument doc = PDDocument.load(pdfBytes)) {
      PDFRenderer renderer = new PDFRenderer(doc);
      int pages = Math.min(doc.getNumberOfPages(), MAX_PAGES);
      List<GeminiVisionClient.ImagePart> list = new ArrayList<>();
      for (int i = 0; i < pages; i++) {
        // 200 DPI로 렌더링 후 다운스케일
        BufferedImage bim = renderer.renderImageWithDPI(i, 200, ImageType.RGB);
        BufferedImage scaled = downscaleIfNeeded(bim);
        byte[] jpg = encodeJpeg(scaled, 0.8f);
        list.add(new GeminiVisionClient.ImagePart("image/jpeg", jpg));
      }
      return list;
    }
  }

  private GeminiVisionClient.ImagePart toImagePart(byte[] bytes, String contentType) throws IOException {
    // Normalize size to avoid huge uploads
    try {
      BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
      if (img == null) return new GeminiVisionClient.ImagePart(contentType, bytes);
      BufferedImage scaled = downscaleIfNeeded(img);
      String mt = contentType.equals("image/png") ? "image/png" : "image/jpeg";
      byte[] out = mt.equals("image/png") ? encodePng(scaled) : encodeJpeg(scaled, 0.85f);
      return new GeminiVisionClient.ImagePart(mt, out);
    } catch (Exception e) {
      log.warn("이미지 리사이즈 중 오류, 원본 사용: {}", e.getMessage());
      return new GeminiVisionClient.ImagePart(contentType, bytes);
    }
  }

  private BufferedImage downscaleIfNeeded(BufferedImage src) {
    int w = src.getWidth();
    int h = src.getHeight();
    double scale = Math.min((double) TARGET_MAX_WIDTH / w, (double) TARGET_MAX_HEIGHT / h);
    if (scale >= 1.0) return src;
    int nw = Math.max(1, (int) Math.round(w * scale));
    int nh = Math.max(1, (int) Math.round(h * scale));
    BufferedImage dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = dst.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.drawImage(src, 0, 0, nw, nh, null);
    g.dispose();
    return dst;
  }

  private byte[] encodeJpeg(BufferedImage img, float quality) throws IOException {
    ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
    ImageWriteParam param = writer.getDefaultWriteParam();
    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    param.setCompressionQuality(quality);
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
         MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(baos)) {
      writer.setOutput(ios);
      writer.write(null, new IIOImage(img, null, null), param);
      writer.dispose();
      return baos.toByteArray();
    }
  }

  private byte[] encodePng(BufferedImage img) throws IOException {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      ImageIO.write(img, "png", baos);
      return baos.toByteArray();
    }
  }

  private String buildInstruction(String userQuestion) {
    return String.join("\n",
        "당신은 전문 OCR+해설 도우미입니다.",
        "첨부된 이미지/문서에서 다음을 수행하세요:",
        "1) 모든 텍스트를 정확히 OCR (한글/영어 혼합).",
        "2) 수학식은 LaTeX로 정갈히 표기하고, 회계 표/금액/계정과목을 구조화하여 추출.",
        "3) 표는 마크다운 표로 재구성.",
        "4) 아래 질문에 답하기 위해 필요한 정보만 선별.",
        "5) 풀이 과정을 단계별로 논리적으로 전개한 뒤 마지막에 정답을 명확히 제시.",
        "6) 정보가 부족하면 가정과 근거를 명시.",
        "출력 형식:",
        "- OCR 요약",
        "- 풀이 과정 (수식은 LaTeX)",
        "- 최종 답",
        "질문: " + userQuestion,
        "한국어로 답변하세요."
    );
  }

  private enum FileType { IMAGE, PDF, UNKNOWN }
}
