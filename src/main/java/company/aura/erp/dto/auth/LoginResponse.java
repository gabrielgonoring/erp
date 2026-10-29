package company.aura.erp.dto.auth;

import company.aura.erp.dto.UserResponse;

public record LoginResponse(
        String token,
        String tokenType,
        Long expiresIn,
        UserResponse user
) {
    public static LoginResponse of(String token, Long expiresIn, UserResponse user) {
        return new LoginResponse(token, "Bearer", expiresIn, user);
    }
}
