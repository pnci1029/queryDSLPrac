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
//        member 1을 찾아라
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
                 * static import  해서 실제로 Qmember.member 생략
                 * 일반적인 쿼리의 select from where
                 */
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))  // 파라미터 바인딩
                .fetchOne();

        assertThat(targetMember.getUsername().equals("member1"));
    }

    //    And절 사용
    @Test
    public void search() {
        Member resultMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(21)))
                .fetchOne();

        assertThat(resultMember.getUsername().equals("member1"));
    }

    //   위 아래 같음 .and 사용 or 파라미터(파라미터중에 널이  들어가도 괜찮음  -> 더 선호)
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
////        리스트 조회
//        List<Member> fetch = jpaQueryFactory
//                .selectFrom(member)
//                .fetch();
////        단 건 조회
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

//        카운트
        long count = jpaQueryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 1. 회원 나이 내림차순
     * 2. 회원 이름 오름차순
     * 2번에서 이름이 없을경우 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
//        멤버 추가
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

    //    페이징1 현재 페이지에 보여지는 개수 출력
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

    //    페이징2 전체 페이지에 보여지는 개수 출력
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

    //    집합
    @Test
    public void aggregation() {
        /**
         * 튜플은 여러개의 타입이 있을때 편하게 꺼내올수있음
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
     * 팀의 이름과 각 팀의 평균 연령구하기
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
     * teamA에 소속된 모든 회원 조회
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
     * 회원과  팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * (연관관계가 있는 경우)
     */
    @Test
    public void join_on_filtering() {
        List<Tuple> result = jpaQueryFactory
                .select(member, team)
                .from(member)

//                조인절 결과값 같음음
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
     * 연관관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     * 사용이 더 많음
     */
    @Test
    public void join_on_noRelation() {
        entityManager.persist(new Member("teamA"));
        entityManager.persist(new Member("teamB"));
        entityManager.persist(new Member("teamC"));

        List<Tuple> result = jpaQueryFactory
                .select(member, team)
                .from(member)
//              원래는 이런식으로 조인함
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
//        영속성 컨텍스트에  있는 값들 모두 정리
        entityManager.flush();
        entityManager.clear();

        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

//        fetch lazy로 설정되어있어서 false로 결과가 나옴옴
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getUsername());
        assertThat(loaded).as("패치 미적용").isFalse();

    }

    @Test
    public void fetchJoin() {
//        영속성 컨텍스트에  있는 값들 모두 정리
        entityManager.flush();
        entityManager.clear();

        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

//        fetch lazy로 설정되어있어서 false로 결과가 나옴옴
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getUsername());
        assertThat(loaded).as("패치 미적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원조회
     * 나이가 평균 이상 회원
     */
    @Test
    public void subQuery() {
//        서브 쿼리 만들때 같은 member 변수를 사용할수없어서 하나 더 생성
        QMember memberSub = new QMember("memberSub");

//        최대나이 출력
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
//                goe -> 같거나 크다
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
        //        서브 쿼리 만들때 같은 member 변수를 사용할수없어서 하나 더 생성
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

    //    단순한 케이스
    @Test
    public void basicCase() {
        List<String> result = jpaQueryFactory
                .select(member.age
                        .when(20).then("스물")
                        .when(24).then("스물넷")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    //    복잡한 케이스
    @Test
    public void complexCase() {
        List<String> result = jpaQueryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20")
                        .when(member.age.between(21, 30)).then("21~30")
                        .otherwise("기타")
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
//            이렇게 출력
//            tuple = [member1, A]
//            tuple = [member2, A]
//            tuple = [member3, A]
//            tuple = [member4, A]
        }
    }

    //    {username}_{age} 로 출력하고싶을때
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

    //    프로젝션 대상이 하나인경우
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

    //    프로젝션 대상이 여러타입으로 반환될경우(튜플)
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
     * Dto 접근방법 3가지
     * 1. property(bean)를 활용한 방법
     * 2. 필드를 활용한 방법
     * 3. 생성자를 활용한 벙법
     */

//    bean을 활용한 dto 접근방법
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

    //    field를 활용한 dto 접근방법
    //    private String username / int age 에 직접
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

    //    생성자를 활용한 방법
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

    //    MemberDto말고 UserDto  생성자
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
     * memberDto 생성자에 어노테이션 붙이고 그레이들에 쿼리dsl 컴파일
     * 프로젝트 그레이들 쿼리dsl 내에 dto 생김
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
     * 쿼리 한번으로  대량 데이터 수정
     * 벌크연산
     */

//    업데이트
    @Test
    public void bulkUpdate() {
        long count = jpaQueryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

//        벌크 연산 후 영속성 컨텍스트와 db에  있는 값들이 달라짐
//        따라서 초기화 후 사용
        entityManager.flush();
        entityManager.clear();
    }

    //    전체내용 +1
    @Test
    public void bulkAdd() {
        long count = jpaQueryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    //    내용 삭제
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
