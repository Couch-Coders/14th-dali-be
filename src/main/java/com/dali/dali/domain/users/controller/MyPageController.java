package com.dali.dali.domain.users.controller;

import com.dali.dali.domain.runner.dto.RunnerDto;
import com.dali.dali.domain.runner.service.RunnerService;
import com.dali.dali.domain.users.dto.MyPageDTO;
import com.dali.dali.domain.users.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/mypage")
@AllArgsConstructor
public class MyPageController {
    private final UserService userService;
    private final RunnerService runnerService;
    @GetMapping
    public MyPageDTO getMyPageInfo(Principal principal) {
        return userService.getMyPageInfo(principal);
    }

    @PostMapping("/runner")
    public void confirmRunner(@RequestBody RunnerDto runnerDto, Principal principal) throws Exception {
        runnerService.confirmRunner(runnerDto, principal);
    }
}
