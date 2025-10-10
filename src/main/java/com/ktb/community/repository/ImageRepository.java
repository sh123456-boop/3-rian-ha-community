package com.ktb.community.repository;

import com.ktb.community.entity.Image;
import com.ktb.community.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {
    Optional<Image> findByS3Key(String s3Key);
}
