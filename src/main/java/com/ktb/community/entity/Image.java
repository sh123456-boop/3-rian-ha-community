package com.ktb.community.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "images")
public class Image extends Timestamped {

    @Id
    @GeneratedValue
    @Column(name = "image_id")
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String s3_key;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

}
