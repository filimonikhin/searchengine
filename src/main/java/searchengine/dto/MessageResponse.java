package searchengine.dto;

import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;

public class MessageResponse implements Response{
    JSONObject object = new JSONObject();
    HttpStatus status;

    public MessageResponse(String message, HttpStatus status) {
        object.put("result", false);
        object.put("error", message);
        this.status = status;
    }

    @Override
    public JSONObject get() {
        return object;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return status;
    }
}
