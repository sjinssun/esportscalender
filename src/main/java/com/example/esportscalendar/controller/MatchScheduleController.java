package com.example.esportscalendar.controller;

import com.example.esportscalendar.domain.MatchSchedule;
import com.example.esportscalendar.service.MatchScheduleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schedules")
public class MatchScheduleController {

    private final MatchScheduleService service;

    public MatchScheduleController(MatchScheduleService service) {
        this.service = service;
    }

    @PostMapping
    public MatchSchedule create(@RequestBody MatchSchedule schedule) {
        return service.save(schedule);
    }

    @GetMapping
    public List<MatchSchedule> getByTeam(@RequestParam String team) {
        return service.findByTeam(team);
    }
}
