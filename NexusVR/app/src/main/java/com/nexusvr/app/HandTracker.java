package com.nexusvr.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.SystemClock;
import androidx.camera.core.ImageProxy;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;

final class HandTracker implements AutoCloseable {
    interface Sink { void hands(List<float[][]> points); void frame(Bitmap bitmap); void error(String message); boolean wantsFrame(); }
    private final Sink sink;
    private HandLandmarker model;
    private long last;
    private boolean closed;
    HandTracker(Context context, Sink sink) {
        this.sink=sink;
        try {
            model=HandLandmarker.createFromOptions(context, HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build())
                .setRunningMode(RunningMode.VIDEO).setNumHands(2).setMinHandDetectionConfidence(.55f)
                .setMinHandPresenceConfidence(.55f).setMinTrackingConfidence(.55f).build());
        } catch(Exception e) { sink.error("Modelo no disponible: "+e.getClass().getSimpleName()); }
    }
    synchronized void analyze(ImageProxy image) {
        Bitmap raw=null, upright=null;
        try {
            if(closed) return;
            if (model==null && !sink.wantsFrame()) return;
            long t=SystemClock.uptimeMillis(); if(t-last<45) return; last=t;
            raw=image.toBitmap(); int w=raw.getWidth(), h=raw.getHeight();
            Matrix m=new Matrix(); m.postRotate(image.getImageInfo().getRotationDegrees());
            upright=Bitmap.createBitmap(raw,0,0,w,h,m,true);
            if(sink.wantsFrame()) sink.frame(upright.copy(Bitmap.Config.ARGB_8888,false));
            if(model==null) return;
            MPImage mp=new BitmapImageBuilder(upright).build();
            try {
                List<float[][]> hands=new ArrayList<>();
                for(List<NormalizedLandmark> hand:model.detectForVideo(mp,t).landmarks()) {
                    float[][] data=new float[hand.size()][3];
                    for(int i=0;i<hand.size();i++){ NormalizedLandmark l=hand.get(i); data[i][0]=l.x(); data[i][1]=l.y(); data[i][2]=l.z(); }
                    if(data.length==21) hands.add(data);
                }
                sink.hands(hands);
            } finally { mp.close(); }
        } catch(Exception e) { sink.error("Error de seguimiento: "+e.getClass().getSimpleName()); }
        finally { image.close(); if(upright!=null && upright!=raw && !upright.isRecycled()) upright.recycle(); if(raw!=null && !raw.isRecycled()) raw.recycle(); }
    }
    @Override public synchronized void close(){ closed=true; if(model!=null){model.close();model=null;} }
}
