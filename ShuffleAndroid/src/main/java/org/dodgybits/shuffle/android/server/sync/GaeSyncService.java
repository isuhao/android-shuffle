package org.dodgybits.shuffle.android.server.sync;

import android.content.Intent;
import android.util.Log;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.textuality.aerc.AppEngineClient;
import com.textuality.aerc.Response;

import org.dodgybits.shuffle.android.preference.model.Preferences;
import org.dodgybits.shuffle.android.server.IntegrationSettings;
import org.dodgybits.shuffle.dto.ShuffleProtos;

import java.net.URL;

import roboguice.service.RoboIntentService;

import static org.dodgybits.shuffle.android.server.sync.SyncSchedulingService.CAUSE_EXTRA;
import static org.dodgybits.shuffle.android.server.sync.SyncSchedulingService.FAILED_STATUS_CAUSE;
import static org.dodgybits.shuffle.android.server.sync.SyncSchedulingService.NO_RESPONSE_CAUSE;
import static org.dodgybits.shuffle.android.server.sync.SyncSchedulingService.INVALID_AUTH_TOKEN_CAUSE;
import static org.dodgybits.shuffle.android.server.sync.SyncSchedulingService.SOURCE_EXTRA;
import static org.dodgybits.shuffle.android.server.sync.SyncSchedulingService.SYNC_FAILED_SOURCE;

public class GaeSyncService extends RoboIntentService {
    private static final String TAG = "GaeSyncService";

    @Inject
    SyncRequestBuilder requestBuilder;

    @Inject
    SyncResponseProcessor responseProcessor;

    @Inject
    IntegrationSettings integrationSettings;

    @Inject
    AuthTokenRetriever authTokenRetriever;

    private String authToken;
    private int attempt;

    public GaeSyncService() {
        super("GaeSyncService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Received sync intent");
        attempt = 1;
        performSync();
    }

    private void performSync() {
        authTokenRetriever.retrieveToken();
        authToken = Preferences.getSyncAuthToken(this);
        if (authToken != null) {
            callService();
        } else {
            Log.i(TAG, "No token, no sync");
        }
    }

    private void callService() {
        AppEngineClient client = new AppEngineClient(integrationSettings.getAppURL(), authToken, this);
        ShuffleProtos.SyncRequest syncRequest = requestBuilder.createRequest();
        byte[] body = syncRequest.toByteArray();
        transmit(body, client, integrationSettings.getSyncURL());
    }

    private void transmit(byte[] body, AppEngineClient client, URL target) {
        Response response = client.post(target, null, body);
        if (response == null) {
            error(client.errorMessage());

            Intent intent = new Intent(this, SyncSchedulingService.class);
            intent.putExtra(SOURCE_EXTRA, SYNC_FAILED_SOURCE);
            intent.putExtra(CAUSE_EXTRA, NO_RESPONSE_CAUSE);
            startService(intent);
            return;
        }

        if (!response.validAuthToken) {
            attempt++;
            if (attempt <= 3) {
                // permit 2 retries on invalid auth token
                Log.e(TAG, "Retry " + attempt + " for invalid auth token");
                performSync();
            } else {
                Intent intent = new Intent(this, SyncSchedulingService.class);
                intent.putExtra(SOURCE_EXTRA, SYNC_FAILED_SOURCE);
                intent.putExtra(CAUSE_EXTRA, INVALID_AUTH_TOKEN_CAUSE);
                startService(intent);
            }
            return;
        }

        if ((response.status / 100) != 2) {
            error("Upload failed: " + response.status);

            Intent intent = new Intent(this, SyncSchedulingService.class);
            intent.putExtra(SOURCE_EXTRA, SYNC_FAILED_SOURCE);
            intent.putExtra(CAUSE_EXTRA, FAILED_STATUS_CAUSE);
            startService(intent);

        } else {
            try {
                ShuffleProtos.SyncResponse syncResponse =
                        ShuffleProtos.SyncResponse.parseFrom(response.body);
                responseProcessor.process(syncResponse);
            } catch (InvalidProtocolBufferException e) {
                error("Response parsing failed : " + e.getMessage());
            }
        }
    }

    private void error(String s) {
        Log.e(TAG, s);
    }
}
