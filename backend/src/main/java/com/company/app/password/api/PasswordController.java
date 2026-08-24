package com.company.app.password.api;

import com.company.app.password.api.dto.PasswordGenerateResponse;
import com.company.app.password.service.PasswordService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/passwords")
public class PasswordController {

    private final PasswordService passwordService;

    public PasswordController(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    @GetMapping(value = "/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public PasswordGenerateResponse generatePassword(
            @RequestParam(defaultValue = "12") int length
    ) {
        return passwordService.generatePassword(length);
    }
}
