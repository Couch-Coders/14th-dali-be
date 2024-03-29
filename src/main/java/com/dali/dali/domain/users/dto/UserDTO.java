package com.dali.dali.domain.users.dto;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO implements Serializable {
    private Long user_id;
    private String name;
    private String gender;
    private String email;
    private String nickname;
    private String profile;
    private int level;
}
