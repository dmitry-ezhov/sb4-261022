package com.example.library.book;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The catalogue API.
 *
 * <p>Written the way a Spring Boot 2 application would have written it: untyped
 * map payloads, and errors that say nothing about what went wrong.
 */
@RestController
@RequestMapping("/api/titles")
public class TitleController {

    private final TitleRepository titleRepository;

    public TitleController(TitleRepository titleRepository) {
        this.titleRepository = titleRepository;
    }

    @GetMapping
    public Map<String, Object> titles() {
        return Map.<String, Object>of("items", titleRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> title(@PathVariable String id) {
        return titleRepository
                .findById(id)
                .map(found -> ResponseEntity.ok(Map.<String, Object>of("id", found.id(), "name", found.name())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/copies")
    public Map<String, Object> copies(@PathVariable String id) {
        return Map.<String, Object>of("items", titleRepository.findCopiesByTitleId(id));
    }
}
