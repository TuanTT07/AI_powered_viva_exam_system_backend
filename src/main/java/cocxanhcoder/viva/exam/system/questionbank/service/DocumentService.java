package cocxanhcoder.viva.exam.system.questionbank.service;

import cocxanhcoder.viva.exam.system.academic.entity.Document;
import cocxanhcoder.viva.exam.system.academic.entity.DocumentIndexStatus;
import cocxanhcoder.viva.exam.system.academic.repository.CourseRepository;
import cocxanhcoder.viva.exam.system.academic.repository.DocumentRepository;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.common.exception.ResourceNotFoundException;
import cocxanhcoder.viva.exam.system.questionbank.dto.DocumentResponse;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DocumentService {
    private final DocumentRepository documentRepository; private final CourseRepository courseRepository;
    private final CourseAccessService access; private final AiClient aiClient; private final Path uploadRoot; private final long maxBytes;
    public DocumentService(DocumentRepository documentRepository, CourseRepository courseRepository, CourseAccessService access, AiClient aiClient,
                           @Value("${app.ai.upload-dir}") String uploadDir, @Value("${app.ai.max-upload-bytes}") long maxBytes) {
        this.documentRepository=documentRepository; this.courseRepository=courseRepository; this.access=access; this.aiClient=aiClient;
        this.uploadRoot=Path.of(uploadDir).toAbsolutePath().normalize(); this.maxBytes=maxBytes;
    }
    public List<DocumentResponse> list(UUID actor, UUID courseId) {
        access.requireManager(actor, courseId);
        return documentRepository.findByCourseIdOrderByCreatedAtDesc(courseId).stream().map(this::toResponse).toList();
    }
    @Transactional
    public DocumentResponse upload(UUID actor, UUID courseId, String title, MultipartFile file) {
        var user=access.requireManager(actor, courseId); var course=courseRepository.findById(courseId).orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        validate(file); Path stored=null;
        try {
            Files.createDirectories(uploadRoot); String extension=extension(file.getOriginalFilename()); String storedName=UUID.randomUUID()+extension;
            stored=uploadRoot.resolve(storedName); Files.copy(file.getInputStream(), stored);
            Document doc=new Document(); doc.setCourse(course); doc.setTitle(title == null || title.isBlank() ? file.getOriginalFilename() : title.trim());
            doc.setOriginalFilename(file.getOriginalFilename()); doc.setStoredFilename(storedName); doc.setFileUrl(stored.toString()); doc.setContentType(file.getContentType()); doc.setFileSize(file.getSize()); doc.setUploadedBy(user);
            doc.setContentExtracted(extractText(stored, extension)); doc.setIndexingStatus(DocumentIndexStatus.INDEXING); doc=documentRepository.save(doc);
            try {
                String vectorStoreId=course.getOpenAiVectorStoreId(); if (vectorStoreId == null || vectorStoreId.isBlank()) { vectorStoreId=aiClient.createVectorStore(course.getCourseCode()); course.setOpenAiVectorStoreId(vectorStoreId); }
                String openAiFileId=aiClient.uploadFile(stored, file.getOriginalFilename()); aiClient.addFileToVectorStore(vectorStoreId, openAiFileId);
                doc.setOpenAiFileId(openAiFileId); doc.setIndexingStatus(DocumentIndexStatus.READY);
            } catch (RuntimeException ex) { doc.setIndexingStatus(DocumentIndexStatus.FAILED); doc.setIndexingError("Không thể lập chỉ mục OpenAI: " + ex.getMessage()); }
            return toResponse(doc);
        } catch (Exception ex) { if (stored != null) try { Files.deleteIfExists(stored); } catch (Exception ignored) {} throw new BusinessException(HttpStatus.BAD_REQUEST, "Không thể lưu hoặc đọc tài liệu: " + ex.getMessage()); }
    }
    @Transactional
    public void delete(UUID actor, UUID courseId, UUID documentId) {
        access.requireManager(actor, courseId); Document doc=documentRepository.findById(documentId).orElseThrow(() -> new ResourceNotFoundException("Document", documentId));
        if (!doc.getCourse().getId().equals(courseId)) throw new ResourceNotFoundException("Document", documentId);
        if (documentRepository.isUsedAsQuestionSource(documentId)) throw new BusinessException(HttpStatus.CONFLICT, "Không thể xoá tài liệu đang là nguồn của câu hỏi AI");
        if (doc.getOpenAiFileId()!=null) aiClient.deleteFile(doc.getOpenAiFileId());
        try { Files.deleteIfExists(Path.of(doc.getFileUrl())); } catch (Exception ex) { throw new BusinessException(HttpStatus.CONFLICT, "Không thể xoá tệp nguồn: " + ex.getMessage()); }
        documentRepository.delete(doc);
    }
    private void validate(MultipartFile file) {
        if (file==null || file.isEmpty()) throw new BusinessException("Vui lòng chọn tài liệu"); if (file.getSize()>maxBytes) throw new BusinessException("Tài liệu vượt quá dung lượng cho phép");
        String ext=extension(file.getOriginalFilename()); if (!(ext.equals(".pdf")||ext.equals(".docx")||ext.equals(".pptx"))) throw new BusinessException("Chỉ hỗ trợ PDF, DOCX hoặc PPTX");
    }
    private String extractText(Path path, String extension) throws Exception {
        if (extension.equals(".pdf")) try (var pdf=Loader.loadPDF(path.toFile())) { return new PDFTextStripper().getText(pdf); }
        if (extension.equals(".docx")) try (InputStream in=Files.newInputStream(path); XWPFDocument d=new XWPFDocument(in); XWPFWordExtractor e=new XWPFWordExtractor(d)) { return e.getText(); }
        try (InputStream in=Files.newInputStream(path); XMLSlideShow s=new XMLSlideShow(in)) { return s.getSlides().stream().flatMap(slide -> slide.getShapes().stream()).filter(shape -> shape instanceof org.apache.poi.xslf.usermodel.XSLFTextShape).map(shape -> ((org.apache.poi.xslf.usermodel.XSLFTextShape)shape).getText()).reduce("", (a,b)->a+"\n"+b); }
    }
    private String extension(String name) { if(name==null||!name.contains(".")) return ""; return name.substring(name.lastIndexOf('.')).toLowerCase(); }
    private DocumentResponse toResponse(Document d) { return new DocumentResponse(d.getId(),d.getTitle(),d.getOriginalFilename(),d.getContentType(),d.getFileSize(),d.getIndexingStatus(),d.getIndexingError(),d.getCreatedAt()); }
}
