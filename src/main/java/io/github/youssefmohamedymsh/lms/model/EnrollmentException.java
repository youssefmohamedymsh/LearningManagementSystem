package io.github.youssefmohamedymsh.lms.model;

/** A request that breaks an enrollment rule. The message is shown to the user as-is. */
public class EnrollmentException extends RuntimeException {
    public EnrollmentException(String message) {
        super(message);
    }
}
