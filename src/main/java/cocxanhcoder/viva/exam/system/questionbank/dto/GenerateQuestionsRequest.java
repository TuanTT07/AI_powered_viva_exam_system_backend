package cocxanhcoder.viva.exam.system.questionbank.dto;
import cocxanhcoder.viva.exam.system.questionbank.entity.BloomLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
public record GenerateQuestionsRequest(@NotNull @Min(1) @Max(20) Integer count, String topic,
                                       List<BloomLevel> bloomLevels, UUID rubricId, List<UUID> documentIds) { }
