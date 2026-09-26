package utils.Response;

public class CommandResult {
    int code ;
    String message ;
    String error ;
    public CommandResult(int code, String message , String error) {
        this.code = code;
        this.error = error;
        this.message = message;
    }
    public int getCode() {
        return code;
    }
    public String getMessage() {
        return message;
    }
    public String getError() {
        return error;
    }
}
