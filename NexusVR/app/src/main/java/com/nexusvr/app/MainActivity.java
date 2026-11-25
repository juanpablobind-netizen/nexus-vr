package com.nexusvr.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.*;
import java.util.concurrent.*;

@androidx.media3.common.util.UnstableApi
public class MainActivity extends ComponentActivity implements HandTracker.Sink {
    private final ExecutorService cameraThread=Executors.newSingleThreadExecutor(), io=Executors.newSingleThreadExecutor();
    private HandTracker tracker; private ProcessCameraProvider provider; private VRView vr; private HeadPose pose;
    private WebView web; private TextureView video; private ExoPlayer player; private FrameLayout root;
    private ActivityResultLauncher<String> permission; private ActivityResultLauncher<String[]> picker, listPicker;
    private final Bitmap webBitmap=Bitmap.createBitmap(900,440,Bitmap.Config.ARGB_8888);
    private volatile boolean dead=false;
    @Override public void onCreate(Bundle state){super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); immersive();
        pose=new HeadPose(this); root=new FrameLayout(this);
        web=new WebView(this); web.setBackgroundColor(Color.WHITE); web.setLayerType(View.LAYER_TYPE_SOFTWARE,null); web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){String s=r.getUrl().getScheme();return !"https".equals(s);} });
        web.getSettings().setJavaScriptEnabled(true); web.getSettings().setDomStorageEnabled(true); web.getSettings().setAllowFileAccess(false); web.getSettings().setAllowContentAccess(false); web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        root.addView(web,new FrameLayout.LayoutParams(900,440)); web.loadDataWithBaseURL(null,"<html><body style='font-family:sans-serif;padding:40px'><h1>Nexus Browser</h1><p>Abre una dirección HTTPS desde el botón URL.</p></body></html>","text/html","UTF-8",null);
        video=new TextureView(this); root.addView(video,new FrameLayout.LayoutParams(640,360));
        player=new ExoPlayer.Builder(this).build(); player.setVideoTextureView(video); player.addListener(new Player.Listener(){@Override public void onPlayerError(PlaybackException e){if(vr!=null)vr.message("Video: "+e.getErrorCodeName());}});
        vr=new VRView(this,pose); root.addView(vr,new FrameLayout.LayoutParams(-1,-1)); setContentView(root);
        permission=registerForActivityResult(new ActivityResultContracts.RequestPermission(),ok->{if(ok)startCamera();else vr.message("Cámara denegada. Usa mirada o toque.");});
        picker=registerForActivityResult(new ActivityResultContracts.OpenDocument(),u->{if(u!=null)play(u.toString(),"Archivo local"); immersive();});
        listPicker=registerForActivityResult(new ActivityResultContracts.OpenDocument(),u->{if(u!=null)importList(u); immersive();});
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)startCamera();else permission.launch(Manifest.permission.CAMERA);
    }
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(5894|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);}
    @Override public void onWindowFocusChanged(boolean f){super.onWindowFocusChanged(f);if(f)immersive();}
    private void startCamera(){if(tracker!=null)return; vr.message("Iniciando cámara..."); cameraThread.execute(()->{HandTracker t=new HandTracker(this,this);runOnUiThread(()->{if(dead){t.close();return;} tracker=t; bindCamera();});});}
    private void bindCamera(){ListenableFuture<ProcessCameraProvider> f=ProcessCameraProvider.getInstance(this);f.addListener(()->{if(dead)return;try{provider=f.get(); ImageAnalysis analysis=new ImageAnalysis.Builder().setTargetResolution(new android.util.Size(640,480)).setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build(); analysis.setAnalyzer(cameraThread,tracker::analyze); provider.unbindAll(); provider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,analysis); vr.message("Cámara trasera activa");}catch(Exception e){vr.message("Cámara no disponible; usa mirada.");}},ContextCompat.getMainExecutor(this));}
    @Override public void hands(List<float[][]> h){if(!dead)runOnUiThread(()->{if(!dead)vr.updateHands(h);});}
    @Override public void frame(Bitmap b){if(dead){b.recycle();return;}runOnUiThread(()->{if(dead)b.recycle();else vr.setCameraFrame(b);});}
    @Override public void error(String s){if(!dead)runOnUiThread(()->{if(!dead)vr.message(s);});}
    @Override public boolean wantsFrame(){return !dead&&vr!=null&&vr.passthrough;}
    Bitmap browserFrame(){Canvas c=new Canvas(webBitmap);c.drawColor(Color.WHITE);web.draw(c);return webBitmap;}
    void browserTap(float x,float y){long t=SystemClock.uptimeMillis(); MotionEvent a=MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,x,y,0),b=MotionEvent.obtain(t,t+15,MotionEvent.ACTION_UP,x,y,0);web.dispatchTouchEvent(a);web.dispatchTouchEvent(b);a.recycle();b.recycle();}
    void browserScroll(int dy){web.scrollBy(0,dy);}
    void browserBack(){if(web.canGoBack())web.goBack();}
    Bitmap videoFrame(Bitmap reuse){if(!video.isAvailable())return reuse;try{return video.getBitmap(reuse);}catch(Exception e){return reuse;}}
    void chooseFile(){picker.launch(new String[]{"video/*","audio/*"});}
    void chooseList(){listPicker.launch(new String[]{"*/*"});}
    void urlDialog(String type){vr.suspendInput=true; EditText field=new EditText(this);field.setSingleLine(true);field.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);field.setHint("https://..."); AlertDialog d=new AlertDialog.Builder(this).setTitle(type.equals("web")?"Dirección web":type.equals("list")?"URL de lista M3U":"URL de video o audio").setView(field).setPositiveButton("Abrir",(a,b)->{String s=field.getText().toString().trim();if(!s.startsWith("https://")){vr.message("Solo direcciones HTTPS por seguridad.");return;} if(type.equals("web"))web.loadUrl(s);else if(type.equals("list"))importList(Uri.parse(s));else play(s,"Streaming");}).setNegativeButton("Cancelar",null).create();d.setOnDismissListener(v->{vr.suspendInput=false; immersive();});d.show();}
    void importList(Uri u){vr.message("Leyendo lista...");io.execute(()->{try{List<Playlist.Channel> list;if("https".equals(u.getScheme())){java.net.URLConnection c=new java.net.URL(u.toString()).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(15000);list=Playlist.parse(c.getInputStream());}else{try(java.io.InputStream in=getContentResolver().openInputStream(u)){if(in==null)throw new java.io.IOException();list=Playlist.parse(in);}}runOnUiThread(()->{if(!dead){vr.channels=list;vr.page=0;vr.message(list.size()+" canales importados");}});}catch(Exception e){runOnUiThread(()->{if(!dead)vr.message("No se pudo leer la lista.");});}});}
    void play(String url,String name){if(url.startsWith("http://")){vr.message("Canal HTTP bloqueado; usa HTTPS.");return;}try{player.setMediaItem(MediaItem.fromUri(url));player.prepare();player.play();vr.mediaTitle=name;vr.screen="media";}catch(Exception e){vr.message("No se pudo abrir el archivo.");}}
    void togglePlay(){if(player.isPlaying())player.pause();else player.play();}
    void stopVideo(){player.stop();}
    @Override protected void onResume(){super.onResume();if(pose!=null)pose.start();if(web!=null)web.onResume();if(vr!=null){vr.active=true;vr.invalidate();}}
    @Override protected void onPause(){if(vr!=null)vr.active=false;if(pose!=null)pose.stop();if(player!=null)player.pause();if(web!=null)web.onPause();super.onPause();}
    @Override protected void onDestroy(){dead=true;if(provider!=null)provider.unbindAll();if(tracker!=null)cameraThread.execute(tracker::close);cameraThread.shutdown();io.shutdownNow();if(player!=null)player.release();if(web!=null){root.removeView(web);web.destroy();}super.onDestroy();}
}
