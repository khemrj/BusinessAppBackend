package com.example.firstapp.repository;

import java.util.List;

import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.firstapp.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = {"author"})
    @Query("""
        SELECT p FROM Post p
        WHERE p.visibility = 'PUBLIC'
           OR p.author.id = :currentUserId
           OR (p.visibility = 'CONNECTIONS' AND p.author.id IN :connectionIds)
        ORDER BY p.createdAt DESC
        """)
    Slice<Post> findFeed(@Param("currentUserId") Long currentUserId,
                          @Param("connectionIds") List<Long> connectionIds,
                          Pageable pageable);

    // Atomic counters: a read-modify-write (read count, +1, save) loses updates under
    // concurrent likes — two requests can both read 5 and both write 6 instead of 7.
    // A direct UPDATE is atomic at the row level and needs no extra locking.
    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
    void incrementLikeCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = :postId AND p.likeCount > 0")
    void decrementLikeCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    void incrementCommentCount(@Param("postId") Long postId);
}
