package com.minisocial.repository;

import com.minisocial.model.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = "author") // avoids lazy issues when mapping authorId
    @Query("""
        select p from Post p
        left join Follow f
            on f.following = p.author
           and f.follower.id = :userId
        order by
            case
                when f.id is not null or p.author.id = :userId then 0
                else 1
            end,
            p.createdAt desc
    """)
    List<Post> findFeedForUser(@Param("userId") Long userId, Pageable pageable);

    List<Post> findByAuthorIdIn(List<Long> authorIds, Pageable pageable);
}
