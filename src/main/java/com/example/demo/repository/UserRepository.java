package com.example.demo.repository;

import com.example.demo.entity.Item;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
  @Modifying
  @Query("UPDATE User u SET u.status = 'PENDING' WHERE u.status = 'APPROVED' AND u.role = 'USER' AND u.id IN :userIds")
  void updatePendingStatus(List<Long> userIds);
  default User findUserById(Long id) {
    return findById(id).orElseThrow(() -> new IllegalArgumentException("해당 ID에 맞는 값이 존재하지 않습니다."));
  }
}
