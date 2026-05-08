package com.rdhxb.smart_building.user.service;

import com.rdhxb.smart_building.user.entity.Role;
import com.rdhxb.smart_building.user.entity.User;
import com.rdhxb.smart_building.user.repo.UserRepo;
import org.springframework.stereotype.Service;

import java.security.Principal;
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

    public User getUserById(Long id){
        return userRepo.findUserById(id);
    }

    public User getYourSelf(Principal principal){
        return userRepo.findUserByUsername(principal.getName());
    }

//    change role
    public void patchRole(long id, Role role){
        User user = userRepo.findUserById(id);
        user.setRole(role);
        userRepo.save(user);
    }

}
