package com.minisocial.repository;

import com.minisocial.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Modifying
    @Query("UPDATE User u SET u.followingCount = u.followingCount + 1 WHERE u.id = :userId")
    int incFollowing(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.followerCount = u.followerCount + 1 WHERE u.id = :userId")
    int incFollowers(@Param("userId") Long userId);

    @Modifying
    @Query("""
        UPDATE User u
           SET u.followingCount = CASE WHEN u.followingCount > 0 THEN u.followingCount - 1 ELSE 0 END
         WHERE u.id = :userId
    """)
    int decFollowing(@Param("userId") Long userId);

    @Modifying
    @Query("""
        UPDATE User u
           SET u.followerCount = CASE WHEN u.followerCount > 0 THEN u.followerCount - 1 ELSE 0 END
         WHERE u.id = :userId
    """)
    int decFollowers(@Param("userId") Long userId);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
