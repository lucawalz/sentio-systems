package org.example.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.example.backend.dto.AuthDTOs;

/**
 * Validator for password confirmation matching in ResetPasswordRequest.
 */
public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, AuthDTOs.ResetPasswordRequest> {

    @Override
    public void initialize(PasswordMatch constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(AuthDTOs.ResetPasswordRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true; // @NotNull handles null validation
        }

        if (request.password() == null || request.confirmPassword() == null) {
            return true; // @NotNull handles null validation for individual fields
        }

        return request.password().equals(request.confirmPassword());
    }
}
