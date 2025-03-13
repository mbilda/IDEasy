package com.devonfw.tools.ide.url.updater;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.tool.docker.DockerDesktopUrlUpdater;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.report.UrlFinalReport;

/**
 * The {@code UpdateManager} class manages the update process for various tools by using a list of {@link AbstractUrlUpdater}s to update the
 * {@link UrlRepository}. The list of {@link AbstractUrlUpdater}s contains crawlers for different tools and services, To use the UpdateManager, simply create an
 * instance with the path to the repository as a parameter and call the {@link #updateAll(UrlFinalReport)} method.
 */
public class UpdateManager extends AbstractProcessorWithTimeout {

  private static final Logger logger = LoggerFactory.getLogger(AbstractUrlUpdater.class);

  private final UrlRepository urlRepository;

  private UrlFinalReport urlFinalReport;

  private final List<AbstractUrlUpdater> updaters = List.of(new DockerDesktopUrlUpdater());

  /**
   * The constructor.
   *
   * @param pathToRepository the {@link Path} to the {@code ide-urls} repository to update.
   * @param expirationTime for GitHub actions url-update job
   */
  public UpdateManager(Path pathToRepository, UrlFinalReport urlFinalReport, Instant expirationTime) {

    this.urlRepository = UrlRepository.load(pathToRepository);
    this.urlFinalReport = urlFinalReport;
    setExpirationTime(expirationTime);
  }

  /**
   * Updates {@code ide-urls} for all tools their editions and all found versions.
   */
  public void updateAll() {

    for (AbstractUrlUpdater updater : this.updaters) {
      if (isTimeoutExpired()) {
        break;
      }
      update(updater);
    }
  }

  /**
   * Update only a single tool. Mainly used in local development only to test updater only for a tool where changes have been made.
   *
   * @param tool the name of the tool to update.
   */
  public void update(String tool) {

    for (AbstractUrlUpdater updater : this.updaters) {
      if (updater.getTool().equals(tool)) {
        update(updater);
      }
    }
  }

  private void update(AbstractUrlUpdater updater) {
    try {
      updater.setExpirationTime(getExpirationTime());
      updater.setUrlFinalReport(this.urlFinalReport);
      String updaterName = updater.getClass().getSimpleName();
      String toolName = updater.getTool();
      logger.debug("Starting {} for tool {}", updaterName, toolName);
      updater.update(this.urlRepository);
      logger.debug("Ended {} for tool {}", updaterName, updater.getTool());
    } catch (Exception e) {
      logger.error("Failed to update {}", updater.getToolWithEdition(), e);
    }
  }

}
