package com.nexusvr.app;
import android.content.Context;
import android.hardware.*;
import android.view.Surface;
import java.util.Arrays;
final class HeadPose implements SensorEventListener {
    private final SensorManager manager; private final Sensor sensor; private final MainActivity activity;
    private float[] current={1,0,0,0,1,0,0,0,1}, center=current.clone(); private boolean calibrated=false;
    HeadPose(MainActivity a){ activity=a; manager=(SensorManager)a.getSystemService(Context.SENSOR_SERVICE); Sensor s=manager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR); sensor=s!=null?s:manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR); }
    boolean available(){return sensor!=null;}
    void start(){if(sensor!=null) manager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_GAME);}
    void stop(){manager.unregisterListener(this);}
    synchronized void recenter(){center=current.clone();calibrated=true;}
    @Override public synchronized void onSensorChanged(SensorEvent e){float[] raw=new float[9], mapped=new float[9]; SensorManager.getRotationMatrixFromVector(raw,e.values); int r=activity.getWindowManager().getDefaultDisplay().getRotation(); int x=SensorManager.AXIS_X,y=SensorManager.AXIS_Y;
        if(r==Surface.ROTATION_90){x=SensorManager.AXIS_Y;y=SensorManager.AXIS_MINUS_X;} else if(r==Surface.ROTATION_270){x=SensorManager.AXIS_MINUS_Y;y=SensorManager.AXIS_X;} else if(r==Surface.ROTATION_180){x=SensorManager.AXIS_MINUS_X;y=SensorManager.AXIS_MINUS_Y;}
        SensorManager.remapCoordinateSystem(raw,x,y,mapped); current=mapped; if(!calibrated) recenter(); }
    synchronized float[] relative(){float[] out=new float[9]; for(int i=0;i<3;i++)for(int j=0;j<3;j++)for(int k=0;k<3;k++)out[i*3+j]+=current[k*3+i]*center[k*3+j]; return out;}
    @Override public void onAccuracyChanged(Sensor s,int a){}
}
