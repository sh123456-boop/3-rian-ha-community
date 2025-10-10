package com.ktb.community.service;

import com.ktb.community.dto.response.PreSignedUrlResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Presigner s3Presigner;
    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);
    private final S3Client s3Client;

    @Value("${aws.bucket}")
    String bucket;

    /**
     * Pre-signed URL과 S3 Key를 생성하여 반환합니다.
     //@param userId 현재 사용자 ID
     //@param fileName 원본 파일명
     * @return PreSignedUrlResponseDto (내부에 key와 url을 포함하는 DTO)
     */
    // 파일 업로드용 Presigned URL 생성
    public PreSignedUrlResponseDto getPresignedPutUrl(Long userId, String fileName) {
        // 1. UUID를 통해 고유한 파일명 생성
        String uuid = UUID.randomUUID().toString();
        String uniqueFileName = uuid + "-" + fileName;

        // 2. S3에 저장될 전체 경로(s3_key) 설정
        String s3_key = "posts/" + userId + "/" + uniqueFileName;

        // 3. pre-signed url 생성에 필요한 요청 객체 생성
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3_key)
                // .contentType("image/jpeg) // 필요시 Content-Type 지정
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10)) // URL 유효 시간
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        // 4. url 생성
        String presignedUrl = presignedRequest.url().toString();
        return new PreSignedUrlResponseDto(s3_key, presignedUrl);
    }

    // s3 파일 삭제
    public void deleteFile(String s3Key) {
        try {
            // 삭제할 객체를 지정하는 요청 객체 생성
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build();

            // S3 클라이언트로 삭제 명령 실행
            s3Client.deleteObject(deleteObjectRequest);
            logger.info("S3에서 파일 삭제 성공: {}", s3Key);

        } catch (S3Exception e) {
            throw new RuntimeException("\"S3 파일 삭제 실패: \" + s3Key, e");
        }
    }
}
