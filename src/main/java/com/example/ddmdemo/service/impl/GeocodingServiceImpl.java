package com.example.ddmdemo.service.impl;

import com.example.ddmdemo.service.interfaces.GeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;

@Service
@RequiredArgsConstructor
public class GeocodingServiceImpl implements GeocodingService {
    @Override
    public double[] getCoordinates(String location) {
        try {
            String urlStr = "https://nominatim.openstreetmap.org/search?q="
                    + URLEncoder.encode(location, StandardCharsets.UTF_8)
                    + "&format=json&limit=1&accept-language=sr-Latn";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Accept", "application/json");
            conn.connect();

            StringBuilder inline = new StringBuilder();
            try (Scanner sc = new Scanner(conn.getInputStream())) {
                while (sc.hasNext()) inline.append(sc.nextLine());
            }

            JSONArray jsonArray = new JSONArray(inline.toString());
            if (jsonArray.length() > 0) {
                JSONObject locationObj = jsonArray.getJSONObject(0);
                double lat = Double.parseDouble(locationObj.getString("lat"));
                double lon = Double.parseDouble(locationObj.getString("lon"));
                return new double[]{lat, lon};
            }

            throw new RuntimeException("Nominatim returned no results for: " + location);

        } catch (IOException e) {
            throw new RuntimeException("Error calling Nominatim for: " + location, e);
        }
    }
}
