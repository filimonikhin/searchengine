package searchengine.utility;

import java.util.regex.Pattern;

public class StrUtl {
    public static <T> T nvl(T expr1, T expr2) {
        return (expr1 != null) ? expr1 : expr2;
    }

    public static boolean isHrefFile(String url) {

        if ((url = nvl(url.toLowerCase(), "")).isBlank()) {
            return false;
        }

        String [] fileExtList = {".jpg",  ".jpeg", ".png", ".gif",  ".webp", ".pdf", ".eps",
                                   ".xlsx", ".xls",  ".doc", ".docx", ".ppts", ".ppt", ".icon",
                                   ".bmp",  ".rtf",  ".zip", ".fig",  ".nc",   ".m"};

        for (String ext : fileExtList) {
            if (url.endsWith(ext) ) {
                return true;
            }
        }

        return false;
    }

    public static boolean isContainsBadChar(String url) {
        Pattern badChars = Pattern.compile(".*[?#&].*");
        url = nvl(url, "");
        return badChars.matcher(url).matches();
    }
}
