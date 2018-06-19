package com.example.willlawler.trailapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;


public class MainActivity extends AppCompatActivity {

    private MapView mMapView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        mMapView = findViewById(R.id.mapView);
        ArcGISMap map = new ArcGISMap(Basemap.Type.TOPOGRAPHIC, 34.056295, -117.195800, 16);
        mMapView.setMap(map);
        */
        mMapView = findViewById(R.id.mapView);
        ArcGISTiledLayer tiledLayer = new ArcGISTiledLayer("http://maps.six.nsw.gov.au/arcgis/rest/services/public/NSW_Base_Map/MapServer");
        Basemap basemap = new Basemap(tiledLayer);



        ArcGISMap map = new ArcGISMap();
        map.setBasemap(basemap);
        mMapView.setMap(map);
    }

    @Override
    protected void onPause(){
        mMapView.pause();
        super.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mMapView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.dispose();
    }
}
