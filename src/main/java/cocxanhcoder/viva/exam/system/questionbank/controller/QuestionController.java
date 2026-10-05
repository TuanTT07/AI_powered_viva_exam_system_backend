package cocxanhcoder.viva.exam.system.questionbank.controller;

import cocxanhcoder.viva.exam.system.questionbank.dto.QuestionRequest;
import cocxanhcoder.viva.exam.system.questionbank.dto.QuestionResponse;
import cocxanhcoder.viva.exam.system.questionbank.entity.BloomLevel;
import cocxanhcoder.viva.exam.system.questionbank.entity.QuestionStatus;
import cocxanhcoder.viva.exam.system.questionbank.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse create(@Valid @RequestBody QuestionRequest request) {
        return questionService.create(request);
    }

    @GetMapping
    public Page<QuestionResponse> search(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(required = false) BloomLevel bloomLevel,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return questionService.search(courseId, status, bloomLevel, keyword, pageable);
    }

    @GetMapping("/{id}")
    public QuestionResponse findById(@PathVariable UUID id) {
        return questionService.findById(id);
    }

    @PutMapping("/{id}")
    public QuestionResponse update(@PathVariable UUID id, @Valid @RequestBody QuestionRequest request) {
        return questionService.update(id, request);
    }

    @PostMapping("/{id}/approve")
    public QuestionResponse approve(@PathVariable UUID id) {
        return questionService.approve(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        questionService.delete(id);
    }
}
