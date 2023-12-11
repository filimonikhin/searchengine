package searchengine.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PageWithMessage {
    PageEntity page;
    String message;
}
