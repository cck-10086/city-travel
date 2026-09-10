package com.citytravel.citytravel.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SecurityProbeController {

    @GetMapping("/api/attraction/list")
    ResponseEntity<Void> publicQuery() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/admin/dashboard")
    ResponseEntity<Void> adminQuery() {
        return ResponseEntity.ok().build();
    }
}
