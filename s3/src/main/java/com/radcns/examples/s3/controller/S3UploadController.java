package com.radcns.examples.s3.controller;

import com.radcns.examples.s3.config.properties.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;


import net.coobird.thumbnailator.Thumbnails;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class S3UploadController {

    private final S3Client s3Client;
    private final AwsProperties awsProperties;
    private String imageServerUrl;

    @Value("${image.base.url}")
    public void setImageBaseUrl(String imageBaseUrl) {
        this.imageServerUrl = imageBaseUrl;
    }

    /**
     * 일반 업로드
     * @param file
     * @return
     */

    @ResponseBody
    @PostMapping("/fileupload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        try {

            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";

            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String fileName = String.format("%s%s", UUID.randomUUID().toString(),fileExtension);

            //-- 업로드 폴더 생성(예제는 날짜)
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String key = String.format("%s/%s/%s", awsProperties.getUploadPath(),today,fileName);

            log.info("파일/s3 정보 file: {} to S3 bucket: {} with key: {}", originalFilename, awsProperties.getBucket(), key);

            // S3에 파일 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(awsProperties.getBucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            PutObjectResponse response = s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("썸네일 업로드 완료. ETag: {}", response.eTag());

            // 업로드된 파일의 URL 반환
            String fileUrl = String.format("%s/%s", imageServerUrl, key);
            return ResponseEntity.ok(fileUrl);

        } catch (IOException e) {
            log.error("IOException : ", e);
            return ResponseEntity.internalServerError().body("upload failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Exception : ", e);
            return ResponseEntity.internalServerError().body("error: " + e.getMessage());
        }
    }


    /**
     * 썸네일 + 오리지널 업로드
     * @param file
     * @return
     */

    @ResponseBody
    @PostMapping("/fileupload/thumbnail")
    public ResponseEntity<Map<String, Object>> uploadThumbnail(@RequestParam("file") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();

        try {
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String fileName = String.format("%s%s", UUID.randomUUID().toString(),fileExtension);

            //-- 업로드 폴더 생성(예제는 날짜)
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String originalKey = String.format("%s/%s/%s", awsProperties.getUploadPath(),today,fileName);

            log.info("파일/s3 정보 file: {} to S3 bucket: {} with key: {}", originalFilename, awsProperties.getBucket(), originalKey);

            //-- 원본 업로드
            PutObjectRequest originalRequest = PutObjectRequest.builder()
                    .bucket(awsProperties.getBucket())
                    .key(originalKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            PutObjectResponse originalResponse = s3Client.putObject(originalRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("원본파일 업로드 롼료. ETag: {}", originalResponse.eTag());

            //---------------------------
            //-- 썸네일 + 업로드
            //---------------------------

            String thumbUrl = generateThumb(file, today, fileName);

            String originalUrl = String.format("%s/%s", imageServerUrl, originalKey);
            response.put("success", true);
            response.put("original", originalUrl);
            response.put("thumbnail", thumbUrl);
            response.put("originalSize", file.getSize());
            response.put("fileName", originalFilename);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("IO Exception", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        } catch (Exception e) {
            log.error("Exception : ", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }


    /**
     * 썸네일 업로드 함수
     * @param originalFile
     * @param today
     * @param originalFileName
     * @return
     * @throws IOException
     */
    private String generateThumb(MultipartFile originalFile, String today, String originalFileName) throws IOException {

        String thumbnailFileName = "thumb_" + originalFileName;
        String thumbnailKey = String.format("%s/%s/thumbnails/%s", awsProperties.getUploadPath(), today, thumbnailFileName);


        log.info("썸네일/s3 정보 file: {} to S3 bucket: {} with key: {}", thumbnailFileName, awsProperties.getBucket(), thumbnailKey);

        // 썸네일 생성
        ByteArrayOutputStream thumbnailOutputStream = new ByteArrayOutputStream();
        Thumbnails.of(originalFile.getInputStream())
                .size(300, 300)
                .keepAspectRatio(true)
                .outputQuality(0.8)
                .toOutputStream(thumbnailOutputStream);

        byte[] thumbnailBytes = thumbnailOutputStream.toByteArray();

        //-- s3업로드
        PutObjectRequest thumbnailRequest = PutObjectRequest.builder()
                .bucket(awsProperties.getBucket())
                .key(thumbnailKey)
                .contentType("image/jpeg")
                .contentLength((long) thumbnailBytes.length)
                .build();

        PutObjectResponse thumbnailResponse = s3Client.putObject(thumbnailRequest,
                RequestBody.fromInputStream(new ByteArrayInputStream(thumbnailBytes), thumbnailBytes.length));

        log.info("썸네일 업로드 완료. ETag: {}", thumbnailResponse.eTag());

        // 썸네일 URL 반환
        return String.format("%s/%s", imageServerUrl, thumbnailKey);
    }


    /**
     * 클라이언트에서 생성한 썸네일과 원본 업로드
     * @param originalFile 원본 이미지 파일
     * @param thumbnailFile 클라이언트에서 생성한 썸네일 파일
     * @return 업로드 결과
     */
    @ResponseBody
    @PostMapping("/fileupload/client/thumbnail")
    public ResponseEntity<Map<String, Object>> uploadClientThumbnail(@RequestParam("original") MultipartFile originalFile, @RequestParam("thumbnail") MultipartFile thumbnailFile) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 파일 검증
            if (originalFile.isEmpty() || thumbnailFile.isEmpty()) {
                response.put("success", false);
                response.put("error", "파일이 비어있습니다.");
                return ResponseEntity.badRequest().body(response);
            }

            String originalFilename = originalFile.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String fileName = String.format("%s%s", UUID.randomUUID().toString(), fileExtension);

            // 업로드 폴더 생성(예제는 날짜)
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String originalKey = String.format("%s/%s/%s", awsProperties.getUploadPath(), today, fileName);
            String thumbnailKey = String.format("%s/%s/thumbnails/%s", awsProperties.getUploadPath(), today, "thumb_" + fileName);

            log.info("클라이언트 썸네일 업로드 - 원본: {} to S3 bucket: {} with key: {}", originalFilename, awsProperties.getBucket(), originalKey);
            log.info("클라이언트 썸네일 업로드 - 썸네일: {} to S3 bucket: {} with key: {}", "thumb_" + fileName, awsProperties.getBucket(), thumbnailKey);

            // 원본 파일 S3 업로드
            PutObjectRequest originalRequest = PutObjectRequest.builder()
                    .bucket(awsProperties.getBucket())
                    .key(originalKey)
                    .contentType(originalFile.getContentType())
                    .contentLength(originalFile.getSize())
                    .build();

            PutObjectResponse originalResponse = s3Client.putObject(originalRequest,
                    RequestBody.fromInputStream(originalFile.getInputStream(), originalFile.getSize()));

            log.info("원본파일 업로드 완료. ETag: {}", originalResponse.eTag());

            // 썸네일 파일 S3 업로드
            PutObjectRequest thumbnailRequest = PutObjectRequest.builder()
                    .bucket(awsProperties.getBucket())
                    .key(thumbnailKey)
                    .contentType(thumbnailFile.getContentType())
                    .contentLength(thumbnailFile.getSize())
                    .build();

            PutObjectResponse thumbnailResponse = s3Client.putObject(thumbnailRequest,
                    RequestBody.fromInputStream(thumbnailFile.getInputStream(), thumbnailFile.getSize()));

            log.info("클라이언트 썸네일 업로드 완료. ETag: {}", thumbnailResponse.eTag());

            // 응답 데이터 구성
            String originalUrl = String.format("%s/%s", imageServerUrl, originalKey);
            String thumbnailUrl = String.format("%s/%s", imageServerUrl, thumbnailKey);

            response.put("success", true);
            response.put("original", originalUrl);
            response.put("thumbnail", thumbnailUrl);
            response.put("originalSize", originalFile.getSize());
            response.put("thumbnailSize", thumbnailFile.getSize());
            response.put("fileName", originalFilename);
            response.put("message", "클라이언트 썸네일 업로드 성공");

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("IO Exception", e);
            response.put("success", false);
            response.put("error", "파일 처리 중 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        } catch (Exception e) {
            log.error("Exception : ", e);
            response.put("success", false);
            response.put("error", "업로드 중 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

}
