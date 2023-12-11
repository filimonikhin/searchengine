package searchengine.dto;

import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;

public class OKResponse implements Response{
    JSONObject object = new JSONObject();

    public OKResponse() {
        object.put("result", true);
    }

    @Override
    public JSONObject get() {
        return object;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.OK;
    }
}
