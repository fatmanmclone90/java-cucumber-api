package utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

/**
 * Utility class for serializing JSON in a consistent way.
 */
public class JsonSerializer {

  private static final Gson gson;

  /**
   * Writes indented JSON for readability.
   */
  private static final Gson formattedGson;

  static {
    var builder = getBuilder(false);
    gson = builder.create();

    var formattedBuilder = getBuilder(true);
    formattedGson = formattedBuilder.create();
  }

  private static GsonBuilder getBuilder(boolean setPrettyPrinting) {
    var builder = new GsonBuilder();
    builder.disableHtmlEscaping();
    builder.excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT);
    if (setPrettyPrinting) {
      builder.setPrettyPrinting();
    }
    return builder;
  }

  public static String toJson(Object object) {
    return gson.toJson(object);
  }

  public static String prettyPrint(String json) throws JsonSyntaxException {
    return gson.toJson(JsonParser.parseString(json).getAsJsonObject());
  }

  /**
   * Pretty prints Json text.
   *
   * @param json      Json text
   * @param formatted When true writes indented JSON
   * @return Json text
   * @throws JsonSyntaxException If invalid JSON provided
   */
  public static String prettyPrint(String json, boolean formatted) throws JsonSyntaxException {
    return formatted
        ? formattedGson.toJson(JsonParser.parseString(json).getAsJsonObject())
        : gson.toJson(JsonParser.parseString(json).getAsJsonObject());
  }

  public static <T> T fromObjectToObject(Object o, Class<T> type) {
    return fromJson(toJson(o), type);
  }

  public static <T> T fromObjectToObject(Object o, Type typeOfT) {
    return fromJson(toJson(o), typeOfT);
  }

  public static <T> T fromJson(String response, TypeToken<T> type) {
    return gson.fromJson(response, type);
  }

  public static <T> T fromJson(String response, Class<T> type) {
    return gson.fromJson(response, type);
  }

  @SuppressWarnings({"unchecked", "java::S1772"})
  public static <T> T fromJson(String json, Type typeOfT) {
    return (T) gson.fromJson(json, TypeToken.get(typeOfT));
  }
}