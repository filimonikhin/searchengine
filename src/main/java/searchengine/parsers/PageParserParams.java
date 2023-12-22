package searchengine.parsers;

import lombok.*;
import searchengine.config.JsoupConfig;
import searchengine.repository.DBRepository;

@Builder
@Getter
@Setter
public class PageParserParams {
    final String pageAddress;
    final String root;
    final Integer siteId;
    final DBRepository dbRepository;
    final JsoupConfig jsoupConfig;
    final Boolean isRoot;
    final String fromPage;
    final Parser parser;
}
