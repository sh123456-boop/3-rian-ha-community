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
    @Column(nullable = false, unique = true)
    private String s3_key;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Image(String s3_key, User user) {
        this.s3_key = s3_key;
        this.user = user;
    }
}
