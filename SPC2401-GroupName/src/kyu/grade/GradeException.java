package kyu.grade;

/** Application-level error (bad mark, unknown student...). Travels to the client by value. */
public class GradeException extends Exception {
    private static final long serialVersionUID = 1L;
    public GradeException(String message) { super(message); }
}
