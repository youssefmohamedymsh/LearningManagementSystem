package io.github.youssefmohamedymsh.lms.model;

/** A student or course id that doesn't exist. */
public class UnknownIdException extends EnrollmentException {
    public UnknownIdException(String message) {
        super(message);
    }
}
