package com.dali.dali.domain.runner.service;

import com.dali.dali.domain.community.entity.Community;
import com.dali.dali.domain.community.repository.CommunityRepository;
import com.dali.dali.domain.runner.dto.RunnerDto;
import com.dali.dali.domain.runner.entity.Runner;
import com.dali.dali.domain.runner.repository.RunRepository;
import com.dali.dali.domain.runner.repository.RunnerRepository;
import com.dali.dali.domain.users.entity.User;
import com.dali.dali.domain.users.repository.UserLevelRepository;
import com.dali.dali.domain.users.repository.UserRepository;
import com.dali.dali.global.exception.DuplicateResourceException;
import com.dali.dali.global.exception.NotFoundException;
import com.dali.dali.global.exception.ParticipationAlreadyConfirmException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class RunnerService {
    private final RunRepository runRepository;
    private final RunnerRepository runnerRepository;
    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final UserLevelRepository userLevelRepository;

    public void addRunner(RunnerDto runnerDto, Principal principal) throws Exception {

        Community community = communityRepository.findById(runnerDto.getCommunity_id())
                .orElseThrow(() -> new NotFoundException(runnerDto.getCommunity_id() + " : 글이 존재하지 않습니다."));

        String email = principal.getName();
        Optional<User> optionalUser = userRepository.findByEmail(email);
        User loginUser = optionalUser.get();

        if (community.getCurrentCount() >= community.getUserCount()) {
            throw new IllegalStateException("참여 마감되었습니다.");
        }

        // 이미 참여하기가 눌린 상태라면 에러 반환
        if (runnerRepository.findByUserAndCommunity(loginUser, community).isPresent()) {
            throw new DuplicateResourceException("이미 참여하기 되어있는 상태입니다. : user: " + loginUser.getUserId()
                    + ", community: " + community.getId());
        }

        // 러닝메이트가 종료된 글에 참여하기를 누르면 에러 반환
        if (runnerRepository.existsByCommunityIdAndParticipation(runnerDto.getCommunity_id(), 1)) {
            throw new ParticipationAlreadyConfirmException("이미 달리는 중입니다. 참여할 수 없습니다.");
        }

        // 로그인한 사용자가 해당 커뮤니티의 작성자와 같다면 에러 반환
        if (loginUser.getUserId().equals(community.getUser().getUserId())) {
            throw new IllegalStateException("작성자는 참여할 수 없습니다.");
        }

        Runner runner = Runner.builder()
                .user(loginUser)
                .community(community)
                .build();

        runnerRepository.save(runner);
        runRepository.addRunner(community);
    }

    public void deleteRunner(RunnerDto runnerDto, Principal principal) throws Exception {
        Community community = communityRepository.findById(runnerDto.getCommunity_id())
                .orElseThrow(() -> new NotFoundException(runnerDto.getCommunity_id() + " : 글이 존재하지 않습니다."));

        String email = principal.getName();
        Optional<User> optionalUser = userRepository.findByEmail(email);
        User loginUser = optionalUser.get();

        Runner runner = runnerRepository.findByUserAndCommunity(loginUser, community)
                .orElseThrow(() -> new NotFoundException("참가하기 정보를 찾을 수 없습니다."));

        if (runnerRepository.existsByCommunityIdAndParticipation(runnerDto.getCommunity_id(), 1)) {
            throw new ParticipationAlreadyConfirmException("참가확인이 완료되어 참가 취소를 할 수 없습니다.");
        }

        runnerRepository.delete(runner);
        runRepository.deleteRunner(community);
    }

    public void confirmRunner(RunnerDto runnerDto, Principal principal) throws Exception {

        String email = principal.getName();
        Optional<User> optionalUser = userRepository.findByEmail(email);
        User loginUser = optionalUser.get();

        User user = userRepository.findById(runnerDto.getUser_id())
                .orElseThrow(() -> new NotFoundException(runnerDto.getUser_id() + " : 유저가 존재하지 않습니다."));

        Community community = communityRepository.findById(runnerDto.getCommunity_id())
                .orElseThrow(() -> new NotFoundException(runnerDto.getCommunity_id() + " : 글이 존재하지 않습니다."));

        Optional<Runner> optionalRunner = runnerRepository.findByCommunityId(runnerDto.getCommunity_id());

        if (!optionalRunner.isPresent()) {
            throw new NotFoundException("해당 게시글은 러닝메이트 팀이 존재하지 않습니다.");
        }

        Runner run = optionalRunner.get();

        // 로그인한 사용자가 커뮤니티의 작성자가 아니라면 예외 발생
        if (!loginUser.getUserId().equals(community.getUser().getUserId())) {
            throw new IllegalStateException("작성자만 참여확인을 할 수 있습니다.");
        }

        if (runnerRepository.existsByCommunityIdAndParticipation(run.getCommunity().getId(), 1)) {
            throw new ParticipationAlreadyConfirmException("참가확인이 완료된 러닝메이트입니다.");
        }

        List<Runner> runners = runnerRepository.findByCommunityIdAndParticipation(run.getCommunity().getId(), 0);

        for (Runner runner : runners) {
            runner.setParticipation(1);
            runner.setRunDate(LocalDate.now());
        }

        runnerRepository.saveAll(runners);
        // 레벨 업데이트 구현
        userLevelRepository.updateLevel(user);
    }
}