package com.example.study;

import com.example.study.entity.HelloEntity;
import com.example.study.entity.QHelloEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

@SpringBootTest
@Transactional
@Commit
class StudyApplicationTests {

	@Autowired
	EntityManager entityManager;

	@Test
	void contextLoads() {
		HelloEntity hello = new HelloEntity();
		entityManager.persist(hello);

		JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
		QHelloEntity qHelloEntity = new QHelloEntity("gd");

		HelloEntity helloEntity = queryFactory
				.selectFrom(qHelloEntity)
				.fetchOne();

		Assertions.assertThat(helloEntity).isEqualTo(hello);
		Assertions.assertThat(helloEntity.getId()).isEqualTo(hello.getId());

	}

}
