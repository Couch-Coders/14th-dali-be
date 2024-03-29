package com.dali.dali.domain.community.service;

import com.dali.dali.domain.city.entity.City;
import com.dali.dali.domain.city.repository.CityRepository;
import com.dali.dali.domain.community.dto.CommunityDto;
import com.dali.dali.domain.community.entity.AMPM;
import com.dali.dali.domain.community.entity.Community;
import com.dali.dali.domain.community.entity.Gender;
import com.dali.dali.domain.community.entity.Time;
import com.dali.dali.domain.community.repository.CommunityRepository;
import com.dali.dali.domain.community.repository.CommunitySpecifications;
import com.dali.dali.domain.users.entity.User;
import com.dali.dali.domain.users.repository.UserRepository;
import com.dali.dali.global.exception.NotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {
    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final CityRepository cityRepository;

    private User getLoggedInUser(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        String email = principal.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("로그인 한 유저 정보를 찾을 수 없습니다."));
    }

    @Transactional
    public Community createPost(CommunityDto communityDto, Principal principal) {

        User loginUser = getLoggedInUser(principal);

        City city = cityRepository.findById(communityDto.getCity_code())
                .orElseThrow(() -> new EntityNotFoundException("지역이 존재하지 않습니다."));

        Community community = Community.builder()
                .title(communityDto.getTitle())
                .content(communityDto.getContent())
                .gender(communityDto.getGender())
                .ampm(communityDto.getAmpm())
                .time(communityDto.getTime())
                .userCount(communityDto.getUserCount())
                .regDate(LocalDateTime.now())
                .user(loginUser)
                .city(city)
                .build();

        return communityRepository.save(community);
    }

    public Community getPost(Long id, Principal principal) {

        User loginUser = getLoggedInUser(principal);

        return communityRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글이 존재하지 않습니다.")
        );
    }

    public Page<Community> getPosts(Pageable pageable, AMPM ampm,
                                    Time time, Gender gender,
                                    String sido, String sigungu, String dong,
                                    Principal principal) {

        User loginUser = getLoggedInUser(principal);

        Specification<Community> spec = Specification.where(null);

        if (gender != null) {
            spec = spec.and(CommunitySpecifications.hasGender(gender));
        }

        if (ampm != null) {
            spec = spec.and(CommunitySpecifications.hasAmpm(ampm));
        }

        if (time != null) {
            spec = spec.and(CommunitySpecifications.hasTime(time));
        }

        if (sido != null) {
            spec = spec.and(CommunitySpecifications.hasSido(sido));
        }

        if (sido != null &&sigungu != null) {
            spec = spec.and(CommunitySpecifications.hasSido(sido));
            spec = spec.and(CommunitySpecifications.hasSigungu(sigungu));
        }

        if (sido != null &&sigungu != null && dong != null) {
            spec = spec.and(CommunitySpecifications.hasSido(sido));
            spec = spec.and(CommunitySpecifications.hasSigungu(sigungu));
            spec = spec.and(CommunitySpecifications.hasDong(dong));
        }

        return communityRepository.findAll(spec, pageable);
    }

    @Transactional
    public Community updatePost(Long id, CommunityDto communityDto, Principal principal) {

        User loginUser = getLoggedInUser(principal);

        Community community = communityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정할 게시글이 존재하지 않습니다."));

        User communityUser = community.getUser();

        if (!loginUser.getEmail().equals(communityUser.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "작성자만 게시글 수정이 가능합니다.");
        }

        City city = cityRepository.findById(communityDto.getCity_code())
                .orElseThrow(() -> new EntityNotFoundException("지역이 존재하지 않습니다."));

        community.updatePost(communityDto, loginUser, city);
        return communityRepository.save(community);
    }

    @Transactional
    public Community deletePost(Long id, Principal principal) {
        Community community = communityRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "삭제할 게시글이 존재하지 않습니다.")
        );

        User loginUser = getLoggedInUser(principal);
        User communityUser = community.getUser();

        if (!loginUser.getEmail().equals(communityUser.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "작성자만 게시글 삭제가 가능합니다.");
        }

        communityRepository.deleteById(id);
        return community;
    }
}
