package cocxanhcoder.viva.exam.system.questionbank.dto;
import java.util.List;
public record ImportResult(int importedCount, List<ImportError> errors) { }
