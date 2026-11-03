package cocxanhcoder.viva.exam.system.grading.dto;

import cocxanhcoder.viva.exam.system.interview.entity.Speaker;
import cocxanhcoder.viva.exam.system.interview.entity.TurnType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TranscriptTurnResponse(UUID id, int turnOrder, Speaker speaker, String textContent,
                                     String audioUrl, TurnType turnType, OffsetDateTime createdAt) {}
