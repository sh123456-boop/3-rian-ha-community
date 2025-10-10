package com.ktb.community.repository;

import com.ktb.community.entity.RefreshEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshRepository extends JpaRepository<RefreshEntity, Long> {

    Boolean existsByRefresh(String refresh);


    // 필터에서 검증하므로 repository에 어노테이션 붙임
    @Transactional
    void deleteByRefresh(String refresh);
}
