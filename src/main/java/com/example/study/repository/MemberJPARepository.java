package com.example.study.repository;

import com.example.study.dto.MemberTeamDto;
import com.example.study.dto.MemberTeamSearchCondition;
import com.example.study.dto.QMemberTeamDto;
import com.example.study.entity.Member;
import com.example.study.entity.QMember;
import com.example.study.entity.QTeam;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

@Repository
public class MemberJPARepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public MemberJPARepository(EntityManager em, JPAQueryFactory jpaQueryFactory) {
        this.em = em;
        this.queryFactory = jpaQueryFactory;
    }

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findAll_querydsl() {
//                                  QMember static import해서  생략가능
        return queryFactory.selectFrom(QMember.member)
                .fetch();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = :username ", Member.class)
                .setParameter("username", username)
                .getResultList();
    }

    public List<Member> findByUsername_querydsl(String username) {
        return queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.username.eq(username))
                .fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberTeamSearchCondition memberTeamSearchCondition) {

        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (StringUtils.hasText(memberTeamSearchCondition.getUsername())) {
            booleanBuilder.and(QMember.member.username.eq(memberTeamSearchCondition.getUsername()));
        }

        if (StringUtils.hasText(memberTeamSearchCondition.getTeamName())) {
            booleanBuilder.and(QTeam.team.name.eq(memberTeamSearchCondition.getTeamName()));
        }

        if (memberTeamSearchCondition.getAgeGoe() != null) {
            booleanBuilder.and(QMember.member.age.goe(memberTeamSearchCondition.getAgeGoe()));
        }

        if (memberTeamSearchCondition.getAgeLoe() != null) {
            booleanBuilder.and(QMember.member.age.loe(memberTeamSearchCondition.getAgeLoe()));
        }

        return queryFactory
                .select(new QMemberTeamDto(
                        QMember.member.id.as("memberid"),
                        QMember.member.username,
                        QMember.member.age,
                        QTeam.team.id.as("teamId"),
                        QTeam.team.name.as("teamName")))
                .from(QMember.member)
                .leftJoin(QMember.member.team, QTeam.team)
                .where(booleanBuilder)
                .fetch();
    }

//    빌더부분 달라짐 (용도에따라)
    public List<MemberTeamDto> search(MemberTeamSearchCondition memberTeamSearchCondition) {


        return queryFactory
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
