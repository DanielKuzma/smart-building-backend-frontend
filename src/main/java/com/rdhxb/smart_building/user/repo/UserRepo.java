package com.rdhxb.smart_building.user.repo;

import com.rdhxb.smart_building.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<User,Long> {
    User findUserByUsername(String username);

    boolean existsUserByUsername(String username);
}
