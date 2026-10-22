package com.example.library.book;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The catalogue API, following Boot 4 conventions: typed record payloads and
 * errors described as RFC 9457 problem details.
 */
@RestController
@RequestMapping("/api/titles")
public class TitleController {

    private final TitleRepository titleRepository;

    public TitleController(TitleRepository titleRepository) {
        this.titleRepository = titleRepository;
    }

    @GetMapping
    public List<Title> titles() {
        return titleRepository.findAll();
    }

    @GetMapping("/{id}")
    public Title title(@PathVariable String id) {
        return titleRepository.findById(id).orElseThrow(() -> new TitleNotFoundException(id));
    }

    @GetMapping("/{id}/copies")
    public List<Copy> copies(@PathVariable String id) {
        return titleRepository.findCopiesByTitleId(id);
    }
}
