package project.drkninja.lbar;

import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.SensorManager;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import com.wikitude.architect.ArchitectView;
import com.wikitude.architect.StartupConfiguration;

import java.io.File;
import java.io.FileOutputStream;

import project.drkninja.utility.References;
import project.drkninja.wikitude.AbstractArchitectCamActivity;
import project.drkninja.wikitude.ArchitectViewHolderInterface;
import project.drkninja.wikitude.LocationProvider;
import project.drkninja.wikitude.SamplePoiDetailActivity;

public class POICam extends AbstractArchitectCamActivity {

    /**
     * last time the calibration toast was shown, this avoids too many toast shown when compass needs calibration
     */
    private long lastCalibrationToastShownTimeMillis = System.currentTimeMillis();

    @Override
    public String getARchitectWorldPath() {
        return getIntent().getExtras().getString(
                References.EXTRAS_KEY_ACTIVITY_ARCHITECT_WORLD_URL);
    }

    @Override
    public String getActivityTitle() {
        return (getIntent().getExtras() != null && getIntent().getExtras().get(
                References.EXTRAS_KEY_ACTIVITY_TITLE_STRING) != null) ? getIntent()
                .getExtras().getString(References.EXTRAS_KEY_ACTIVITY_TITLE_STRING)
                : "Test-World";
    }

    @Override
    public int getContentViewId() {
        return R.layout.poi_cam;
    }

    @Override
    public int getArchitectViewId() {
        return R.id.architectView;
    }

    @Override
    public String getWikitudeSDKLicenseKey() {
        return References.WIKI_KEY;
    }

    @Override
    public ArchitectView.SensorAccuracyChangeListener getSensorAccuracyListener() {
        return new ArchitectView.SensorAccuracyChangeListener() {
            @Override
            public void onCompassAccuracyChanged(int accuracy) {
				/* UNRELIABLE = 0, LOW = 1, MEDIUM = 2, HIGH = 3 */
                if (accuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM && POICam.this != null && !POICam.this.isFinishing() && System.currentTimeMillis() - POICam.this.lastCalibrationToastShownTimeMillis > 5 * 1000) {
                    Toast.makeText(POICam.this, R.string.compass_accuracy_low, Toast.LENGTH_LONG).show();
                    POICam.this.lastCalibrationToastShownTimeMillis = System.currentTimeMillis();
                }
            }
        };
    }

    @Override
    public ArchitectView.ArchitectUrlListener getUrlListener() {
        return new ArchitectView.ArchitectUrlListener() {

            @Override
            public boolean urlWasInvoked(String uriString) {
                Uri invokedUri = Uri.parse(uriString);

                // pressed "More" button on POI-detail panel
                if ("markerselected".equalsIgnoreCase(invokedUri.getHost())) {
                    final Intent poiDetailIntent = new Intent(POICam.this, SamplePoiDetailActivity.class);
                    poiDetailIntent.putExtra(SamplePoiDetailActivity.EXTRAS_KEY_POI_ID, String.valueOf(invokedUri.getQueryParameter("id")));
                    poiDetailIntent.putExtra(SamplePoiDetailActivity.EXTRAS_KEY_POI_TITILE, String.valueOf(invokedUri.getQueryParameter("title")));
                    poiDetailIntent.putExtra(SamplePoiDetailActivity.EXTRAS_KEY_POI_DESCR, String.valueOf(invokedUri.getQueryParameter("description")));
                    POICam.this.startActivity(poiDetailIntent);
                    return true;
                }

                // pressed snapshot button. check if host is button to fetch e.g. 'architectsdk://button?action=captureScreen', you may add more checks if more buttons are used inside AR scene
                else if ("button".equalsIgnoreCase(invokedUri.getHost())) {
                    POICam.this.architectView.captureScreen(ArchitectView.CaptureScreenCallback.CAPTURE_MODE_CAM_AND_WEBVIEW, new ArchitectView.CaptureScreenCallback() {

                        @Override
                        public void onScreenCaptured(final Bitmap screenCapture) {
                            // store screenCapture into external cache directory
                            final File screenCaptureFile = new File(Environment.getExternalStorageDirectory().toString(), "screenCapture_" + System.currentTimeMillis() + ".jpg");

                            // 1. Save bitmap to file & compress to jpeg. You may use PNG too
                            try {
                                final FileOutputStream out = new FileOutputStream(screenCaptureFile);
                                screenCapture.compress(Bitmap.CompressFormat.JPEG, 90, out);
                                out.flush();
                                out.close();

                                // 2. create send intent
                                final Intent share = new Intent(Intent.ACTION_SEND);
                                share.setType("image/jpg");
                                share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(screenCaptureFile));

                                // 3. launch intent-chooser
                                final String chooserTitle = "Share Snaphot";
                                POICam.this.startActivity(Intent.createChooser(share, chooserTitle));

                            } catch (final Exception e) {
                                // should not occur when all permissions are set
                                POICam.this.runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        // show toast message in case something went wrong
                                        Toast.makeText(POICam.this, "Unexpected error, " + e, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    });
                }
                return true;
            }
        };
    }

    @Override
    public ILocationProvider getLocationProvider(final LocationListener locationListener) {
        return new LocationProvider(this, locationListener);
    }

    @Override
    public float getInitialCullingDistanceMeters() {
        // you need to adjust this in case your POIs are more than 50km away from user here while loading or in JS code (compare 'AR.context.scene.cullingDistance')
        return ArchitectViewHolderInterface.CULLING_DISTANCE_DEFAULT_METERS;
    }

    @Override
    protected boolean hasGeo() {
        return true;
    }

    @Override
    protected boolean hasIR() {
        return getIntent().getExtras().getBoolean(
                References.EXTRAS_KEY_ACTIVITY_IR);
    }

    @Override
    protected StartupConfiguration.CameraPosition getCameraPosition() {
        return StartupConfiguration.CameraPosition.DEFAULT;
    }
}