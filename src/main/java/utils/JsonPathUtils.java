package utils;

import static com.jayway.jsonpath.JsonPath.using;

import com.google.gson.JsonObject;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for the JsonPath library.
 */
public class JsonPathUtils {

  private static final String ROOT = "$";

  private JsonPathUtils() {
  }

  /**
   * Parses the Json using configuration {@link #getConfiguration()}.
   *
   * @param json JSON in string format.
   * @return The JSON document.
   */
  public static DocumentContext parse(String json) {
    return using(getConfiguration()).parse(json);
  }

  /**
   * Reads the field specified by jsonPath.
   *
   * @param document Json
   * @param jsonPath The JSON Path to read
   * @return The value of the property or null if not exists.
   */
  public static Object read(DocumentContext document, String jsonPath) {
    return parse(document.jsonString()).read(jsonPath);
  }

  /**
   * Executes a filter.
   *
   * @param document The document
   * @param jsonPath The Path
   * @return a list
   */
  public static List<HashMap<String, String>> executeFilter(
      DocumentContext document,
      String jsonPath) {
    return parse(document.jsonString()).read(jsonPath);
  }

  /**
   * Checks if property exists in JSON.
   *
   * @param document JSON.
   * @param jsonPath The JSON Path to read
   * @return True if property is found.
   * @throws com.google.gson.JsonIOException when property is not found.
   */
  public static Boolean propertyExists(DocumentContext document, String jsonPath) {
    return read(document, jsonPath) != null;
  }

  /**
   * Checks if property exists in JSON.
   *
   * @param document JSON.
   * @param jsonPath The JSON Path to property.
   * @param key      Appended to JSON Path before lookup.
   * @return True if property is found.
   */
  public static Boolean propertyExists(DocumentContext document, String jsonPath, String key) {
    var fullJsonPath = String.format("%s.%s", jsonPath, key);
    return propertyExists(document, fullJsonPath);
  }

  /**
   * Sets the property with the specified value.  Adds parent nodes if they do not already exist in
   * the JSON.
   *
   * @param document JSON.
   * @param jsonPath The JSON Path to property.
   * @param value    The value to add as a String.
   */
  public static void setValue(
      DocumentContext document,
      String jsonPath,
      Object value) {
    if (Boolean.TRUE.equals(propertyExists(document, jsonPath))) {
      set(document, jsonPath, value);
    } else {
      createAllNodes(document, jsonPath, value);
    }
  }

  public static void setJsonObject(
      DocumentContext document,
      String jsonPath,
      JsonObject jsonObject) {
    document.set(jsonPath, jsonObject);
  }

  /**
   * Appends value to an existing array in Environment Data.
   *
   * @param document JSON.
   * @param jsonPath The JSON Path to property.
   * @param value    Value to append.
   */
  public static void updateArray(
      DocumentContext document,
      String jsonPath,
      Map<String, Object> value) {
    if (Boolean.TRUE.equals(propertyExists(document, jsonPath))) {
      List<Map<String, Object>> values = document.read(jsonPath);

      var mutableList = new ArrayList<>(values);
      mutableList.add(value);
      document.set(jsonPath, mutableList);
    } else {
      var mutableList = new ArrayList<>();
      mutableList.add(value);
      document.set(jsonPath, mutableList);
    }
  }

  /**
   * Remove property from JSON.
   *
   * @param document JSON.
   * @param jsonPath JSON Path of the property to remove
   */
  public static void remove(DocumentContext document, String jsonPath)
      throws PathNotFoundException {
    if (Boolean.FALSE.equals(propertyExists(document, jsonPath))) {
      throw new PathNotFoundException(
          String.format("JSON Path %s not found in document", jsonPath));
    }

    document.delete(jsonPath);
  }

  /**
   * Returns JSON Array type value of given JSON Path.
   *
   * @param jsonPath JSON Path to lookup
   * @return String or throws when array is not found.
   */
  @SuppressWarnings("unchecked")
  public static List<Map<String, Object>> getArray(DocumentContext document, String jsonPath) {
    var obj = read(document, jsonPath);
    if (obj == null
        || (obj instanceof List<?>
        && (((List<?>) obj).isEmpty() || ((List<?>) obj).getFirst() instanceof Map<?, ?>))) {
      return (List<Map<String, Object>>) obj;
    } else {
      throw new IllegalArgumentException("Value must be an JSON Array");
    }
  }

  /**
   * Checks each node in the JSON Path, creating when it does not already exist.
   *
   * @param document JSON.
   * @param jsonPath The JSON Path of the property to add.
   * @param value    The value of the property to add, as a String.
   */
  private static void createAllNodes(
      DocumentContext document,
      String jsonPath,
      Object value) {
    var jsonPathNodes = jsonPath.split("\\.");
    for (var i = 2; i <= jsonPathNodes.length; i++) {
      if (i == jsonPathNodes.length) {
        var key = jsonPathNodes[jsonPathNodes.length - 1];
        put(
            document,
            String.join(
                ".",
                Arrays.copyOf(jsonPathNodes, jsonPathNodes.length - 1)),
            key,
            value);
      } else {
        var nodeValue = new HashMap<>();
        var parentJsonPathNodes = Arrays.copyOf(jsonPathNodes, i);
        var parentKey = parentJsonPathNodes[parentJsonPathNodes.length - 1];

        var parentJsonPath = String.join(
            ".",
            Arrays.copyOf(parentJsonPathNodes, parentJsonPathNodes.length - 1));
        putIfNotExists(document, parentJsonPath, parentKey, nodeValue);
      }
    }
  }

  private static void putIfNotExists(
      DocumentContext document,
      String jsonPath,
      String key,
      Object value) {
    if (Boolean.FALSE.equals(propertyExists(document, jsonPath, key))) {
      put(document, jsonPath, key, value);
    }
  }

  private static void set(
      DocumentContext document,
      String jsonPath,
      Object value) {
    document.set(jsonPath, value);
  }

  private static void put(
      DocumentContext document,
      String parentPath,
      String key,
      Object value) {
    if (!ROOT.equals(parentPath) && document.read(parentPath) instanceof Map) {
      HashMap<String, Object> existing = new HashMap<>(document.read(parentPath));
      existing.put(key, value);
      document.set(parentPath, existing);
    }

    document.put(parentPath, key, value);
  }

  /**
   * Configuration to prevent exceptions and allow for new properties to be added to the root of the
   * JSON document.
   *
   * @return The JSON PAth Configuration.
   */
  private static Configuration getConfiguration() {
    return com.jayway.jsonpath
        .Configuration
        .builder()
        .options(Option.SUPPRESS_EXCEPTIONS)
        .options(Option.DEFAULT_PATH_LEAF_TO_NULL)
        .build();
  }


}
