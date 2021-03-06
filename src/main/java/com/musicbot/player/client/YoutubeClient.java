package com.musicbot.player.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class YoutubeClient {
    private final String KEY = "AIzaSyCaXRXVBL3V9nc9Tl1SlV3IxzwwRZwNpuA";
    private final String BASE_URL = "https://www.googleapis.com/youtube/v3/search?type=video&part=snippet&maxResults=10";

    private URL getRequestURL(String keyword) {
        URL url = null;
        try {
            String builder = new StringBuilder()
                    .append(BASE_URL)
                    .append("&key=" + KEY)
                    .append("&q=" + URLEncoder.encode(keyword, "UTF-8"))
                    .toString();

            url = new URL(builder);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }

    public JsonNode search(String keyword) {
        URL url = this.getRequestURL(keyword);
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String line = "";
            StringBuilder sb = new StringBuilder();

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            System.out.println(sb.toString());

            return new ObjectMapper().readTree(sb.toString()).get("items");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
