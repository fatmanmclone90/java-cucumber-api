package managers;

import com.microsoft.playwright.Playwright;
import java.util.Optional;

public class PlaywrightManager {

  private static PlaywrightManager instance;
  private final ThreadLocal<Playwright> playwrightThreadLocal = new ThreadLocal<>();

  /**
   * Retrieves the singleton instance of PlaywrightManager.
   *
   * @return The singleton instance of PlaywrightManager.
   * @throws NullPointerException If the browser factory has not been started.
   */
  public static PlaywrightManager get() {
    return Optional.ofNullable(instance)
        .orElseThrow(() -> new NullPointerException("Playwright manager has not started"));
  }

  /**
   * Starts the PlaywrightManager by creating a new instance of it.
   */
  public static void startPlaywright() {
    instance = new PlaywrightManager();
  }

  /**
   * Retrieves the current Playwright instance for the current thread.
   *
   * @return The current Playwright instance.
   */
  public Playwright playwright() {
    if (this.playwrightThreadLocal.get() == null) {
      playwrightThreadLocal.set(Playwright.create());
    }
    return this.playwrightThreadLocal.get();
  }

  /**
   * Ends the current test session.
   */
  public void teardown() {
    if (this.playwrightThreadLocal.get() != null) {
      this.playwrightThreadLocal.remove();
    }
  }

}
