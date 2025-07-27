package com.example.esportscalendar.service;

import com.example.esportscalendar.domain.MatchSchedule;
import com.example.esportscalendar.repository.MatchScheduleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatchScheduleService {

    private final MatchScheduleRepository repository;

    public MatchScheduleService(MatchScheduleRepository repository) {
        this.repository = repository;
    }

    public MatchSchedule save(MatchSchedule schedule) {
        return repository.save(schedule);
    }

    public List<MatchSchedule> findByTeam(String teamName) {
        return repository.findByTeamAOrTeamB(teamName, teamName);
    }

    public List<MatchSchedule> findAll() {
        return repository.findAll();
    }
}
