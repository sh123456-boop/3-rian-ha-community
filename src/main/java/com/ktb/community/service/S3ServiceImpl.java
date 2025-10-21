package com.ktb.community.service;

import com.ktb.community.dto.response.PreSignedUrlResponseDto;
import com.ktb.community.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

import static com.ktb.community.exception.ErrorCode.INTERNAL_SERVER_ERROR;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class S3ServiceImpl implements S3Service {

    private final S3Presigner s3Presigner;
    private static final Logger logger = LoggerFactory.getLogger(S3ServiceImpl.class);
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
    @Transactional
    public PreSignedUrlResponseDto getPostPresignedPutUrl(Long userId, String fileName) {

        // 1. S3에 저장될 전체 경로(s3_key) 설정
        String s3_key = "posts/" + userId + "/" + createUniqueFileName(fileName);

        // 2. presigendUrl 생성
        String presignedUrl = generatePresignedUrl(s3_key);
        return new PreSignedUrlResponseDto(s3_key, presignedUrl);
    }

    // 유저 프로필용 Presigned Url 생성
    @Transactional
    public PreSignedUrlResponseDto getProfileImagePresignedUrl(Long userId, String fileName) {

        // S3 키 경로를 프로필 이미지용으로 지정
        String s3_key = "profiles/" + userId + "/" + createUniqueFileName(fileName);

        // 2. presigendUrl 생성
        String presignedUrl = generatePresignedUrl(s3_key);
        return new PreSignedUrlResponseDto(s3_key, presignedUrl);
    }

    // s3 파일 삭제
    @Transactional
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
            throw new BusinessException(INTERNAL_SERVER_ERROR);
        }
    }

    // 고유 파일명을 만드는 공통 메서
    private String createUniqueFileName(String fileName) {
        return UUID.randomUUID().toString() + "-" + fileName;
    }

    // 3. 실제 URL 생성 로직을 담당하는 공통 메서드
    private String generatePresignedUrl(String s3Key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

}
