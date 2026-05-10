package com.rdhxb.smart_building.user.DTO;


import com.rdhxb.smart_building.user.entity.Role;
import com.rdhxb.smart_building.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private Role role;


    public static UserResponse from(User user){
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }
}
