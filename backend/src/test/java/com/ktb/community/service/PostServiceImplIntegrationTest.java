package com.ktb.community.service;

import com.ktb.community.dto.response.PostSliceResponseDto;
import com.ktb.community.entity.Image;
import com.ktb.community.entity.Post;
import com.ktb.community.entity.User;
import com.ktb.community.repository.ImageRepository;
import com.ktb.community.repository.PostRepository;
import com.ktb.community.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test") // 위에서 만든 application-test.yml 설정을 사용
class PostServiceImplIntegrationTest {

    @Autowired
    private PostServiceImpl postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImageRepository imageRepository;

    private static final int PAGE_SIZE = 10;

    @BeforeEach
    void setUp() {
        // 각 테스트 실행 전 데이터를 모두 삭제하여 격리성 보장
        postRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        imageRepository.deleteAllInBatch();

        // 테스트 데이터 생성 (50개의 게시글)
        for (int i = 1; i <= 50; i++) {
            Image image = Image.builder().s3Key("profile-" + i).build();

            // ✅ 수정된 부분: 유효성 검사를 통과하는 User 객체 생성
            User user = User.builder()
                    .email("user" + i + "@test.com")
                    .password("password123")   // 유효한 길이의 비밀번호
                    .nickname("testuser" + i)   // 닉네임 추가
                    .image(image)
                    .build();
            User savedUser = userRepository.save(user);

            Post post = Post.builder().title("Title " + i).contents("Content " + i).user(savedUser).build();
            postRepository.save(post);
        }
    }

    @Test
    @DisplayName("게시글 목록 조회(getPostSlice) 동시성 통합 테스트")
    void getPostSlice_concurrencyIntegrationTest() throws InterruptedException {
        // given
        int threadCount = 1000; // 동시에 요청을 보낼 스레드 수

        // when
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<PostSliceResponseDto> results = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 실제 서비스 메서드 호출 (DB 조회 발생)
                    PostSliceResponseDto result = postService.getPostSliceForNPlusOneTest(null);
                    results.add(result);
                } catch (Exception e) {
                    exceptions.add(e); // 예외가 발생하면 기록
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드가 작업을 완료할 때까지 대기
        executorService.shutdown();

        // then
        // 1. 테스트 동안 예외가 발생하지 않았는지 확인
        assertThat(exceptions).isEmpty();

        // 2. 모든 스레드가 결과를 정상적으로 받았는지 확인
        assertThat(results.size()).isEqualTo(threadCount);

        // 3. 모든 스레드가 받은 결과의 내용이 일관되는지 확인
        PostSliceResponseDto firstResult = results.get(0);
        assertThat(firstResult.getPosts()).hasSize(PAGE_SIZE);
        assertThat(firstResult.isHasNext()).isTrue();
        // 첫 번째 게시물의 제목이 'Title 50'인지 확인 (ID 내림차순이므로)
        assertThat(firstResult.getPosts().get(0).getTitle()).isEqualTo("Title 50");
    }
}