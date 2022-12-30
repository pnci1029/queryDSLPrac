package com.example.study.repository;

import com.example.study.dto.MemberTeamDto;
import com.example.study.dto.MemberTeamSearchCondition;
import com.example.study.dto.QMemberTeamDto;
import com.example.study.entity.Member;
import com.example.study.entity.QMember;
import com.example.study.entity.QTeam;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

public class MemberRepositoryImpl implements MemberRepositoryCustom{
    /**
     * 실제로 사용하게 될 부분
     */

    private final JPAQueryFactory jpaQueryFactory;

//    스프링 빈에 등록되서 생성자로 바로 만들수있음
    public MemberRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public List<MemberTeamDto> search(MemberTeamSearchCondition memberTeamSearchCondition) {
        return jpaQueryFactory
                .select(new QMemberTeamDto(
                        QMember.member.id.as("memberid"),
                        QMember.member.username,
                        QMember.member.age,
                        QTeam.team.id.as("teamId"),
                        QTeam.team.name.as("teamName")))
                .from(QMember.member)
                .leftJoin(QMember.member.team, QTeam.team)
                .where(
                        usernameEq(memberTeamSearchCondition.getUsername()),
                        teamNameEq(memberTeamSearchCondition.getTeamName()),
                        ageGoe(memberTeamSearchCondition.getAgeGoe()),
                        ageLoe(memberTeamSearchCondition.getAgeLoe())
                )
                .fetch();
    }

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberTeamSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = jpaQueryFactory
                .select(new QMemberTeamDto(
                        QMember.member.id.as("memberid"),
                        QMember.member.username,
                        QMember.member.age,
                        QTeam.team.id.as("teamId"),
                        QTeam.team.name.as("teamName")))
                .from(QMember.member)
                .leftJoin(QMember.member.team, QTeam.team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
//                offset 시작점
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberTeamSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> results = jpaQueryFactory
                .select(new QMemberTeamDto(
                        QMember.member.id.as("memberid"),
                        QMember.member.username,
                        QMember.member.age,
                        QTeam.team.id.as("teamId"),
                        QTeam.team.name.as("teamName")))
                .from(QMember.member)
                .leftJoin(QMember.member.team, QTeam.team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
//                offset 시작점
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

//        토탈 카운트용 쿼리 작성
        JPAQuery<Member> countQuery = jpaQueryFactory
                .select(QMember.member)
                .from(QMember.member)
                .leftJoin(QMember.member.team, QTeam.team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );
//                .fetchCount();



//        return new PageImpl<>(results, pageable, count);
        return PageableExecutionUtils.getPage(results, pageable, countQuery::fetchCount);
    }

    private BooleanExpression usernameEq(String username) {
        return StringUtils.hasText(username) ? QMember.member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return StringUtils.hasText(teamName) ? QTeam.team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? QMember.member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? QMember.member.age.loe(ageLoe) : null;
    }

}

