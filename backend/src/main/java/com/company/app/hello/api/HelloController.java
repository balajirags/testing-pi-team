package com.company.app.hello.api;

import com.company.app.hello.api.dto.HelloResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/hello")
public class HelloController {

    @GetMapping
    public ResponseEntity<HelloResponse> getHello() {
        HelloResponse response = new HelloResponse("Hello World!", "UP", Instant.now());
        return ResponseEntity.ok(response);
    }
}
