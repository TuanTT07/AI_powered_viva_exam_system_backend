package cocxanhcoder.viva.exam.system.questionbank.dto;

import cocxanhcoder.viva.exam.system.questionbank.entity.BloomLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record QuestionRequest(
        @NotNull UUID courseId,
        UUID rubricId,
        @NotNull UUID createdById,
        @NotBlank String content,
        @NotNull BloomLevel bloomLevel,
        boolean aiGenerated
) {
}
