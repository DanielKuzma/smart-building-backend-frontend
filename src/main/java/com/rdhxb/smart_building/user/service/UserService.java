package com.rdhxb.smart_building.user.service;

import com.rdhxb.smart_building.user.entity.User;
import com.rdhxb.smart_building.user.repo.UserRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }


    public List<User> findAll(){
        return userRepo.findAll();
    }

}
