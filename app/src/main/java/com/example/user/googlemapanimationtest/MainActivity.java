package com.example.user.googlemapanimationtest;

import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Toast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
    private SyncedMapFragment mMapFragment;
    private GoogleMap mMap;
    private Marker mMarker;
    LatLng latLng = new LatLng(23.696136, 120.534142);
    Location location;
    final List<LatLng> latLng2=new ArrayList(); //此陣列是為了將之後路線規劃所有的點存起來並用setPoints()畫出
    PolylineOptions polylineOptions;
    Polyline polyline= null;
    private LatLngInterpolator mLatLngInterpolator;
    Handler mThreadHandler;
    Button button;
    Button btClear;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapFragment = new SyncedMapFragment(); //設定自訂同步器，讓map的fragment在變動時能夠被自己寫的功能同步
        getSupportFragmentManager().beginTransaction()
                .add(R.id.map, mMapFragment) //設定map介面id與同步自訂fragment
                .commit();

        button=(Button)findViewById(R.id.simulation_button);
        btClear=(Button)findViewById(R.id.clear);
    }

    @Override
    protected void onResumeFragments()  //所有與map有直接關連的功能需全部寫在這，因為這裡是確保map正常運作後才執行的
    {
        super.onResumeFragments(); //延續父寫法
        mMap = mMapFragment.getMap(); //取得自訂fragment的map
        mMap.setOnMapClickListener(mMapClickListener); //設定map監聽韓式，當map點及事件觸發時執行

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        mMarker=mMap.addMarker(markerOptions); //將起始點記錄在mMarker中

        //==================set map draw line======================

        latLng2.add(new LatLng(latLng.latitude,latLng.longitude)); //先加入起始點
        polylineOptions=new PolylineOptions() //畫線的細部設定
                .width(10)
                .color(Color.RED)
                .zIndex(2);//
        polyline=mMap.addPolyline(polylineOptions); //將畫線設定套用至map上
        //===========此map mark觸發設定是為了讓路線規畫能回到原點=============
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter(){
            //private final View infoWindow=getLayoutInflater().inflate(R.layout.user_position,null);
            @Override
            public View getInfoWindow(Marker marker) {
                latLng2.add(new LatLng(latLng.latitude,latLng.longitude)); //設定當原點的mark被按下時，將座標存入
                polyline.setPoints(latLng2);
                return null;
            }
            @Override
            public View getInfoContents(Marker marker) {
                return null;
            }
        });
        btClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                mMarker=mMap.addMarker(markerOptions); //將起始點記錄在mMarker中
                latLng2.clear();
                latLng2.add(new LatLng(latLng.latitude,latLng.longitude)); //先加入起始點
                polyline=mMap.addPolyline(polylineOptions); //將畫線設定套用至map上
            }
        });


        //Button button=(Button)findViewById(R.id.simulation_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mThreadHandler = new Handler();
                mThreadHandler.postDelayed(runnable1,100);
            }
        });

    }
    private int index=1; //略過起點
    private Runnable runnable1=new Runnable () {
        public void run() {

            if(mMarker!=null&&index<latLng2.size()){
                mMapFragment.animateMarkerToGB(mMarker, latLng2.get(index), mLatLngInterpolator, 1500); //使用外部java檔實作mark動畫
                index++;
                btClear.setEnabled(false);
                mThreadHandler.postDelayed(this,2500); //遞迴性呼叫自己

            }else{
                index=1;
                btClear.setEnabled(true);
            }


        }

    };


    private GoogleMap.OnMapClickListener mMapClickListener = new GoogleMap.OnMapClickListener()
    {


        @Override
        public void onMapClick(LatLng latLng)
        {
            /*
            if(mMarker != null){
                mMapFragment.animateMarkerToGB(mMarker, point, mLatLngInterpolator, 1500);
            }
            else{
                mLatLngInterpolator = new LatLngInterpolator.Linear();
                mMarker = mMap.addMarker(new MarkerOptions().position(point));
            }
            */
            mLatLngInterpolator = new LatLngInterpolator.Linear(); //這行一定要加在每次click新增mark的時候，目的是為了讓mark動畫知道此點的資訊與存在

            Toast.makeText(MainActivity.this,latLng.latitude+" "+latLng.longitude,Toast.LENGTH_SHORT).show();
            latLng2.add(new LatLng(latLng.latitude,latLng.longitude));
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng2.get(latLng2.size()-1));
            int temp=latLng2.size()-1;
            switch(temp){
                case 1:
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_1)); //設定圖示
                    break;
                case 2:
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_2)); //設定圖示
                    break;
                case 3:
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_3)); //設定圖示
                    break;
                case 4:
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_4)); //設定圖示
                    break;
                case 5:
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_5)); //設定圖示
                    break;
                case 6:
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_6)); //設定圖示
                    break;
                case 7:
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_7)); //設定圖示
                    break;
                case 8:
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_8)); //設定圖示
                    break;
                case 9:
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_9)); //設定圖示
                    break;
                default:
                    break;
            }

            //markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.station));
            polyline.setPoints(latLng2);
            mMap.addMarker(markerOptions);
        }
    };


    public static class SyncedMapFragment extends SupportMapFragment //自訂mapfragment功能
    {
        private MapViewWrapper mWrapper;


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            mWrapper = new MapViewWrapper(getActivity());
            mWrapper.addView(super.onCreateView(inflater, container, savedInstanceState));
            return mWrapper;
        }

        public void animateMarkerToGB(Marker marker, LatLng finalPosition, LatLngInterpolator latLngInterpolator,
                                      long duration)
        {
            if(mWrapper == null){
                throw new IllegalStateException("MapFragment view not yet created.");
            }
            mWrapper.animateMarkerToGB(marker, finalPosition, latLngInterpolator, duration);
        }


        public static class MapViewWrapper extends FrameLayout
        {
            private HashSet<MarkerAnimation> mAnimations = new HashSet<MarkerAnimation>();


            public MapViewWrapper(Context context)
            {
                super(context);
                setWillNotDraw(false);
            }

            public void animateMarkerToGB(Marker marker, LatLng finalPosition, LatLngInterpolator latLngInterpolator,
                                          long duration)
            {
                mAnimations.add(new MarkerAnimation(marker, finalPosition, latLngInterpolator, duration));
                invalidate();
            }

            @Override
            protected void onDraw(Canvas canvas)
            {
                super.onDraw(canvas);

                boolean shouldPost = false;

                Iterator<MarkerAnimation> iterator = mAnimations.iterator();
                while(iterator.hasNext()){
                    MarkerAnimation markerAnimation = iterator.next();
                    if(markerAnimation.animate()){
                        shouldPost = true;
                    }
                    else{
                        iterator.remove();
                    }
                }

                if(shouldPost){
                    postInvalidateOnAnimation();
                }
            }

            @SuppressLint("NewApi")
            public void postInvalidateOnAnimation()
            {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                    super.postInvalidateOnAnimation();
                }
                else{
                    ViewCompat.postInvalidateOnAnimation(this);
                }
            }


            private static class MarkerAnimation
            {
                final static Interpolator sInterpolator = new AccelerateDecelerateInterpolator();

                private final Marker mMarker;
                private final LatLngInterpolator mLatLngInterpolator;
                private final LatLng mStartPosition;
                private final LatLng mFinalPosition;
                private final long mDuration;
                private final long mStartTime;


                public MarkerAnimation(Marker marker, LatLng finalPosition, LatLngInterpolator latLngInterpolator,
                                       long duration)
                {
                    mMarker = marker;
                    mLatLngInterpolator = latLngInterpolator;
                    mStartPosition = marker.getPosition();
                    mFinalPosition = finalPosition;
                    mDuration = duration;
                    mStartTime = AnimationUtils.currentAnimationTimeMillis();
                }

                public boolean animate()
                {
                    // Calculate progress using interpolator
                    long elapsed = AnimationUtils.currentAnimationTimeMillis() - mStartTime;
                    float t = elapsed / (float)mDuration;
                    float v = sInterpolator.getInterpolation(t);
                    mMarker.setPosition(mLatLngInterpolator.interpolate(v, mStartPosition, mFinalPosition));

                    // Repeat till progress is complete.
                    return (t < 1);
                }

                @Override
                public int hashCode()
                {
                    // So we only get one animation for the same marker in our HashSet
                    return mMarker.hashCode();
                }

                @Override
                public boolean equals(Object o)
                {
                    if(o instanceof Marker){
                        return mMarker.equals(o);
                    }
                    return super.equals(o);
                }
            }
        }
    }



}
