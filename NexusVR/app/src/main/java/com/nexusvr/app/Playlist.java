package com.nexusvr.app;
import java.io.*;
import java.util.*;
final class Playlist {
    static final class Channel { final String name,url; Channel(String n,String u){name=n;url=u;} }
    static List<Channel> parse(InputStream input) throws IOException {
        List<Channel> out=new ArrayList<>(); String pending=null;
        try(BufferedReader r=new BufferedReader(new InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8))){ String l; while((l=r.readLine())!=null){l=l.trim(); if(l.startsWith("\uFEFF")) l=l.substring(1); if(l.startsWith("#EXTINF:")){ int comma=l.lastIndexOf(','); pending=comma<0?"Canal":l.substring(comma+1).trim(); } else if(!l.isEmpty()&&!l.startsWith("#")){ if(l.startsWith("https://")||l.startsWith("http://")) out.add(new Channel(pending==null||pending.isEmpty()?"Canal "+(out.size()+1):pending,l)); pending=null; if(out.size()>=50000)break; } } }
        return out;
    }
}
