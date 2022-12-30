package com.example.study.controller;

import com.example.study.dto.MemberTeamDto;
import com.example.study.dto.MemberTeamSearchCondition;
import com.example.study.repository.MemberJPARepository;
import com.example.study.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberJPARepository memberJPARepository;
    private final MemberRepository memberRepository;

    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberv1(MemberTeamSearchCondition memberTeamSearchCondition) {
        return memberJPARepository.search(memberTeamSearchCondition);
    }

    @GetMapping("/v2/members")
    public Page<MemberTeamDto> searchMemberv2(MemberTeamSearchCondition memberTeamSearchCondition, Pageable pageable) {
        return memberRepository.searchPageSimple(memberTeamSearchCondition,pageable);
    }

    @GetMapping("/v3/members")
    public Page<MemberTeamDto> searchMemberv3(MemberTeamSearchCondition memberTeamSearchCondition, Pageable pageable) {
        return memberRepository.searchPageComplex(memberTeamSearchCondition,pageable);
    }
}
