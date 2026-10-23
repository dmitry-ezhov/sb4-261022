package com.example.library.book;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The catalogue API, following Boot 4 conventions: typed record payloads and
 * errors described as RFC 9457 problem details. The answers are the
 * database's: whatever a migration put there is what the endpoints say.
 */
@RestController
@RequestMapping("/api/titles")
public class TitleController {

    private final TitleCatalogue titleCatalogue;
    private final CopyRepository copyRepository;

    public TitleController(TitleCatalogue titleCatalogue, CopyRepository copyRepository) {
        this.titleCatalogue = titleCatalogue;
        this.copyRepository = copyRepository;
    }

    @GetMapping
    public List<Title> titles() {
        return titleCatalogue.findAll();
    }

    @GetMapping("/{id}")
    public Title title(@PathVariable Long id) {
        return titleCatalogue.findById(id).orElseThrow(() -> new TitleNotFoundException(id));
    }

    @GetMapping("/{id}/copies")
    public List<Copy> copies(@PathVariable Long id) {
        return copyRepository.findByTitleId(id);
    }
}
