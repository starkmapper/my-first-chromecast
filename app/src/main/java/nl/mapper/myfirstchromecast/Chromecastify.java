package nl.mapper.myfirstchromecast;

import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v4.view.MenuItemCompat;


import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.cast.Cast.MessageReceivedCallback;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import java.io.IOException;


public class Chromecastify extends ActionBarActivity
{

  private static final String TAG = Chromecastify.class.getSimpleName();
  ChromeTour chromecast;
  @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chromecastify);
    chromecast = new ChromeTour(getApplicationContext());

    }

  class LeaderBoardChannel implements Cast.MessageReceivedCallback {
    public String getNamespace() {
      return getString(R.string.chromecase_namespace);
    }

    @Override
    public void onMessageReceived(CastDevice castDevice, String namespace,
                                  String message) {
      Log.d(TAG, "onMessageReceived: " + message);
    }
  }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chromecastify, menu);
      MenuItem ChromecastMenu = menu.findItem(R.id.media_route_menu_item);
      if (ChromecastMenu != null)
        chromecast.SetChromecastSelector(ChromecastMenu);

      chromecast.Start();
      return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
          chromecast.sendMessage(getString(R.string.loading_message));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

  @Override
  protected void onResume() {
    super.onResume();
    if (chromecast != null)
    chromecast.Start();
  }
  @Override
  protected void onPause() {
    if (isFinishing()) {
      chromecast.Stop();
    }
    super.onPause();
  }
  @Override
  protected void onStart() {
    super.onStart();
    chromecast.Start();;
  }

  @Override
  protected void onStop() {
    chromecast.Start();
    super.onStop();
  }


}
