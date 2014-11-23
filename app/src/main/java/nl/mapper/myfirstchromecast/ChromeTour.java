package nl.mapper.myfirstchromecast;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;

/**
 * Created by stark on 23-11-14.
 */
public class ChromeTour
{
  Context appContext;
  public ChromeTour(Context ApplicationContext)
  {
    appContext = ApplicationContext;
    initChromecast();

  }

  private static final String TAG = Chromecastify.class.getSimpleName();
  private MediaRouter.Callback mMediaRouterCallback;
  MediaRouter chromecastRouter;
  MediaRouteSelector chromecastSelector;
  private String App_ID;

  private void initChromecast()
  {

    chromecastRouter = MediaRouter.getInstance(appContext);
    App_ID = appContext.getString(R.string.app_id);
    //App_ID = CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;


    chromecastSelector = new MediaRouteSelector.Builder()
        .addControlCategory(CastMediaControlIntent.categoryForCast(App_ID))
        .build();
    mMediaRouterCallback = new ChromecastRouterCallback();
  }

  class LeaderBoardChannel implements Cast.MessageReceivedCallback
  {
    public String getNamespace()
    {
      return appContext.getString(R.string.chromecase_namespace);
    }

    @Override
    public void onMessageReceived(CastDevice castDevice, String namespace,
                                  String message)
    {
      Log.d(TAG, "onMessageReceived: " + message);
    }
  }


  public void SetChromecastSelector(MenuItem mediaRouteMenuItem)
  {
    MediaRouteActionProvider mediaRouteActionProvider =
        (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
    mediaRouteActionProvider.setRouteSelector(chromecastSelector);
  }


  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    if (id == R.id.action_settings)
    {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    chromecastRouter.addCallback(chromecastSelector, mMediaRouterCallback,
        MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
  }

  @Override
  protected void onPause()
  {
    if (isFinishing())
    {
      chromecastRouter.removeCallback(mMediaRouterCallback);
    }
    super.onPause();
  }

  @Override
  protected void onStart()
  {
    super.onStart();
    chromecastRouter.addCallback(chromecastSelector, mMediaRouterCallback,
        MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
  }

  @Override
  protected void onStop()
  {
    chromecastRouter.removeCallback(mMediaRouterCallback);
    super.onStop();
  }

  CastDevice selectedChromecast;

  private class ChromecastRouterCallback extends MediaRouter.Callback
  {

    @Override
    public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info)
    {
      selectedChromecast = CastDevice.getFromBundle(info.getExtras());
      String routeId = info.getId();
      launchReceiver();

    }

    @Override
    public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info)
    {
      teardown();
      selectedChromecast = null;
    }

    private GoogleApiClient apiClient;
    private ConnectionCallbacks gapiConnectionCallbacks;
    private ConnectionFailedListener gapiConnectionFailedListener;
    private Cast.Listener mCastListener = new Cast.Listener()
    {
      @Override
      public void onApplicationStatusChanged()
      {
        if (apiClient != null)
        {
          Log.d(TAG, "onApplicationStatusChanged: "
              + Cast.CastApi.getApplicationStatus(apiClient));
        }
      }

      @Override
      public void onVolumeChanged()
      {
        if (apiClient != null)
        {
          Log.d(TAG, "onVolumeChanged: " + Cast.CastApi.getVolume(apiClient));
        }
      }

      @Override
      public void onApplicationDisconnected(int errorCode)
      {
        teardown();
      }
    };

    private void launchReceiver()
    {
      try
      {
        // Connect to Google Play services
        gapiConnectionCallbacks = new ConnectionCallbacks();
        gapiConnectionFailedListener = new ConnectionFailedListener();
        Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
            .builder(selectedChromecast, mCastListener);
        apiClient = new GoogleApiClient.Builder(getApplicationContext())
            .addApi(Cast.API, apiOptionsBuilder.build())
            .addConnectionCallbacks(gapiConnectionCallbacks)
            .addOnConnectionFailedListener(gapiConnectionFailedListener)
            .build();

        apiClient.connect();
      }
      catch (Exception e)
      {
        Log.e(TAG, "Failed launchReceiver", e);
      }

    }

    private boolean mWaitingForReconnect;


    private void reconnectChannels()
    {
      // TODO something?
    }

    private LeaderBoardChannel leaderChannel;
    boolean chromecastApplicationLaunched;
    private String chromecastSessionId;

    private class ConnectionCallbacks implements
        GoogleApiClient.ConnectionCallbacks
    {
      @Override
      public void onConnected(Bundle connectionHint)
      {
        if (mWaitingForReconnect)
        {
          mWaitingForReconnect = false;
          reconnectChannels();
        } else
        {
          try
          {
            Cast.CastApi.launchApplication(apiClient, App_ID, false)
                .setResultCallback(
                    new ResultCallback<Cast.ApplicationConnectionResult>()
                    {
                      @Override
                      public void onResult(Cast.ApplicationConnectionResult result)
                      {
                        Status status = result.getStatus();
                        if (status.isSuccess())
                        {
                          ApplicationMetadata applicationMetadata = result.getApplicationMetadata();
                          chromecastSessionId = result.getSessionId();
                          String applicationStatus = result.getApplicationStatus();
                          boolean wasLaunched = result.getWasLaunched();
                          chromecastApplicationLaunched = true;
                          leaderChannel = new LeaderBoardChannel();
                          try
                          {
                            Cast.CastApi.setMessageReceivedCallbacks(apiClient, leaderChannel.getNamespace(), leaderChannel);
                            sendString(getString(R.string.loading_message));
                          }
                          catch (IOException e)
                          {
                            Log.e(TAG, "Failed to create chanel", e);
                          }

                        } else
                        {
                          teardown();
                        }
                      }
                    });

          }
          catch (Exception e)
          {
            Log.e(TAG, "Failed to launch application", e);
          }
        }
      }

      @Override
      public void onConnectionSuspended(int cause)
      {
        mWaitingForReconnect = true;
      }
    }

    private void sendString(String string)
    {
      string = string.replace("\n", "<br/>");
      if (apiClient != null && leaderChannel != null)
      {
        try
        {
          Cast.CastApi.sendMessage(apiClient, leaderChannel.getNamespace(), string)
              .setResultCallback(
                  new ResultCallback<Status>()
                  {
                    @Override
                    public void onResult(Status result)
                    {
                      if (!result.isSuccess())
                      {
                        Log.e(TAG, "Sending message failed");
                      }
                    }
                  });
        }
        catch (Exception e)
        {
          Log.e(TAG, "Exception while sending message", e);
        }
      }
    }

    private class ConnectionFailedListener implements
        GoogleApiClient.OnConnectionFailedListener
    {
      @Override
      public void onConnectionFailed(ConnectionResult result)
      {
        teardown();
      }
    }

    private void teardown()
    {
      if (apiClient != null)
      {
        if (chromecastApplicationLaunched)
        {
          if (apiClient.isConnected() || apiClient.isConnecting())
          {
            try
            {
              Cast.CastApi.stopApplication(apiClient, chromecastSessionId);
              if (leaderChannel != null)
              {
                Cast.CastApi.removeMessageReceivedCallbacks(apiClient, leaderChannel.getNamespace());
                leaderChannel = null;
              }
            }
            catch (IOException e)
            {
              Log.e(TAG, "Exception while removing channel", e);
            }
            apiClient.disconnect();
          }
          chromecastApplicationLaunched = false;
        }
        apiClient = null;
      }
      selectedChromecast = null;
      mWaitingForReconnect = false;
      chromecastSessionId = null;
    }
  }
}