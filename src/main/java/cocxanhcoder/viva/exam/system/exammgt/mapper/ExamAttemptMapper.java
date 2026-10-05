package cocxanhcoder.viva.exam.system.exammgt.mapper;

import cocxanhcoder.viva.exam.system.exammgt.dto.response.CandidateScheduleResponse;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.QuestionAttemptResponse;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.StudentExamSlotResponse;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttempt;
import cocxanhcoder.viva.exam.system.exammgt.entity.QuestionAttempt;

import java.util.List;

public final class ExamAttemptMapper {

    private ExamAttemptMapper() {}

    public static CandidateScheduleResponse toScheduleResponse(ExamAttempt attempt) {
        if (attempt == null) return null;

        return new CandidateScheduleResponse(
            attempt.getId(),
            attempt.getExam().getId(),
            attempt.getStudent().getId(),
            attempt.getStudent().getUserCode(),
            attempt.getStudent().getFullName(),
            attempt.getStudent().getEmail(),
            attempt.getSlotNumber(),
            attempt.getScheduledStartTime(),
            attempt.getScheduledEndTime(),
            attempt.getActualStartTime(),
            attempt.getActualEndTime(),
            attempt.getStatus(),
            attempt.getAccessCode()
        );
    }

    public static QuestionAttemptResponse toQuestionAttemptResponse(QuestionAttempt qa) {
        if (qa == null) return null;

        String bloom = qa.getQuestion().getBloomLevel() != null ? qa.getQuestion().getBloomLevel().name() : null;

        return new QuestionAttemptResponse(
            qa.getId(),
            qa.getQuestion().getId(),
            qa.getQuestion().getContent(),
            bloom,
            qa.getQuestionOrder(),
            qa.getStatus(),
            qa.getStartTime(),
            qa.getEndTime()
        );
    }

    public static StudentExamSlotResponse toStudentSlotResponse(ExamAttempt attempt, List<QuestionAttemptResponse> questions) {
        if (attempt == null) return null;

        return new StudentExamSlotResponse(
            attempt.getId(),
            attempt.getExam().getId(),
            attempt.getExam().getTitle(),
            attempt.getExam().getCourse().getCourseCode(),
            attempt.getExam().getCourse().getCourseName(),
            attempt.getSlotNumber(),
            attempt.getScheduledStartTime(),
            attempt.getScheduledEndTime(),
            attempt.getActualStartTime(),
            attempt.getActualEndTime(),
            attempt.getStatus(),
            attempt.getAccessCode(),
            ExamMapper.parseConfig(attempt.getExam().getExamConfig()),
            questions != null ? questions : List.of()
        );
    }
}
