package com.qandding.common.bootstrap;

import com.qandding.professor.domain.Professor;
import com.qandding.professor.repository.ProfessorRepository;
import com.qandding.question.domain.QuestionImage;
import com.qandding.question.domain.QuestionPost;
import com.qandding.question.repository.QuestionImageRepository;
import com.qandding.question.repository.QuestionPostRepository;
import com.qandding.subject.domain.Subject;
import com.qandding.subject.repository.SubjectRepository;
import com.qandding.user.domain.User;
import com.qandding.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Profile("test")
public class TestDataInitializer {
	private final UserRepository userRepository;
	private final SubjectRepository subjectRepository;
	private final ProfessorRepository professorRepository;
	private final QuestionPostRepository questionPostRepository;
	private final QuestionImageRepository questionImageRepository;

	public TestDataInitializer(UserRepository userRepository,
	                          SubjectRepository subjectRepository,
	                          ProfessorRepository professorRepository,
	                          QuestionPostRepository questionPostRepository,
	                          QuestionImageRepository questionImageRepository) {
		this.userRepository = userRepository;
		this.subjectRepository = subjectRepository;
		this.professorRepository = professorRepository;
		this.questionPostRepository = questionPostRepository;
		this.questionImageRepository = questionImageRepository;
	}

	@Bean
	CommandLineRunner loadDummyData() {
		return args -> seedIfEmpty();
	}

	@Transactional
	void seedIfEmpty() {
		if (userRepository.count() > 0 || subjectRepository.count() > 0 || professorRepository.count() > 0 || questionPostRepository.count() > 0) {
			return;
		}

		List<User> users = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			users.add(new User("user" + i, "1" + i, "CS", "user" + i + "@example.com"));
		}
		userRepository.saveAll(users);

		List<Subject> subjects = new ArrayList<>();
		for (int i = 1; i <= 6; i++) {
			subjects.add(new Subject("Subject " + i));
		}
		subjectRepository.saveAll(subjects);

		List<Professor> professors = new ArrayList<>();
		for (int i = 1; i <= 4; i++) {
			professors.add(new Professor("Professor " + i));
		}
		professorRepository.saveAll(professors);

		for (int i = 1; i <= 10; i++) {
			User u = users.get((i - 1) % users.size());
			Subject s = subjects.get((i - 1) % subjects.size());
			Professor p = professors.get((i - 1) % professors.size());
			QuestionPost post = questionPostRepository.save(new QuestionPost(u, p, s, "Sample Question " + i, "This is a sample content for question " + i));
			questionImageRepository.save(new QuestionImage(post, "https://example.com/image-" + i + "-1.jpg", 0));
			questionImageRepository.save(new QuestionImage(post, "https://example.com/image-" + i + "-2.jpg", 1));
		}
	}
}



