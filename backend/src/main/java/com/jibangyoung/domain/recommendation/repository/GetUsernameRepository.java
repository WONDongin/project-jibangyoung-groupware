// backend/src/main/java/com/jibangyoung/domain/auth/repository/UserRepository.java
package com.jibangyoung.domain.recommendation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jibangyoung.domain.auth.entity.User;

@Repository
public interface GetUsernameRepository extends JpaRepository<User, Long> {
    @Query("SELECT nickname FROM User u WHERE u.id = :userId")
    String getUsernameById(@Param("userId") Long userId);
}
