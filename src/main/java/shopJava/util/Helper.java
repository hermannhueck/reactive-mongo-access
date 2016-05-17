package shopJava.util;

import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

public class Helper {

    public static void printJson(final Document doc) {
        System.out.println(toJson(doc));
    }

    public static void printJsonPretty(final Document doc) {
        System.out.println(toJsonPretty(doc));
    }

    public static String toJsonPretty(final Document doc) {
        return toJson(doc, true);
    }

    public static String toJson(final Document doc) {
        return toJson(doc, false);
    }

    public static String toJson(final Document doc, boolean pretty) {
        return doc.toJson(new JsonWriterSettings(JsonMode.SHELL, pretty));
    }
}
