/*
 * Copyright (c) 2023 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.notification;

import com.google.common.annotations.VisibleForTesting;
import io.airbyte.commons.json.Jsons;
import io.airbyte.commons.resources.MoreResources;
import io.airbyte.config.ActorDefinitionBreakingChange;
import io.airbyte.config.ActorType;
import io.airbyte.config.EnvConfigs;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Notification client that uses customer.io API send emails.
 *
 * These notifications rely on `TRANSACTION_MESSAGE_ID`, which are basically templates you create
 * through customer.io. These IDs are specific to a user's account on customer.io, so they will be
 * different for every user. For now they are stored as variables here, but in the future they may
 * be stored in as a notification config in the database.
 *
 * For Airbyte Cloud, Airbyte engineers may use `DEFAULT_TRANSACTION_MESSAGE_ID = "6"` as a generic
 * template for notifications.
 */
public class CustomerioNotificationClient extends NotificationClient {

  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

  private static final Logger LOGGER = LoggerFactory.getLogger(CustomerioNotificationClient.class);

  private static final String AUTO_DISABLE_TRANSACTION_MESSAGE_ID = "7";
  private static final String AUTO_DISABLE_WARNING_TRANSACTION_MESSAGE_ID = "8";
  private static final String BREAKING_CHANGE_WARNING_BROADCAST_ID = "32";
  private static final String BREAKING_CHANGE_SYNCS_DISABLED_BROADCAST_ID = "33";

  private static final String SYNC_SUCCEED_MESSAGE_ID = "18";
  private static final String SYNC_SUCCEED_TEMPLATE_PATH = "customerio/sync_succeed_template.json";
  private static final String SYNC_FAILURE_MESSAGE_ID = "19";
  private static final String SYNC_FAILURE_TEMPLATE_PATH = "customerio/sync_failure_template.json";

  private static final String CUSTOMERIO_BASE_URL = "https://api.customer.io/";
  private static final String CUSTOMERIO_EMAIL_API_ENDPOINT = "v1/send/email";
  private static final String CUSTOMERIO_BROADCAST_API_ENDPOINT_TEMPLATE = "v1/campaigns/%s/triggers";
  private static final String AUTO_DISABLE_NOTIFICATION_TEMPLATE_PATH = "customerio/auto_disable_notification_template.json";

  private static final String CUSTOMERIO_TYPE = "customerio";

  private final String baseUrl;
  private final OkHttpClient okHttpClient;
  private final String apiToken;

  public CustomerioNotificationClient() {
    final EnvConfigs configs = new EnvConfigs();
    this.apiToken = configs.getCustomerIoKey();
    this.okHttpClient = new OkHttpClient();
    this.baseUrl = CUSTOMERIO_BASE_URL;
  }

  @VisibleForTesting
  public CustomerioNotificationClient(final String apiToken,
                                      final String baseUrl) {
    this.apiToken = apiToken;
    this.baseUrl = baseUrl;
    this.okHttpClient = new OkHttpClient();
  }

  @Override
  public boolean notifyJobFailure(
                                  final String receiverEmail,
                                  final String sourceConnector,
                                  final String destinationConnector,
                                  final String connectionName,
                                  final String jobDescription,
                                  final String logUrl,
                                  final Long jobId)
      throws IOException {
    final String requestBody = renderTemplate(SYNC_FAILURE_TEMPLATE_PATH, SYNC_FAILURE_MESSAGE_ID, receiverEmail,
        receiverEmail, sourceConnector, destinationConnector, connectionName, jobDescription, logUrl, jobId.toString());
    return notifyByEmail(requestBody);
  }

  @Override
  public boolean notifyJobSuccess(
                                  final String receiverEmail,
                                  final String sourceConnector,
                                  final String destinationConnector,
                                  final String connectionName,
                                  final String jobDescription,
                                  final String logUrl,
                                  final Long jobId)
      throws IOException {
    final String requestBody = renderTemplate(SYNC_SUCCEED_TEMPLATE_PATH, SYNC_SUCCEED_MESSAGE_ID, receiverEmail,
        receiverEmail, sourceConnector, destinationConnector, connectionName, jobDescription, logUrl, jobId.toString());
    return notifyByEmail(requestBody);
  }

  // Once the configs are editable through the UI, the reciever email should be stored in
  // airbyte-config/models/src/main/resources/types/CustomerioNotificationConfiguration.yaml
  // instead of being passed in
  @Override
  public boolean notifyConnectionDisabled(final String receiverEmail,
                                          final String sourceConnector,
                                          final String destinationConnector,
                                          final String jobDescription,
                                          final UUID workspaceId,
                                          final UUID connectionId)
      throws IOException {
    final String requestBody = renderTemplate(AUTO_DISABLE_NOTIFICATION_TEMPLATE_PATH, AUTO_DISABLE_TRANSACTION_MESSAGE_ID, receiverEmail,
        receiverEmail, sourceConnector, destinationConnector, jobDescription, workspaceId.toString(), connectionId.toString());
    return notifyByEmail(requestBody);
  }

  @Override
  public boolean notifyConnectionDisableWarning(final String receiverEmail,
                                                final String sourceConnector,
                                                final String destinationConnector,
                                                final String jobDescription,
                                                final UUID workspaceId,
                                                final UUID connectionId)
      throws IOException {
    final String requestBody = renderTemplate(AUTO_DISABLE_NOTIFICATION_TEMPLATE_PATH, AUTO_DISABLE_WARNING_TRANSACTION_MESSAGE_ID, receiverEmail,
        receiverEmail, sourceConnector, destinationConnector, jobDescription, workspaceId.toString(), connectionId.toString());
    return notifyByEmail(requestBody);
  }

  @Override
  public boolean notifyBreakingChangeWarning(final List<String> receiverEmails,
                                             final String connectorName,
                                             final ActorType actorType,
                                             final ActorDefinitionBreakingChange breakingChange)
      throws IOException {
    return notifyByEmailBroadcast(BREAKING_CHANGE_WARNING_BROADCAST_ID, receiverEmails, Map.of(
        "connector_name", connectorName,
        "connector_type", actorType.value(),
        "connector_version_new", breakingChange.getVersion().serialize(),
        "connector_version_upgrade_deadline", breakingChange.getUpgradeDeadline(),
        "connector_version_change_description", breakingChange.getMessage(),
        "connector_version_migration_url", breakingChange.getMigrationDocumentationUrl()));
  }

  @Override
  public boolean notifyBreakingChangeSyncsDisabled(final List<String> receiverEmails,
                                                   final String connectorName,
                                                   final ActorType actorType,
                                                   final ActorDefinitionBreakingChange breakingChange)
      throws IOException {
    return notifyByEmailBroadcast(BREAKING_CHANGE_SYNCS_DISABLED_BROADCAST_ID, receiverEmails, Map.of(
        "connector_name", connectorName,
        "connector_type", actorType.value(),
        "connector_version_new", breakingChange.getVersion().serialize(),
        "connector_version_change_description", breakingChange.getMessage(),
        "connector_version_migration_url", breakingChange.getMigrationDocumentationUrl()));
  }

  @Override
  public boolean notifySuccess(final String message) {
    throw new NotImplementedException();
  }

  @Override
  public boolean notifyFailure(final String message) {
    throw new NotImplementedException();
  }

  @Override
  public boolean notifySchemaChange(final UUID connectionId,
                                    final boolean isBreaking,
                                    final String url) {
    throw new NotImplementedException();
  }

  @Override
  public boolean notifySchemaPropagated(final UUID connectionId, final List<String> changes, final String url, boolean isBreaking)
      throws IOException, InterruptedException {
    throw new NotImplementedException();
  }

  @Override
  public String getNotificationClientType() {
    return CUSTOMERIO_TYPE;
  }

  private boolean notifyByEmail(final String requestPayload) throws IOException {
    return sendNotifyRequest(CUSTOMERIO_EMAIL_API_ENDPOINT, requestPayload);
  }

  @VisibleForTesting
  boolean notifyByEmailBroadcast(final String broadcastId, final List<String> emails, final Map<String, String> data) throws IOException {
    if (emails.isEmpty()) {
      LOGGER.info("No emails to notify. Skipping email notification.");
      return false;
    }

    final String broadcastTriggerUrl = String.format(CUSTOMERIO_BROADCAST_API_ENDPOINT_TEMPLATE, broadcastId);

    final String payload = Jsons.serialize(Map.of(
        "emails", emails,
        "data", data,
        "email_add_duplicates", true,
        "email_ignore_missing", true,
        "id_ignore_missing", true));

    return sendNotifyRequest(broadcastTriggerUrl, payload);
  }

  @VisibleForTesting
  boolean sendNotifyRequest(final String urlEndpoint, final String payload) throws IOException {
    if (StringUtils.isEmpty(apiToken)) {
      LOGGER.info("Customer.io API token is empty. Skipping email notification.");
      return false;
    }

    final String url = baseUrl + urlEndpoint;
    final RequestBody requestBody = RequestBody.create(payload, JSON);

    final okhttp3.Request request = new Request.Builder()
        .addHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        .addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", apiToken))
        .url(url)
        .post(requestBody)
        .build();

    try (final Response response = okHttpClient.newCall(request).execute()) {
      if (response.isSuccessful()) {
        LOGGER.info("Successful notification ({}): {}", response.code(), response.body());
        return true;
      } else {
        final String errorMessage = String.format("Failed to deliver notification (%s): %s", response.code(), response.body());
        throw new IOException(errorMessage);
      }
    }
  }

  @Override
  public String renderTemplate(final String templateFile, final String... data) throws IOException {
    final String template = MoreResources.readResource(templateFile);
    return String.format(template, data);
  }

}
