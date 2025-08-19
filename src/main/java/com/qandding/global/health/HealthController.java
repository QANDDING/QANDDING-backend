package com.qandding.global.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    
    @GetMapping("/")
    public ResponseEntity<String> root() {
        return ResponseEntity.ok("Qandding Backend is running!");
    }
    
    @RequestMapping("/api")
    public class ApiController {
        @GetMapping("/health")
        public ResponseEntity<Map<String, Object>> health() {
            return ResponseEntity.ok(Map.of(
                "status", "OK",
                "service", "qandding-backend"
            ));
        }
    }
}
