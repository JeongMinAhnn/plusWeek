package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {
    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // TODO: 4. find or save 예제 개선
//    - 개선
//    - DB 접근을 최소화하는 방향으로 수정합니다.
    @Transactional
    public void reportUsers(List<Long> userIds) {
        List<User> users = userRepository.findAllById(userIds);

        if(users.isEmpty()){
            throw new IllegalArgumentException("해당 아이디에 대한 유저 권한인 사용자가 존재하지 않습니다.");
        }
        userRepository.updatePendingStatus(userIds);
    }
}
