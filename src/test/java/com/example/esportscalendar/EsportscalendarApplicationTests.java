package com.example.esportscalendar;

import com.example.esportscalendar.domain.MatchSchedule;
import com.example.esportscalendar.repository.MatchScheduleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class EsportscalendarApplicationTests {

	@Autowired
	MatchScheduleRepository matchScheduleRepository;

	@Test
	public void 경기_등록() throws Exception {
		// Given
		MatchSchedule match = new MatchSchedule(
				"League of Legends", "T1", "Gen.G",
				LocalDateTime.of(2025, 8, 1, 18, 0),
				 "LCK","SCHEDULED"
		);

		// When
		MatchSchedule saved = matchScheduleRepository.save(match);

		// Then
		Optional<MatchSchedule> result = matchScheduleRepository.findById(saved.getId());
		assertThat(result).isPresent();
		assertThat(result.get().getTeamA()).isEqualTo("T1");
		assertThat(result.get().getTeamB()).isEqualTo("Gen.G");
		assertThat(result.get().getLeagueName()).isEqualTo("LCK");
	}
}