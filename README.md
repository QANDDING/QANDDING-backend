# QANDDING Backend

## 주요 기능

### 질문 관리

- 질문 생성, 조회, 삭제
- **이미지 첨부 기능**: 질문 생성 시 이미지를 S3에 업로드하여 저장
- 과목별, 교수별 질문 필터링

### AI 답변 생성

- 텍스트 기반 AI 답변 생성
- **이미지 기반 AI 답변 생성**: 이미지를 AI에게 전달하여 분석 기반 답변 생성
- 모든 이미지는 S3에 자동 저장

### 사용자 관리

- Google OAuth2 로그인
- JWT 기반 인증
- 사용자 프로필 관리

## API 엔드포인트

### 질문 생성 (이미지 포함)

```
POST /api/questions/with-images
Content-Type: multipart/form-data

data: {
  "title": "질문 제목",
  "content": "질문 내용",
  "subjectId": 1,
  "professorId": 1
}
images: [파일1, 파일2, ...]
```

### AI 답변 생성 (이미지 포함)

```
POST /api/ai-answers/generate-and-save-with-image
Content-Type: multipart/form-data

data: {
  "questionPostId": 1,
  "prompt": "AI에게 보낼 프롬프트",
  "title": "답변 제목 (선택사항)"
}
image: 파일
```

### 이미지 업로드

```
GET /api/storage/presign?key=파일명
```

## 환경 설정

### S3 설정

```yaml
app:
  s3:
    bucket: ${APP_S3_BUCKET:qandding-s3}
    region: ${APP_S3_REGION:ap-northeast-2}
    upload-prefix: ${APP_S3_UPLOAD_PREFIX:uploads/}
```

### AI 모델 설정

```yaml
app:
  huggingface:
    api-key: ${HUGGINGFACE_API_KEY:your-api-key}
    model: Salesforce/blip-image-captioning-large
```

## 이미지 처리 흐름

1. **질문 생성 시**: 이미지 파일 → S3 업로드 → DB에 URL 저장
2. **AI 답변 생성 시**: 이미지 파일 → S3 업로드 → AI 모델에 전달 → 분석 결과 기반 답변 생성
3. **모든 이미지**: 자동으로 S3에 저장되어 영구 보관

## 기술 스택

- Spring Boot 3.x
- Spring Security + JWT
- Spring Data JPA + QueryDSL
- MySQL

- AWS S3
- Hugging Face AI API
