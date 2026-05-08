package com.rdhxb.smart_building.user.controller;

import com.rdhxb.smart_building.user.entity.Role;
import com.rdhxb.smart_building.user.entity.User;
import com.rdhxb.smart_building.user.service.UserService;
import jakarta.transaction.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> findAll(){
        return userService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public User getUserById(@PathVariable long id){
        return userService.getUserById(id);
    }


    @GetMapping("/me")
    public User getYourSelf(Principal principal){
        return userService.getYourSelf(principal);
    }

    @PostMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void patchRole(@PathVariable long id, @RequestParam Role role){
        userService.patchRole(id,role);
    }

}
