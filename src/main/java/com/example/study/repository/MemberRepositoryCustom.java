package com.example.study.repository;

import com.example.study.dto.MemberTeamDto;
import com.example.study.dto.MemberTeamSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberRepositoryCustom {
//    검색기능
    List<MemberTeamDto> search(MemberTeamSearchCondition condition);
    Page<MemberTeamDto> searchPageSimple(MemberTeamSearchCondition condition, Pageable pageable);
    Page<MemberTeamDto> searchPageComplex(MemberTeamSearchCondition condition, Pageable pageable);

}
