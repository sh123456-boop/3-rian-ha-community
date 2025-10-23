package com.ktb.community.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import com.ktb.community.dto.response.PostSliceResponseDto;
import com.ktb.community.entity.Image;
import com.ktb.community.entity.Post;
import com.ktb.community.entity.User;
import com.ktb.community.repository.PostRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {


    @InjectMocks
    private PostServiceImpl postService;

    @Mock
    private PostRepository postRepository;

    private static final int PAGE_SIZE = 10;

    @Test
    @DisplayName("게시글 목록 조회(getPostSlice) 동시성 단위 테스트")
    void getPostSlice() throws InterruptedException{

        // 동시에 요청을 보낼 스레드(사용자) 수
        int threadCount = 1000;

        // @Value 필드 값 주입
        ReflectionTestUtils.setField(postService, "cloudfrontDomain", "test.cloudfront.net");
        ReflectionTestUtils.setField(postService, "defaultProfileImageKey", "default-key");

        // Mock 데이터 생성
        List<Post> mockPosts = IntStream.range(0, PAGE_SIZE)
                .mapToObj(i -> {
                    User user = User.builder().id((long) i).build();
                    Image image = Image.builder().id((long)i).s3Key("user-profile-" + i).build();
                    // 리플렉션을 사용해 'image' 라는 이름의 필드에 image 객체를 직접 주입
                    ReflectionTestUtils.setField(user, "image", image);
                    return Post.builder().id((long)i).title("Title " + i).user(user).build();
                })
                .collect(Collectors.toList());

        // Mock Repository 설정 : 어떤 Pageable이 들어오든 항상 동일한 Slice<Post>를 반환하도록 설정
        Slice<Post> mockSlice = new SliceImpl<>(mockPosts, PageRequest.of(0, PAGE_SIZE), true);
        when(postRepository.findSliceByOrderByIdDesc(any(Pageable.class))).thenReturn(mockSlice);

        //when
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<PostSliceResponseDto> results  = Collections.synchronizedList(new ArrayList<>());


        for (int i = 0 ; i< threadCount ; i++) {
            executorService.submit(()->{
                try {
                    //서비스 메서드 호출
                    PostSliceResponseDto result = postService.getPostSlice(null);
                    results.add(result);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드가 작업을 완료할 때까지 대기
        executorService.shutdown();

        //then
        // 1. Repository 메서드가 스레드 수만큼 정확히 호출되었는지 확인
        verify(postRepository, times(threadCount)).findSliceByOrderByIdDesc(any(Pageable.class));

        // 2. 모든 스레드가 결과를 정상적으로 받았는지 확인
        assertThat(results.size()).isEqualTo(threadCount);

        // 3. 모든 스레드가 받은 결과가 일관성이 있는지 확인 (첫 번째 결과와 마지막 결과를 비교)
        PostSliceResponseDto firstResult = results.get(0);
        PostSliceResponseDto lastResult = results.get(threadCount - 1);

        assertThat(firstResult.isHasNext()).isTrue();
        assertThat(firstResult.getPosts()).hasSize(PAGE_SIZE);

        //DTO 내용 비교( 객체끼리 비교하면 주소값이 달라 실패하므로, 특정 필드 값으로 비교)
        assertThat(firstResult.getPosts().get(0).getTitle())
                .isEqualTo(lastResult.getPosts().get(0).getTitle());

        // 생성된 프로필 이미지 URL이 올바른지 확인
        String expectedProfileUrl = "https://test.cloudfront.net/user-profile-0";
        assertThat(firstResult.getPosts().get(0).getAuthorProfileImageUrl()).isEqualTo(expectedProfileUrl);
    }

    @Test
    @DisplayName("게시글 목록 조회(getPostSlice) 동시성 통합 테스트")
    void getPostSlice2() throws InterruptedException{

    }
}