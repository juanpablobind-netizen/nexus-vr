package com.nexusvr.app;
import java.io.*;
class PlaylistTest { public static void main(String[] args)throws Exception {StringBuilder s=new StringBuilder("\uFEFF#EXTM3U\n"); for(int i=0;i<6000;i++)s.append("#EXTINF:-1,Canal ").append(i).append("\n#EXTVLCOPT:foo\nhttps://example.com/").append(i).append(".m3u8\n"); var result=Playlist.parse(new ByteArrayInputStream(s.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))); if(result.size()!=6000||!result.get(5999).name.equals("Canal 5999"))throw new AssertionError();System.out.println("PASS: 6000 canales y nombre final correcto"); } }
