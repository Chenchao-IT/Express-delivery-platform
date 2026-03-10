package com.campus.express.controller;

import com.campus.express.algorithm.CampusPathPlanningService;
import com.campus.express.algorithm.PathPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/path")
@RequiredArgsConstructor
public class PathPlanningController {

    private final CampusPathPlanningService pathPlanningService;

    @GetMapping("/plan")
    public ResponseEntity<?> planPath(
            @RequestParam String start,
            @RequestParam String end) {
        try {
            List<PathPoint> path = pathPlanningService.findShortestPath(start, end);
            var coords = path.stream()
                .map(p -> Map.of("x", p.x(), "y", p.y()))
                .toList();
            return ResponseEntity.ok(Map.of("path", coords, "length", path.size()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/destinations")
    public ResponseEntity<java.util.Set<String>> getDestinations() {
        return ResponseEntity.ok(pathPlanningService.getAvailableDestinations());
    }
}
