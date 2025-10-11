package com.ktb.community.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "images")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Image  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;

    @NotBlank
    @Column(name = "s3_key", nullable = false, unique = true)
    private String s3Key;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Image(String s3Key, User user) {
        this.s3Key = s3Key;
        this.user = user;
    }
}
