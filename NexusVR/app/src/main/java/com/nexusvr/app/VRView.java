package com.nexusvr.app;

import android.graphics.*;
import android.os.SystemClock;
import android.view.*;
import java.util.*;

final class VRView extends View {
    final MainActivity app; final HeadPose pose; final Paint p=new Paint(3); final Bitmap panel=Bitmap.createBitmap(1000,640,Bitmap.Config.ARGB_8888); final Canvas pc=new Canvas(panel);
    final Bitmap movie=Bitmap.createBitmap(640,360,Bitmap.Config.ARGB_8888); final ArrayList<Hit> hits=new ArrayList<>();
    final Matrix centerMatrix=new Matrix(), inverse=new Matrix(); boolean validInverse=false;
    final int white=0xffeff5ff, muted=0xffa0b1cb, cyan=0xff78e6f3, navy=0xff111c30;
    String screen="home", notice="Bienvenido a Nexus VR", mediaTitle="Sin reproducción"; long noticeUntil=Long.MAX_VALUE;
    List<Playlist.Channel> channels=new ArrayList<>(); int page=0, theme=0;
    volatile boolean passthrough=false; boolean active=true, suspendInput=false, dwell=true, stereo=true, showHands=true;
    Bitmap cameraFrame; List<float[][]> hands=Collections.emptyList(); long lastHand=0;
    boolean pinched=false, pendingPinch=false, dragging=false, scaling=false; float pointerX=.5f,pointerY=.5f, prevX,prevY,scaleStart,spanStart;
    float panelX=0,panelY=.03f, panelScale=1, ipd=.064f; long hoverSince=0, lastClick=0, lastBrowser=0; String hoverId="", latched=""; Bitmap browser;
    float[] orientation={1,0,0,0,1,0,0,0,1};
    static final int[][] BONES={{0,1},{1,2},{2,3},{3,4},{0,5},{5,6},{6,7},{7,8},{5,9},{9,10},{10,11},{11,12},{9,13},{13,14},{14,15},{15,16},{13,17},{17,18},{18,19},{19,20},{17,0}};
    static final class Hit { final String id; final RectF rect; final Runnable action; Hit(String id,RectF r,Runnable a){this.id=id;rect=r;action=a;} }
    VRView(MainActivity a,HeadPose pose){super(a);app=a;this.pose=pose;setFocusable(true);setLayerType(View.LAYER_TYPE_HARDWARE,null);}
    void message(String s){notice=s;noticeUntil=SystemClock.uptimeMillis()+4500;invalidate();}
    void setCameraFrame(Bitmap b){cameraFrame=b;invalidate();}
    void updateHands(List<float[][]> data){hands=data;lastHand=SystemClock.uptimeMillis();invalidate();}
    void go(String s){screen=s;hoverId="";latched="";hoverSince=0;}
    void reset(){pose.recenter();panelX=0;panelY=.03f;dragging=false;scaling=false;message("Vista centrada");}
    float dist(float[] a,float[] b){return (float)Math.hypot(a[0]-b[0],a[1]-b[1]);}
    boolean pinch(float[][] h,boolean prior){float ref=Math.max(.025f,dist(h[0],h[9]));return dist(h[4],h[8])/ref < (prior?.42f:.27f);}
    void drawText(Canvas c,String s,float x,float y,float sz,int color,boolean bold){p.reset();p.setAntiAlias(true);p.setColor(color);p.setTextSize(sz);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));c.drawText(s,x,y,p);}
    void round(Canvas c,RectF r,float rad,int col){p.reset();p.setAntiAlias(true);p.setColor(col);c.drawRoundRect(r,rad,rad,p);}
    void line(Canvas c,float x,float y,float a,float b,int col,float width){p.reset();p.setAntiAlias(true);p.setColor(col);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,a,b,p);}
    String cut(String s,int max){return s.length()>max?s.substring(0,max-1)+"…":s;}
    void button(String id,String text,float x,float y,float w,float h,Runnable action){RectF r=new RectF(x,y,x+w,y+h);round(pc,r,15,id.equals(hoverId)?0xff28566d:0xff23334c);drawText(pc,text,x+18,y+h*.60f+5,25,white,true);hits.add(new Hit(id,r,action));}
    void tile(String id,String icon,String title,String sub,float x,float y,int color,Runnable action){RectF r=new RectF(x,y,x+282,y+142);round(pc,r,22,id.equals(hoverId)?0xff315873:0xff203047);round(pc,new RectF(x+18,y+17,x+76,y+75),17,color);drawText(pc,icon,x+29,y+57,27,0xff101827,true);drawText(pc,title,x+18,y+108,28,white,true);drawText(pc,sub,x+18,y+132,17,muted,false);hits.add(new Hit(id,r,action));}
    void title(String s,String sub){drawText(pc,s,45,155,42,white,true);drawText(pc,sub,46,188,22,muted,false);}
    void buildPanel(long now){hits.clear();pc.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);round(pc,new RectF(2,2,998,638),32,0xf2152035);round(pc,new RectF(10,10,990,630),27,0xf0141f33);
        drawText(pc,"NEXUS / VR",43,58,28,cyan,true);drawText(pc,new java.text.SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date()),850,58,25,muted,false);line(pc,43,78,957,78,0xff34445d,2);
        hits.add(new Hit("drag",new RectF(35,15,795,76),()->{}));
        if(screen.equals("home")){title("Tu espacio virtual","Selecciona con pulgar + índice o mantén la mirada.");
            tile("web","W","Navegador","Páginas HTTPS",43,217,0xff82d8f4,()->go("web"));tile("media","▶","Multimedia","Videos y música",359,217,0xffb7a8fb,()->go("media"));tile("iptv","TV","IPTV","Listas M3U",675,217,0xff91e6c1,()->go("iptv"));
            tile("files","+","Archivos","Elegir contenido",43,379,0xfff5ca86,()->{go("media");app.chooseFile();});tile("world","✦","Entornos","Personaliza tu mundo",359,379,0xffa6baff,()->go("world"));tile("settings","⚙","Ajustes","Visor y gestos",675,379,0xff9fe3ed,()->go("settings"));
        }else if(screen.equals("settings")){title("Configuración","Ajusta el visor antes de colocar el teléfono.");
            button("gaze","Mirada: "+(dwell?"SÍ":"NO"),44,214,285,66,()->{dwell=!dwell;});button("hands","Manos: "+(showHands?"SÍ":"NO"),357,214,285,66,()->showHands=!showHands);button("stereo","Vista: "+(stereo?"VR":"ÚNICA"),670,214,285,66,()->stereo=!stereo);
            button("small","Ventana −",44,299,285,66,()->panelScale=Math.max(.65f,panelScale-.1f));button("large","Ventana +",357,299,285,66,()->panelScale=Math.min(1.25f,panelScale+.1f));button("recenter","Centrar en 5 s",670,299,285,66,()->{message("Mira al frente; centrando en 5 segundos");postDelayed(this::reset,5000);});
            button("ipdMinus","IPD −",44,384,190,62,()->ipd=Math.max(.05f,ipd-.002f));drawText(pc,Math.round(ipd*1000)+" mm",265,425,28,white,true);button("ipdPlus","IPD +",430,384,190,62,()->ipd=Math.min(.075f,ipd+.002f));
            drawText(pc,"IPD virtual; no reemplaza la calibración de lentes.",44,492,22,muted,false);
        }else if(screen.equals("world")){title("Entornos","La cámara es una vista monocular, no realidad mixta calibrada.");button("space","Órbita / azul",44,230,425,85,()->{theme=0;passthrough=false;});button("sun","Atardecer",530,230,425,85,()->{theme=1;passthrough=false;});button("forest","Bosque / verde",44,340,425,85,()->{theme=2;passthrough=false;});button("camera","Cámara / experimental",530,340,425,85,()->passthrough=!passthrough);drawText(pc,"Usa el visor sentado y con la cámara libre.",44,478,23,muted,false);
        }else if(screen.equals("web")){drawText(pc,"Navegador",44,128,36,white,true);button("url","URL",650,91,130,52,()->app.urlDialog("web"));button("back","Atrás",795,91,160,52,app::browserBack);
            if(now-lastBrowser>180||browser==null){browser=app.browserFrame();lastBrowser=now;}
            p.reset();p.setFilterBitmap(true);pc.drawBitmap(browser,null,new RectF(44,153,956,520),p);hits.add(new Hit("webContent",new RectF(44,153,956,520),()->{}));button("up","Subir",44,529,180,47,()->app.browserScroll(-300));button("down","Bajar",240,529,180,47,()->app.browserScroll(300));
        }else if(screen.equals("media")){drawText(pc,cut(mediaTitle,42),44,130,34,white,true);button("mediaUrl","URL",810,91,145,53,()->app.urlDialog("media"));p.reset();p.setFilterBitmap(true);pc.drawBitmap(app.videoFrame(movie),null,new RectF(190,150,810,499),p);button("open","Archivo",44,514,220,53,app::chooseFile);button("play","Pausa / Play",280,514,275,53,app::togglePlay);button("stop","Detener",575,514,220,53,app::stopVideo);
        }else if(screen.equals("iptv")){drawText(pc,"IPTV  /  "+channels.size()+" canales",44,132,35,white,true);button("listfile","Archivo",630,91,155,52,app::chooseList);button("listurl","URL",800,91,155,52,()->app.urlDialog("list"));
            for(int j=0;j<5;j++){int idx=page*5+j;if(idx>=channels.size())break;Playlist.Channel ch=channels.get(idx);button("channel"+idx,cut(ch.name,55),44,160+j*70,912,60,()->app.play(ch.url,ch.name));}
            if(channels.isEmpty())drawText(pc,"Importa una lista M3U propia o autorizada.",44,270,27,muted,false);
            button("prev","Anterior",44,520,200,50,()->page=Math.max(0,page-1));drawText(pc,(page+1)+" / "+Math.max(1,(channels.size()+4)/5),430,555,25,muted,true);button("next","Siguiente",755,520,200,50,()->page=Math.min(Math.max(0,(channels.size()-1)/5),page+1));
        }
        if(!screen.equals("home"))button("home","Inicio",43,582,142,43,()->go("home"));
        String status=now<noticeUntil?notice:((now-lastHand<600&&!hands.isEmpty())?"Hand Tracking activo":(pose.available()?"Mira un botón o muestra tu mano":"Sin sensor de orientación"));drawText(pc,cut(status,58),screen.equals("home")?44:205,610,21,muted,false);
    }
    float[] project(float x,float y,float z,float eye,float w,float h){float[] m=orientation;float vx=m[0]*x+m[1]*y+m[2]*z-eye,vy=m[3]*x+m[4]*y+m[5]*z,vz=m[6]*x+m[7]*y+m[8]*z;if(vz>-.12f)return null;float f=Math.min(w*.95f,h*.82f);return new float[]{w/2+f*vx/-vz,h/2-f*vy/-vz};}
    Matrix panelMatrix(float eye,float w,float h){float halfX=1.43f*panelScale,halfY=.9152f*panelScale;float[] a=project(panelX-halfX,panelY+halfY,-3,eye,w,h),b=project(panelX+halfX,panelY+halfY,-3,eye,w,h),c=project(panelX+halfX,panelY-halfY,-3,eye,w,h),d=project(panelX-halfX,panelY-halfY,-3,eye,w,h);if(a==null||b==null||c==null||d==null)return null;Matrix m=new Matrix();if(!m.setPolyToPoly(new float[]{0,0,1000,0,1000,640,0,640},0,new float[]{a[0],a[1],b[0],b[1],c[0],c[1],d[0],d[1]},0,4))return null;return m;}
    void background(Canvas c,float eye,float w,float h){if(passthrough&&cameraFrame!=null){p.reset();p.setFilterBitmap(true);c.drawBitmap(cameraFrame,null,new RectF(0,0,w,h),p);c.drawColor(0x28000000);return;}
        int top=theme==1?0xff301d43:theme==2?0xff102d31:0xff080e24, bottom=theme==1?0xffa94f4f:theme==2?0xff306454:0xff273d70; p.reset();p.setShader(new LinearGradient(0,0,0,h,top,bottom,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);Random r=new Random(9);for(int i=0;i<100;i++){float angle=r.nextFloat()*6.283f, y=r.nextFloat()*8-1;float[] q=project((float)Math.sin(angle)*15,y,(float)Math.cos(angle)*15,eye,w,h);if(q!=null){p.setColor(0xffb6c9e9);p.setAlpha(70+i%140);c.drawCircle(q[0],q[1],1+i%3*.5f,p);}}p.setAlpha(255);
        for(int x=-10;x<=10;x++){float[] a=project(x,-1.3f,-.7f,eye,w,h),b=project(x,-1.3f,-19,eye,w,h);if(a!=null&&b!=null)line(c,a[0],a[1],b[0],b[1],0x344c8baf,1);}
        for(int z=-1;z>-20;z--){float[] a=project(-10,-1.3f,z,eye,w,h),b=project(10,-1.3f,z,eye,w,h);if(a!=null&&b!=null)line(c,a[0],a[1],b[0],b[1],0x344c8baf,1);}
    }
    float sx(float norm,float w){return w*(.5f+(norm-.5f)*1.15f);}float sy(float norm,float h){return h*(.5f+(norm-.5f)*.9f);}
    void eye(Canvas c,float x,float w,float h,float eye,long now,boolean hand){c.save();c.translate(x,0);c.clipRect(0,0,w,h);background(c,eye,w,h);Matrix m=panelMatrix(eye,w,h);if(m!=null){p.reset();p.setAntiAlias(true);p.setFilterBitmap(true);c.drawBitmap(panel,m,p);}
        if(hand&&showHands){for(float[][] a:hands){for(int[] b:BONES)line(c,sx(a[b[0]][0],w),sy(a[b[0]][1],h),sx(a[b[1]][0],w),sy(a[b[1]][1],h),0x9a78e6f3,2);for(float[] q:a){p.reset();p.setAntiAlias(true);p.setColor(0xbbd2faff);c.drawCircle(sx(q[0],w),sy(q[1],h),2.7f,p);}}}
        float px=hand?sx(pointerX,w):w/2,py=hand?sy(pointerY,h):h/2;p.reset();p.setAntiAlias(true);p.setColor(pinched?0xffa2ffc8:Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.3f);c.drawCircle(px,py,8,p);if(!hand&&dwell&&!hoverId.isEmpty()&&!latched.equals(hoverId)){p.setColor(cyan);p.setStrokeWidth(3);c.drawArc(px-13,py-13,px+13,py+13,-90,Math.min(360,(now-hoverSince)*360f/1400),false,p);}
        round(c,new RectF(w*.35f,h-35,w*.65f,h-8),12,0xb0132030);drawText(c,"MANTÉN PARA CENTRAR",w*.36f,h-17,Math.max(8,w*.018f),white,true);c.restore();}
    Hit find(float x,float y){if(!validInverse)return null;float[] q={x,y};inverse.mapPoints(q);for(Hit hit:hits)if(hit.rect.contains(q[0],q[1]))return hit;return null;}
    void activate(Hit hit,float x,float y,long now){if(hit==null||suspendInput||now-lastClick<350)return;lastClick=now;if(hit.id.equals("webContent")){float[] q={x,y};inverse.mapPoints(q);app.browserTap((q[0]-44)*900/912,(q[1]-153)*440/367);}else if(!hit.id.equals("drag"))hit.action.run();}
    void input(float w,float h,long now,boolean hand){if(suspendInput)return;boolean next=false,both=false;float span=0;if(hand){float[][] a=hands.get(0);pointerX=pointerX*.65f+a[8][0]*.35f;pointerY=pointerY*.65f+a[8][1]*.35f;next=pinch(a,pinched);if(hands.size()>1){float[][] b=hands.get(1);both=next&&pinch(b,scaling);span=dist(a[8],b[8]);}}
        float x=hand?sx(pointerX,w):w/2,y=hand?sy(pointerY,h):h/2;Hit hit=find(x,y);String id=hit==null?"":hit.id;if(!id.equals(hoverId)){hoverId=id;hoverSince=now;if(!id.equals(latched))latched="";}
        if(hand){if(both){if(!scaling){scaling=true;spanStart=Math.max(.04f,span);scaleStart=panelScale;}panelScale=Math.max(.65f,Math.min(1.25f,scaleStart*span/spanStart));dragging=false;}else{if(scaling)scaling=false;if(next&&!pinched){if("drag".equals(id)){dragging=true;prevX=x;prevY=y;}else activate(hit,x,y,now);}if(next&&dragging){float f=Math.min(w*.95f,h*.82f);float dx=(x-prevX)*3/f,dy=-(y-prevY)*3/f;panelX=Math.max(-2,Math.min(2,panelX+orientation[0]*dx+orientation[3]*dy));panelY=Math.max(-1,Math.min(1,panelY+orientation[1]*dx+orientation[4]*dy));prevX=x;prevY=y;}if(!next)dragging=false;}}
        else{dragging=false;scaling=false;if(dwell&&!id.isEmpty()&&!id.equals("drag")&&!id.equals("webContent")&&!id.equals(latched)&&now-hoverSince>=1400){latched=id;activate(hit,x,y,now);}}
        pinched=next;
    }
    @Override protected void onDraw(Canvas c){super.onDraw(c);long now=SystemClock.uptimeMillis();float w=stereo?getWidth()/2f:getWidth(),h=getHeight();if(w<=0||h<=0)return;orientation=pose.relative();Matrix m=panelMatrix(0,w,h);validInverse=m!=null&&m.invert(inverse);buildPanel(now);boolean hand=now-lastHand<600&&!hands.isEmpty();if(active)input(w,h,now,hand);if(stereo){eye(c,0,w,h,-ipd/2,now,hand);eye(c,w,w,h,ipd/2,now,hand);line(c,w,0,w,h,0xff05070d,2);}else eye(c,0,w,h,0,now,hand);if(active)postInvalidateOnAnimation();}
    long downTime;float downX,downY;
    @Override public boolean onTouchEvent(android.view.MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){downTime=SystemClock.uptimeMillis();downX=e.getX();downY=e.getY();return true;}if(e.getAction()==MotionEvent.ACTION_UP){long now=SystemClock.uptimeMillis();if(now-downTime>650||downY>getHeight()-42){reset();return true;}float w=stereo?getWidth()/2f:getWidth();float x=e.getX()%w,y=e.getY();activate(find(x,y),x,y,now);performClick();return true;}return true;}
    @Override public boolean performClick(){super.performClick();return true;}
}
