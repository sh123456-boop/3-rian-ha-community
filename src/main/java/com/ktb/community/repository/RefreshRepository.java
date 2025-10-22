package com.ktb.community.repository;

import com.ktb.community.entity.RefreshEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshRepository extends JpaRepository<RefreshEntity, Long> {

    Boolean existsByRefresh(String refresh);


    // 필터에서 검증하므로 repository에 어노테이션 붙임
    @Transactional
    void deleteByRefresh(String refresh);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update RefreshEntity r
           set r.refresh = :newRefresh,
               r.expiration = :expiration
         where r.userId = :userId
           and r.refresh = :oldRefresh
    """)
    int rotateRefresh(@Param("userId") Long userId,
                      @Param("oldRefresh") String oldRefresh,
                      @Param("newRefresh") String newRefresh,
                      @Param("expiration") String expiration);
}
