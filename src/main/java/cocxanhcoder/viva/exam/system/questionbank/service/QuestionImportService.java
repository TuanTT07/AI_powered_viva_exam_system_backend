package cocxanhcoder.viva.exam.system.questionbank.service;

import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.questionbank.dto.ImportError;
import cocxanhcoder.viva.exam.system.questionbank.dto.ImportResult;
import cocxanhcoder.viva.exam.system.questionbank.entity.BloomLevel;
import cocxanhcoder.viva.exam.system.questionbank.repository.RubricRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class QuestionImportService {
    private final CourseAccessService access;
    private final QuestionService questions;
    private final RubricRepository rubrics;

    public QuestionImportService(CourseAccessService access, QuestionService questions, RubricRepository rubrics) {
        this.access = access;
        this.questions = questions;
        this.rubrics = rubrics;
    }

    @Transactional
    public ImportResult importWorkbook(UUID actor, UUID courseId, UUID defaultRubricId, MultipartFile file) {
        access.requireManager(actor, courseId);
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".xlsx"))
            throw new BusinessException("Chỉ hỗ trợ file Excel .xlsx");
        List<RowData> rows = new ArrayList<>();
        List<ImportError> errors = new ArrayList<>();
        try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getNumberOfSheets() == 0 ? null : wb.getSheetAt(0);
            if (sheet == null) throw new BusinessException("Workbook không có sheet");
            Map<String, Integer> header = headers(sheet.getRow(0));
            for (String required : List.of("content", "bloomlevel"))
                if (!header.containsKey(required)) throw new BusinessException("Thiếu cột bắt buộc: " + required);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || blank(row)) continue;
                String content = value(row, header.get("content"));
                String bloom = value(row, header.get("bloomlevel"));
                String rubric = header.containsKey("rubricid") ? value(row, header.get("rubricid")) : "";
                UUID rubricId = defaultRubricId;
                if (!rubric.isBlank()) try {
                    rubricId = UUID.fromString(rubric);
                } catch (Exception e) {
                    errors.add(new ImportError(i + 1, "rubricId", "UUID không hợp lệ"));
                }
                BloomLevel level = null;
                try {
                    level = BloomLevel.valueOf(bloom.trim().toUpperCase());
                } catch (Exception e) {
                    errors.add(new ImportError(i + 1, "bloomLevel", "Bloom level không hợp lệ"));
                }
                if (content.isBlank()) errors.add(new ImportError(i + 1, "content", "Không được để trống"));
                if (rubricId != null && !rubrics.existsByIdAndCourseId(rubricId, courseId))
                    errors.add(new ImportError(i + 1, "rubricId", "Rubric không thuộc môn học"));
                if (content.length() > 0 && level != null && (rubricId == null || rubrics.existsByIdAndCourseId(rubricId, courseId)))
                    rows.add(new RowData(content, level, rubricId));
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Không đọc được file Excel: " + e.getMessage());
        }
        if (!errors.isEmpty()) return new ImportResult(0, errors);
        for (RowData row : rows)
            questions.createDraft(actor, courseId, row.content, row.bloom, row.rubricId, false, Set.of());
        return new ImportResult(rows.size(), List.of());
    }

    private Map<String, Integer> headers(Row row) {
        if (row == null) throw new BusinessException("Thiếu dòng tiêu đề");
        Map<String, Integer> result = new HashMap<>();
        for (Cell cell : row) result.put(cell.getStringCellValue().trim().toLowerCase(), cell.getColumnIndex());
        return result;
    }

    private String value(Row row, int index) {
        Cell c = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return c == null ? "" : new DataFormatter().formatCellValue(c).trim();
    }

    private boolean blank(Row row) {
        for (Cell c : row) if (!new DataFormatter().formatCellValue(c).isBlank()) return false;
        return true;
    }

    private record RowData(String content, BloomLevel bloom, UUID rubricId) {
    }
}
