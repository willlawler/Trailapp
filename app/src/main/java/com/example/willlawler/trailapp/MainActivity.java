package com.example.willlawler.trailapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.Toast;

import com.esri.arcgisruntime.ArcGISRuntimeException;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureEditResult;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private MapView mMapView;
    private LocationDisplay mLocationDisplay;
    private FeatureLayer mLayer;

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
        ArcGISMap map = new ArcGISMap(mapPortalItem);
        //addLayer(map);
        mMapView.setMap(map);
        final PortalItem portalItem = new PortalItem(portal, "bcbdeb93c6774b01b3b5bf0f76901df8");
        mLayer = new FeatureLayer(portalItem,0);
        map.getOperationalLayers().add(mLayer);

       
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView = (MapView) findViewById(R.id.mapView);
        setupMap();
        setupLocationDisplay();


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

}
