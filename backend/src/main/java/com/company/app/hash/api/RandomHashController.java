package com.company.app.hash.api;

import com.company.app.hash.api.dto.RandomHashResponse;
import com.company.app.hash.service.RandomHashService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/random-hash")
public class RandomHashController {

    private final RandomHashService randomHashService;

    public RandomHashController(RandomHashService randomHashService) {
        this.randomHashService = randomHashService;
    }

    @GetMapping
    public ResponseEntity<RandomHashResponse> getRandomHash() {
        RandomHashResponse response = randomHashService.generateRandomHash();
        return ResponseEntity.ok(response);
    }
}
