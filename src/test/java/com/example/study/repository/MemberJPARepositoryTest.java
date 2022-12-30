package com.example.study.repository;

import com.example.study.dto.MemberTeamDto;
import com.example.study.dto.MemberTeamSearchCondition;
import com.example.study.entity.Member;
import com.example.study.entity.Team;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest
class MemberJPARepositoryTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    MemberJPARepository memberJPARepository;

    @Test
    public void basicTest() {
        Member member = new Member("member1", 30);
        memberJPARepository.save(member);

        /**
         * 각각 위 JPQL / 아래 QueryDsl 방법 사용
         */

        Member findMember = memberJPARepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);


//        List<Member> resultList = memberJPARepository.findAll();
        List<Member> resultList = memberJPARepository.findAll_querydsl();


        assertThat(resultList).containsExactly(member);


        List<Member> result1 = memberJPARepository.findByUsername("member1");
        assertThat(result1).containsExactly(member);
    }

    @Test
    public void searchTest() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        entityManager.persist(teamA);
        entityManager.persist(teamB);

        Member member1 = new Member("member1", 21, teamA);
        Member member2 = new Member("member2", 25, teamA);

        Member member3 = new Member("member3", 24, teamB);
        Member member4 = new Member("member4", 24, teamB);

        entityManager.persist(member1);
        entityManager.persist(member2);
        entityManager.persist(member3);
        entityManager.persist(member4);


        MemberTeamSearchCondition memberTeamSearchCondition = new MemberTeamSearchCondition();
//        동적쿼리 조건 3가지 축족해야 테스트 성공
        memberTeamSearchCondition.setAgeGoe(15);
        memberTeamSearchCondition.setAgeLoe(23);
        memberTeamSearchCondition.setTeamName("teamA");

//        List<MemberTeamDto> result = memberJPARepository.searchByBuilder(memberTeamSearchCondition);
        List<MemberTeamDto> result = memberJPARepository.search(memberTeamSearchCondition);

        assertThat(result).extracting("username").containsExactly("member1");
    }


}