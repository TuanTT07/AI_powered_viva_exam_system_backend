package cocxanhcoder.viva.exam.system.questionbank.controller;

import cocxanhcoder.viva.exam.system.questionbank.dto.RubricRequest;
import cocxanhcoder.viva.exam.system.questionbank.dto.RubricResponse;
import cocxanhcoder.viva.exam.system.questionbank.service.RubricService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rubrics")
public class RubricController {

    private final RubricService rubricService;

    public RubricController(RubricService rubricService) {
        this.rubricService = rubricService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RubricResponse create(@Valid @RequestBody RubricRequest request) {
        return rubricService.create(request);
    }

    @GetMapping
    public List<RubricResponse> findAll() {
        return rubricService.findAll();
    }

    @GetMapping("/{id}")
    public RubricResponse findById(@PathVariable UUID id) {
        return rubricService.findById(id);
    }

    @PutMapping("/{id}")
    public RubricResponse update(@PathVariable UUID id, @Valid @RequestBody RubricRequest request) {
        return rubricService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        rubricService.delete(id);
    }
}
