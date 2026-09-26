package com.example.firstapp.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.firstapp.entity.PostLike;
import com.example.firstapp.entity.PostLikeId;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {

    boolean existsById_PostIdAndId_UserId(Long postId, Long userId);

    void deleteById_PostIdAndId_UserId(Long postId, Long userId);

    // Batches "did the current user like each of these posts" for a whole feed page
    // in one query instead of one exists-check per post.
    @Query("SELECT pl.id.postId FROM PostLike pl WHERE pl.id.userId = :userId AND pl.id.postId IN :postIds")
    Set<Long> findLikedPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
