package com.ktb.community.dto.response;

import com.ktb.community.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor
public class PostResponseDto {
    private Long postId;
    private String title;
    private String content;
    private String nickname;
    private Instant updatedAt;
    private List<ImageInfo> images;// 이미지 정보 리스트를 내부에 포함
    private String authorProfileImageUrl;
    private Long userId;
    private boolean likedByUser;

    private int viewCount;
    private int likeCount;
    private int commentCount;


    // 재사용을 위한 내부 DTO (또는 별도 파일로 분리 가능)
    @Getter
    public static class ImageInfo {
        private String imageUrl; // 최종 CloudFront URL
        private int order;
        private String s3_key;

        public ImageInfo(String imageUrl, int order, String s3_key) {
            this.imageUrl = imageUrl;
            this.order = order;
            this.s3_key = s3_key;
        }
    }

    public PostResponseDto(Post post, List<ImageInfo> list, String authorProfileImageUrl, Long userId, boolean likedByUser) {
        this.postId = post.getId();
        this.title = post.getTitle();
        this.content = post.getContents();
        this.nickname = post.getUser().getNickname();
        this.updatedAt = post.getUpdatedAt();
        this.images = list;
        this.viewCount = post.getPostCount().getView_cnt();
        this.likeCount = post.getPostCount().getLikes_cnt();
        this.commentCount = post.getPostCount().getCmt_cnt();
        this.authorProfileImageUrl = authorProfileImageUrl;
        this.userId = userId;
        this.likedByUser = likedByUser;
    }


}
