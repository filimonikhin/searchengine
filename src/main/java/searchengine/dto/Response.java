package searchengine.dto;

import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;

public interface Response {
    JSONObject get();
    HttpStatus getHttpStatus();
}