package com.rdhxb.smart_building.user.service;

import com.rdhxb.smart_building.user.DTO.UserResponse;
import com.rdhxb.smart_building.user.entity.Role;
import com.rdhxb.smart_building.user.entity.User;
import com.rdhxb.smart_building.user.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepo;


    public List<UserResponse> findAll(){
        return userRepo.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse getUserById(Long id){
        User user = userRepo.findUserById(id);
        return UserResponse.from(user);
    }

    public UserResponse getYourSelf(Principal principal){
        User user =  userRepo.findUserByUsername(principal.getName());
        return UserResponse.from(user);
    }

//    change role
    public void patchRole(long id, Role role){
        User user = userRepo.findUserById(id);
        user.setRole(role);
        userRepo.save(user);
    }

}
