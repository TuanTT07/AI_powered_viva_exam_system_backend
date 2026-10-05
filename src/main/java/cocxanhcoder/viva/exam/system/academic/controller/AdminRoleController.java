package cocxanhcoder.viva.exam.system.academic.controller;

import cocxanhcoder.viva.exam.system.academic.dto.RoleResponse;
import cocxanhcoder.viva.exam.system.academic.service.UserService;
import cocxanhcoder.viva.exam.system.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** API danh sách role (FE dùng để đổ dropdown khi tạo / sửa tài khoản). */
@RestController
@RequestMapping("/api/admin/roles")
@Tag(name = "Admin - Roles", description = "Danh sách phân quyền")
public class AdminRoleController {

    private final UserService userService;

    public AdminRoleController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Danh sách role: ADMIN, LECTURER, STUDENT")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoles() {
        return ApiResponse.ok("Lấy danh sách role thành công", userService.getRoles());
    }
}
