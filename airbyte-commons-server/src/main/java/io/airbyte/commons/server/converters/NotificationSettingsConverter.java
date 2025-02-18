/*
 * Copyright (c) 2023 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.commons.server.converters;

import io.airbyte.commons.enums.Enums;
import java.util.stream.Collectors;

/**
 * Convert between API and internal versions of notification models.
 */
@SuppressWarnings({"MissingJavadocMethod", "LineLength"})
public class NotificationSettingsConverter {

  public static io.airbyte.config.NotificationSettings toConfig(final io.airbyte.api.model.generated.NotificationSettings notification) {
    io.airbyte.config.NotificationSettings configNotificationSettings = new io.airbyte.config.NotificationSettings();
    if (notification == null) {
      return configNotificationSettings;
    }

    if (notification.getSendOnSuccess() != null) {
      configNotificationSettings.setSendOnSuccess(toConfig(notification.getSendOnSuccess()));
    }
    if (notification.getSendOnFailure() != null) {
      configNotificationSettings.setSendOnFailure(toConfig(notification.getSendOnFailure()));
    }
    if (notification.getSendOnConnectionUpdate() != null) {
      configNotificationSettings.setSendOnConnectionUpdate(toConfig(notification.getSendOnConnectionUpdate()));
    }
    if (notification.getSendOnSyncDisabled() != null) {
      configNotificationSettings.setSendOnSyncDisabled(toConfig(notification.getSendOnSyncDisabled()));
    }
    if (notification.getSendOnSyncDisabledWarning() != null) {
      configNotificationSettings.setSendOnSyncDisabledWarning(toConfig(notification.getSendOnSyncDisabledWarning()));
    }
    if (notification.getSendOnConnectionUpdateActionRequired() != null) {
      configNotificationSettings.setSendOnConnectionUpdateActionRequired(toConfig(notification.getSendOnConnectionUpdateActionRequired()));
    }
    if (notification.getSendOnBreakingChangeWarning() != null) {
      configNotificationSettings.setSendOnBreakingChangeWarning(toConfig(notification.getSendOnBreakingChangeWarning()));
    }
    if (notification.getSendOnBreakingChangeSyncsDisabled() != null) {
      configNotificationSettings.setSendOnBreakingChangeSyncsDisabled(toConfig(notification.getSendOnBreakingChangeSyncsDisabled()));
    }

    return configNotificationSettings;
  }

  // Currently customerIoConfiguration is an empty object, so we tend to keep it as null.
  private static io.airbyte.config.NotificationItem toConfig(final io.airbyte.api.model.generated.NotificationItem notificationItem) {
    io.airbyte.config.NotificationItem result = new io.airbyte.config.NotificationItem()
        .withNotificationType(notificationItem.getNotificationType().stream()
            .map(notificationType -> Enums.convertTo(notificationType, io.airbyte.config.Notification.NotificationType.class)).collect(
                Collectors.toList()));

    if (notificationItem.getSlackConfiguration() != null) {
      result.withSlackConfiguration(toConfig(notificationItem.getSlackConfiguration()));
    }
    return result;
  }

  /**
   * Convert SlackNotificationConfiguration from api to config. Used in notifications/trywebhook api
   * path.
   */
  public static io.airbyte.config.SlackNotificationConfiguration toConfig(final io.airbyte.api.model.generated.SlackNotificationConfiguration notification) {
    if (notification == null) {
      return new io.airbyte.config.SlackNotificationConfiguration();
    }
    return new io.airbyte.config.SlackNotificationConfiguration()
        .withWebhook(notification.getWebhook());
  }

  public static io.airbyte.api.model.generated.NotificationSettings toApi(final io.airbyte.config.NotificationSettings notificationSettings) {
    io.airbyte.api.model.generated.NotificationSettings apiNotificationSetings = new io.airbyte.api.model.generated.NotificationSettings();
    if (notificationSettings == null) {
      return apiNotificationSetings;
    }

    if (notificationSettings.getSendOnSuccess() != null) {
      apiNotificationSetings.setSendOnSuccess(toApi(notificationSettings.getSendOnSuccess()));
    }
    if (notificationSettings.getSendOnFailure() != null) {
      apiNotificationSetings.setSendOnFailure(toApi(notificationSettings.getSendOnFailure()));
    }
    if (notificationSettings.getSendOnConnectionUpdate() != null) {
      apiNotificationSetings.setSendOnConnectionUpdate(toApi(notificationSettings.getSendOnConnectionUpdate()));
    }
    if (notificationSettings.getSendOnSyncDisabled() != null) {
      apiNotificationSetings.setSendOnSyncDisabled(toApi(notificationSettings.getSendOnSyncDisabled()));
    }
    if (notificationSettings.getSendOnSyncDisabledWarning() != null) {
      apiNotificationSetings.setSendOnSyncDisabledWarning(toApi(notificationSettings.getSendOnSyncDisabledWarning()));
    }
    if (notificationSettings.getSendOnConnectionUpdateActionRequired() != null) {
      apiNotificationSetings.setSendOnConnectionUpdateActionRequired(toApi(notificationSettings.getSendOnConnectionUpdateActionRequired()));
    }
    if (notificationSettings.getSendOnBreakingChangeWarning() != null) {
      apiNotificationSetings.setSendOnBreakingChangeWarning(toApi(notificationSettings.getSendOnBreakingChangeWarning()));
    }
    if (notificationSettings.getSendOnBreakingChangeSyncsDisabled() != null) {
      apiNotificationSetings.setSendOnBreakingChangeSyncsDisabled(toApi(notificationSettings.getSendOnBreakingChangeSyncsDisabled()));
    }
    return apiNotificationSetings;
  }

  private static io.airbyte.api.model.generated.NotificationItem toApi(final io.airbyte.config.NotificationItem notificationItem) {
    var result = new io.airbyte.api.model.generated.NotificationItem()
        .notificationType(notificationItem.getNotificationType().stream()
            .map(notificationType -> Enums.convertTo(notificationType, io.airbyte.api.model.generated.NotificationType.class)).collect(
                Collectors.toList()));
    if (notificationItem.getSlackConfiguration() != null) {
      result.slackConfiguration(toApi(notificationItem.getSlackConfiguration()));
    }
    return result;
  }

  private static io.airbyte.api.model.generated.SlackNotificationConfiguration toApi(final io.airbyte.config.SlackNotificationConfiguration notification) {
    if (notification == null) {
      return new io.airbyte.api.model.generated.SlackNotificationConfiguration();
    }
    return new io.airbyte.api.model.generated.SlackNotificationConfiguration()
        .webhook(notification.getWebhook());
  }

}
