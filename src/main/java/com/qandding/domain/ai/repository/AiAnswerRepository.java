package com.qandding.domain.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qandding.domain.ai.entity.AiAnswer;

public interface AiAnswerRepository extends JpaRepository<AiAnswer, Long> {
}
