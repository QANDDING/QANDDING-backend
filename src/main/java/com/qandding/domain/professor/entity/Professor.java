package com.qandding.domain.professor.entity;

import com.qandding.global.entity.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "professor")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Professor extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "professor_id")
	private Long id;

	@Column(name = "professor_name", nullable = false, length = 100)
	private String name;

	public Professor(String name) {
		this.name = name;
	}
}
