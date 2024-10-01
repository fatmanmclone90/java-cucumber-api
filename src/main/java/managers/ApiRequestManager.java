package managers;

import com.smartbear.har.creator.DefaultHarStreamWriter;
import com.smartbear.har.model.HarEntry;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class ApiRequestManager {

  private static final String HAR_FOLDER = "har";
  private static final String LOG_FOLDER_PATH = String.format("%s/%s",
      ScenarioManager.SOURCE_FOLDER, HAR_FOLDER);
  private static ApiRequestManager instance;
  private final ThreadLocal<ArrayList<HarEntry>> requests;

  public ApiRequestManager() {
    this.requests = ThreadLocal.withInitial(ArrayList::new);
  }

  public static synchronized ApiRequestManager get() {
    if (instance == null) {
      instance = new ApiRequestManager();
    }
    return instance;
  }

  public void addRequest(HarEntry harEntry) {
    this.requests.get().add(harEntry);
  }

  public Path writeHar() throws IOException {
    if (this.requests.get().isEmpty()) {
      return null;
    } else {
      Files.createDirectories(Path.of(LOG_FOLDER_PATH));
      var fileName = ScenarioManager.instance().getFileName(HAR_FOLDER, "har");
      var harWriter = new DefaultHarStreamWriter.Builder()
          .withOutputFile(new File(fileName))
          .withUsePrettyPrint(true)
          .build();

      for (var request : requests.get()) {
        harWriter.addEntry(request);
      }
      harWriter.closeHar();

      return Path.of(fileName);
    }
  }

  public void teardown() {
    this.requests.remove();
  }

}
