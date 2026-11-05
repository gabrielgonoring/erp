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

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserPrincipal> getCurrentUser(@AuthenticationPrincipal UserPrincipal currentUser) {
        log.info("getCurrentUser() - START - userId={}", currentUser != null ? currentUser.getId() : null);
        log.info("getCurrentUser() = END - userId={}", currentUser != null ? currentUser.getId() : null);
        return ResponseEntity.ok(currentUser);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> findAllByCompany(@AuthenticationPrincipal UserPrincipal currentUser) {
        Long companyId = currentUser != null ? currentUser.getCompanyId() : null;
        log.info("findAllByCompany() - START - companyId={}", companyId);

        List<UserResponse> users = userService.findAllByCompanyId(companyId);

        log.info("findAllByCompany() = END - companyId={}, total={}", companyId, users.size());
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @RequestBody @Valid UserCreateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Long companyId = currentUser != null ? currentUser.getCompanyId() : null;
        log.info("createUser() - START - companyId={}, email={}", companyId, request != null ? request.email() : null);

        UserResponse response = userService.createUserForCompany(request, companyId);

        log.info("createUser() = END - userId={}, email={}", response.id(), response.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @RequestMapping(
            value = {"/me/password", "/change-password"},
            method = {RequestMethod.PUT, RequestMethod.PATCH}
    )
    public ResponseEntity<Void> changePassword(
            @RequestBody @Valid ChangePasswordRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Long userId = currentUser != null ? currentUser.getId() : null;
        log.info("changePassword() - START - userId={}", userId);

        userService.changePassword(userId, request);

        log.info("changePassword() = END - userId={}", userId);
        return ResponseEntity.noContent().build();
    }
}
