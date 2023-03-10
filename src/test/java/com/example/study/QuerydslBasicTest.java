package com.example.study;

import com.example.study.dto.MemberDto;
import com.example.study.dto.QMemberDto;
import com.example.study.dto.UserDto;
import com.example.study.entity.Member;
import com.example.study.entity.QMember;
import com.example.study.entity.QTeam;
import com.example.study.entity.Team;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.transaction.Transactional;

import java.util.List;

import static com.example.study.entity.QMember.*;
import static com.example.study.entity.QTeam.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    JPAQueryFactory jpaQueryFactory;


    @Autowired
    EntityManager entityManager;

    @BeforeEach
    public void before() {
        jpaQueryFactory = new JPAQueryFactory(entityManager);
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
    }

    //  querydsl vs JPQL
//    JPQL
    @Test
    public void startJPQL() {
//        member 1??? ?????????
        Member findByJPQL = entityManager.createQuery("select m from Member m where " +
                        "m.username= : username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findByJPQL.getUsername().equals("member1"));

    }

    //    querydsl
    @Test
    public void startQueryDSL() {
//        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(entityManager);

        Member targetMember = jpaQueryFactory
                /**
                 * static import  ?????? ????????? Qmember.member ??????
                 * ???????????? ????????? select from where
                 */
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))  // ???????????? ?????????
                .fetchOne();

        assertThat(targetMember.getUsername().equals("member1"));
    }

    //    And??? ??????
    @Test
    public void search() {
        Member resultMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(21)))
                .fetchOne();

        assertThat(resultMember.getUsername().equals("member1"));
    }

    //   ??? ?????? ?????? .and ?????? or ????????????(?????????????????? ??????  ???????????? ?????????  -> ??? ??????)
    @Test
    public void searchAndParam() {
        Member resultMember = jpaQueryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(21))
                .fetchOne();

        assertThat(resultMember.getUsername().equals("member1"));
    }

    @Test
    public void resultFetch() {
////        ????????? ??????
//        List<Member> fetch = jpaQueryFactory
//                .selectFrom(member)
//                .fetch();
////        ??? ??? ??????
//        Member fetchOne = jpaQueryFactory
//                .selectFrom(member)
//                .fetchOne();
//
//        Member fetchFirst = jpaQueryFactory
//                .selectFrom(member)
//                .fetchFirst();

        QueryResults<Member> results = jpaQueryFactory
                .selectFrom(member)
                .fetchResults();

        results.getTotal();
        List<Member> content = results.getResults();

//        ?????????
        long count = jpaQueryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 1. ?????? ?????? ????????????
     * 2. ?????? ?????? ????????????
     * 2????????? ????????? ???????????? ???????????? ??????(nulls last)
     */
    @Test
    public void sort() {
//        ?????? ??????
        entityManager.persist(new Member(null, 100));
        entityManager.persist(new Member("member5", 100));
        entityManager.persist(new Member("member6", 100));

        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

    }

    //    ?????????1 ?????? ???????????? ???????????? ?????? ??????
    @Test
    public void paging1() {

        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
        assertThat(result.size()).isEqualTo(2);
    }

    //    ?????????2 ?????? ???????????? ???????????? ?????? ??????
    @Test
    public void paging2() {

        QueryResults<Member> results = jpaQueryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(results.getTotal()).isEqualTo(4);
        assertThat(results.getLimit()).isEqualTo(2);
        assertThat(results.getOffset()).isEqualTo(1);
        assertThat(results.getResults().size()).isEqualTo(2);
    }

    //    ??????
    @Test
    public void aggregation() {
        /**
         * ????????? ???????????? ????????? ????????? ????????? ??????????????????
         */
        List<Tuple> result = jpaQueryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);

        assertThat(tuple.get(member.count())).isEqualTo(4);
    }

    /**
     * ?????? ????????? ??? ?????? ?????? ???????????????
     */
    @Test
    public void group() {
        List<Tuple> result = jpaQueryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
    }

    /**
     * teamA??? ????????? ?????? ?????? ??????
     */
    @Test
    public void join() {
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * ?????????  ?????? ???????????????, ??? ????????? teamA??? ?????? ??????, ????????? ?????? ??????
     * (??????????????? ?????? ??????)
     */
    @Test
    public void join_on_filtering() {
        List<Tuple> result = jpaQueryFactory
                .select(member, team)
                .from(member)

//                ????????? ????????? ?????????
//               .leftJoin(member.team, team)
//                .on(team.name.eq("teamA"))
                .join(member.team, team)
                .where(team.name.eq("teamA"))

                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * ??????????????? ?????? ????????? ?????? ??????
     * ????????? ????????? ??? ????????? ?????? ?????? ?????? ??????
     * ????????? ??? ??????
     */
    @Test
    public void join_on_noRelation() {
        entityManager.persist(new Member("teamA"));
        entityManager.persist(new Member("teamB"));
        entityManager.persist(new Member("teamC"));

        List<Tuple> result = jpaQueryFactory
                .select(member, team)
                .from(member)
//              ????????? ??????????????? ?????????
//                .leftJoin(member.team).on(member.username.eq(team.name))

                .leftJoin(team).on(member.username.eq(team.name))
//                .where(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
//        ????????? ???????????????  ?????? ?????? ?????? ??????
        entityManager.flush();
        entityManager.clear();

        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

//        fetch lazy??? ????????????????????? false??? ????????? ?????????
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getUsername());
        assertThat(loaded).as("?????? ?????????").isFalse();

    }

    @Test
    public void fetchJoin() {
//        ????????? ???????????????  ?????? ?????? ?????? ??????
        entityManager.flush();
        entityManager.clear();

        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

//        fetch lazy??? ????????????????????? false??? ????????? ?????????
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getUsername());
        assertThat(loaded).as("?????? ?????????").isTrue();
    }

    /**
     * ????????? ?????? ?????? ????????????
     * ????????? ?????? ?????? ??????
     */
    @Test
    public void subQuery() {
//        ?????? ?????? ????????? ?????? member ????????? ????????????????????? ?????? ??? ??????
        QMember memberSub = new QMember("memberSub");

//        ???????????? ??????
//        List<Member> result = jpaQueryFactory
//                .selectFrom(member)
//                .where(member.age.eq(
//                                JPAExpressions
//                                        .select(memberSub.age.max())
//                                        .from(memberSub)
//                        )
//                )
//                .fetch();

        List<Member> result = jpaQueryFactory
                .selectFrom(member)
//                goe -> ????????? ??????
                .where(member.age.goe(
                                JPAExpressions
                                        .select(memberSub.age.avg())
                                        .from(memberSub)
                        )
                )
                .fetch();
        assertThat(result)
                .extracting("age")
                .containsExactly(25, 24, 24);

    }

    @Test
    public void selectSubQuery() {
        //        ?????? ?????? ????????? ?????? member ????????? ????????????????????? ?????? ??? ??????
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = jpaQueryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    //    ????????? ?????????
    @Test
    public void basicCase() {
        List<String> result = jpaQueryFactory
                .select(member.age
                        .when(20).then("??????")
                        .when(24).then("?????????")
                        .otherwise("??????"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    //    ????????? ?????????
    @Test
    public void complexCase() {
        List<String> result = jpaQueryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20")
                        .when(member.age.between(21, 30)).then("21~30")
                        .otherwise("??????")
                )
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void constant() {
        List<Tuple> result = jpaQueryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
//            ????????? ??????
//            tuple = [member1, A]
//            tuple = [member2, A]
//            tuple = [member3, A]
//            tuple = [member4, A]
        }
    }

    //    {username}_{age} ??? ?????????????????????
    @Test
    public void concat() {
        List<String> result = jpaQueryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    //    ???????????? ????????? ???????????????
    @Test
    public void singleProjection() {
        List<String> result = jpaQueryFactory
                .select(member.username)
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    //    ???????????? ????????? ?????????????????? ???????????????(??????)
    @Test
    public void tupleProjection() {
        List<Tuple> result = jpaQueryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    /**
     * Dto ???????????? 3??????
     * 1. property(bean)??? ????????? ??????
     * 2. ????????? ????????? ??????
     * 3. ???????????? ????????? ??????
     */

//    bean??? ????????? dto ????????????
    @Test
    public void findDtoBySetter() {
        List<MemberDto> result = jpaQueryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age)
                )
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    //    field??? ????????? dto ????????????
    //    private String username / int age ??? ??????
    @Test
    public void findDtoByField() {
        List<MemberDto> result = jpaQueryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age)
                )
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    //    ???????????? ????????? ??????
    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = jpaQueryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    //    MemberDto?????? UserDto  ?????????
    @Test
    public void findUserDtoByConstructor() {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = jpaQueryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * memberDto ???????????? ??????????????? ????????? ??????????????? ??????dsl ?????????
     * ???????????? ???????????? ??????dsl ?????? dto ??????
     */

    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = jpaQueryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = null;
        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    //    dynamicQuery_BooleanBuilde
    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (usernameCond != null) {
            booleanBuilder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            booleanBuilder.and(member.age.eq(ageCond));
        }
        return jpaQueryFactory
                .selectFrom(member)
                .where(booleanBuilder)
                .fetch();
    }


    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = 21;
        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    //    dynamicQuery_WhereParam
    private List<Member> searchMember2(String usernameParam, Integer ageParam) {
        return jpaQueryFactory
                .selectFrom(member)
                .where(usernameEq(usernameParam), ageEq(ageParam))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    /**
     * ?????? ????????????  ?????? ????????? ??????
     * ????????????
     */

//    ????????????
    @Test
    public void bulkUpdate() {
        long count = jpaQueryFactory
                .update(member)
                .set(member.username, "?????????")
                .where(member.age.lt(28))
                .execute();

//        ?????? ?????? ??? ????????? ??????????????? db???  ?????? ????????? ?????????
//        ????????? ????????? ??? ??????
        entityManager.flush();
        entityManager.clear();
    }

    //    ???????????? +1
    @Test
    public void bulkAdd() {
        long count = jpaQueryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    //    ?????? ??????
    @Test
    public void bulkDelete() {
        long count = jpaQueryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

//              member1 -> m1
//            s = m1
//            s = m2
//            s = m3
//            s = m4

    @Test
    public void sqlFunction() {
        List<String> result = jpaQueryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0},{1}, {2})",
                        member.username, "member", "m"
                ))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }


    @Test
    public void sqlFunction2() {
        List<String> result = jpaQueryFactory
                .select(member.username)
                .from(member)
                .where((member.username.eq(Expressions.stringTemplate(
                        "function('lower',{0})", member.username
                ))))
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}
