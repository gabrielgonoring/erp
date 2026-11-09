package company.aura.erp.controller;

import company.aura.erp.dto.ChangePasswordRequest;
import company.aura.erp.dto.UserCreateRequest;
import company.aura.erp.dto.UserResponse;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal currentUser) {

        return ResponseEntity.ok(userService.findUserById(currentUser.getId()));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> findAllByCompany(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Long companyId = currentUser != null ? currentUser.getCompanyId() : null;

        var users = userService.findAllByCompanyId(companyId, status);

        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @RequestBody @Valid UserCreateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {

        UserResponse response = userService.createUserForCompany(request, currentUser.getCompanyId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/me/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestBody @Valid ChangePasswordRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {

        Long userId = currentUser != null ? currentUser.getId() : null;

        userService.changePassword(userId, request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {

        userService.deleteUser(id, currentUser);

        return ResponseEntity.noContent().build();
    }
}
