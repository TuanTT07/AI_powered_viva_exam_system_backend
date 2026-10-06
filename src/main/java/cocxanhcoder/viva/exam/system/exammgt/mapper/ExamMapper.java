package cocxanhcoder.viva.exam.system.exammgt.mapper;

import cocxanhcoder.viva.exam.system.exammgt.dto.request.ExamConfigDto;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.CandidateScheduleResponse;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.ExamDetailResponse;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.ExamResponse;
import cocxanhcoder.viva.exam.system.exammgt.entity.Exam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ExamMapper {

    private ExamMapper() {}

    public static ExamResponse toResponse(Exam exam) {
        if (exam == null) return null;

        return new ExamResponse(
            exam.getId(),
            exam.getCourse().getId(),
            exam.getCourse().getCourseCode(),
            exam.getCourse().getCourseName(),
            exam.getTitle(),
            exam.getStartTime(),
            exam.getEndTime(),
            exam.getStatus(),
            parseConfig(exam.getExamConfig()),
            exam.getCreatedAt()
        );
    }

    public static ExamDetailResponse toDetailResponse(Exam exam, List<CandidateScheduleResponse> candidates) {
        if (exam == null) return null;

        return new ExamDetailResponse(
            exam.getId(),
            exam.getCourse().getId(),
            exam.getCourse().getCourseCode(),
            exam.getCourse().getCourseName(),
            exam.getTitle(),
            exam.getStartTime(),
            exam.getEndTime(),
            exam.getStatus(),
            parseConfig(exam.getExamConfig()),
            exam.getCreatedAt(),
            candidates != null ? candidates.size() : 0,
            candidates != null ? candidates : List.of()
        );
    }

    public static ExamConfigDto parseConfig(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return new ExamConfigDto(3, 2, 60, Map.of(), true);
        }

        Integer maxMain = map.get("maxMainQuestions") instanceof Number n ? n.intValue() : 3;
        Integer maxFollowUp = map.get("maxFollowUpQuestions") instanceof Number n ? n.intValue() : 2;
        Integer timeLimit = map.get("timeLimitPerTurnSeconds") instanceof Number n ? n.intValue() : 60;
        Boolean antiOverlap = map.get("antiOverlapEnabled") instanceof Boolean b ? b : true;

        @SuppressWarnings("unchecked")
        Map<String, Double> bloomRatios = map.get("bloomRatios") instanceof Map m ? (Map<String, Double>) m : Map.of();

        return new ExamConfigDto(maxMain, maxFollowUp, timeLimit, bloomRatios, antiOverlap);
    }

    public static Map<String, Object> toConfigMap(ExamConfigDto dto) {
        if (dto == null) return new HashMap<>();

        Map<String, Object> map = new HashMap<>();
        map.put("maxMainQuestions", dto.maxMainQuestions() != null ? dto.maxMainQuestions() : 3);
        map.put("maxFollowUpQuestions", dto.maxFollowUpQuestions() != null ? dto.maxFollowUpQuestions() : 2);
        map.put("timeLimitPerTurnSeconds", dto.timeLimitPerTurnSeconds() != null ? dto.timeLimitPerTurnSeconds() : 60);
        map.put("antiOverlapEnabled", dto.antiOverlapEnabled() != null ? dto.antiOverlapEnabled() : true);
        map.put("bloomRatios", dto.bloomRatios() != null ? dto.bloomRatios() : Map.of());
        return map;
    }
}
