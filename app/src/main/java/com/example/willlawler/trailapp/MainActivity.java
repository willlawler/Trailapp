package com.example.willlawler.trailapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.Toast;

import com.esri.arcgisruntime.ArcGISRuntimeException;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureEditResult;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.popup.PopupAttachment;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;

public class MainActivity extends AppCompatActivity {

    private MapView mMapView;
    private LocationDisplay mLocationDisplay;
    private FeatureLayer mLayer;

    private List<Feature> mSelectedFeatures;
    private EditState mCurrentEditState;

    private ArcGISMap map;


    private void setupMap() {

        /*
        ArcGISTiledLayer tiledLayer = new ArcGISTiledLayer("http://maps.six.nsw.gov.au/arcgis/rest/services/public/NSW_Base_Map/MapServer");
        Basemap basemap = new Basemap(tiledLayer);
        ArcGISMap map = new ArcGISMap();
        map.setBasemap(basemap);

        ArcGISMap map = new ArcGISMap("https://bob-jane.maps.arcgis.com/home/item.html?id=b74c3e1f7f344b7099df9f9d78fcc273");
        mMapView.setMap(map);

 */

        //construct the portal from the URL of the portal
        Portal portal = new Portal("http://www.arcgis.com");
        //construct a portal item from the portal and item ID string
        PortalItem mapPortalItem = new PortalItem(portal, "b5adb856bc224c9483ffe10b3aafdbbb");
        //construct a map from the portal item
        map = new ArcGISMap(mapPortalItem);
        //addLayer(map);
        mMapView.setMap(map);
        final PortalItem portalItem = new PortalItem(portal, "bcbdeb93c6774b01b3b5bf0f76901df8");
        mLayer = new FeatureLayer(portalItem,0);
        map.getOperationalLayers().add(mLayer);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.toggleLayer:
                removeWaterLayer();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void removeWaterLayer(){
        if (map.getOperationalLayers().contains(mLayer)){
            map.getOperationalLayers().remove(mLayer);
        }
        else {
            map.getOperationalLayers().add(mLayer);
        }

    }



    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView = (MapView) findViewById(R.id.mapView);
        setupMap();
        setupLocationDisplay();




        // add listener to handle motion events, which only responds once a geodatabase is loaded
        mMapView.setOnTouchListener(
                new DefaultMapViewOnTouchListener(MainActivity.this, mMapView) {
                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
                        selectFeaturesAt(mapPointFrom(motionEvent), 10);

                        return true;
                    }
                }
        );
    }


    private Point mapPointFrom(MotionEvent motionEvent) {
        // get the screen point
        android.graphics.Point screenPoint = new android.graphics.Point(Math.round(motionEvent.getX()),
                Math.round(motionEvent.getY()));
        // return the point that was clicked in map coordinates
        return mMapView.screenToLocation(screenPoint);
    }

    private void selectFeaturesAt(Point point, int tolerance) {
        // define the tolerance for identifying the feature
        final double mapTolerance = tolerance * mMapView.getUnitsPerDensityIndependentPixel();
        // create objects required to do a selection with a query
        Envelope envelope = new Envelope(point.getX() - mapTolerance, point.getY() - mapTolerance,
                point.getX() + mapTolerance, point.getY() + mapTolerance, mMapView.getSpatialReference());
        QueryParameters query = new QueryParameters();
        query.setGeometry(envelope);
        mSelectedFeatures = new ArrayList<>();
        // select features within the envelope for all features on the map
        for (Layer layer : mMapView.getMap().getOperationalLayers()) {
            final FeatureLayer featureLayer = (FeatureLayer) layer;
            final ListenableFuture<FeatureQueryResult> featureQueryResultFuture = featureLayer
                    .selectFeaturesAsync(query, FeatureLayer.SelectionMode.NEW);
            // add done loading listener to fire when the selection returns
            featureQueryResultFuture.addDoneListener(new Runnable() {
                @Override
                public void run() {
                    // Get the selected features
                    final ListenableFuture<FeatureQueryResult> featureQueryResultFuture = featureLayer.getSelectedFeaturesAsync();
                    featureQueryResultFuture.addDoneListener(new Runnable() {
                        @Override public void run() {
                            try {
                                FeatureQueryResult layerFeatures = featureQueryResultFuture.get();
                                for (Feature feature : layerFeatures) {
                                    // Only select points for editing
                                    if (feature.getGeometry().getGeometryType() == GeometryType.POINT) {
                                        mSelectedFeatures.add(feature);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(getResources().getString(R.string.app_name), "Select feature failed: " + e.getMessage());
                            }
                        }
                    });
                    // set current edit state to editing
                    mCurrentEditState = EditState.Editing;
                }
            });
        }
    }

    private void setupLocationDisplay() {
        mLocationDisplay = mMapView.getLocationDisplay();
        mLocationDisplay.addDataSourceStatusChangedListener(new LocationDisplay.DataSourceStatusChangedListener() {
            @Override
            public void onStatusChanged(LocationDisplay.DataSourceStatusChangedEvent dataSourceStatusChangedEvent) {

                if (dataSourceStatusChangedEvent.isStarted() || dataSourceStatusChangedEvent.getError() == null) {
                    return;
                }

                int requestPermissionsCode = 2;
                String[] requestPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

                if (!(ContextCompat.checkSelfPermission(MainActivity.this, requestPermissions[0]) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(MainActivity.this, requestPermissions[1]) == PackageManager.PERMISSION_GRANTED)) {
                    ActivityCompat.requestPermissions(MainActivity.this, requestPermissions, requestPermissionsCode);
                } else {
                    String message = String.format("Error in DataSourceStatusChangedListener: %s",
                            dataSourceStatusChangedEvent.getSource().getLocationDataSource().getError().getMessage());
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                }
            }
        });
        mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.OFF);
        // .COMPASS_NAVIGATION = compass mode
        // .NAVIGATION = car mode
        // .OFF = no auto rotation
        mLocationDisplay.startAsync();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mLocationDisplay.startAsync();
        } else {
            Toast.makeText(MainActivity.this, getResources().getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    protected void onPause() {
        super.onPause();
        mMapView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.dispose();
    }

    public enum EditState {
        NotReady, // Geodatabase has not yet been generated
        Editing, // A feature is in the process of being moved
        Ready // The geodatabase is ready for synchronization or further edits
    }

}
