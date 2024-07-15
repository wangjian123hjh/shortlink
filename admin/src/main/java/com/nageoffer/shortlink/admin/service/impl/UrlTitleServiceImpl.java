package com.nageoffer.shortlink.admin.service.impl;

import com.nageoffer.shortlink.admin.service.UrlTitleService;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class UrlTitleServiceImpl implements UrlTitleService {

    @Override
    @SneakyThrows
    public String getTitleByUrl(String urlTit) {
        URL url = new URL(urlTit);
        HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.connect();
        int responseCode = urlConnection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK){
            Document document = Jsoup.connect(urlTit).get();
            return document.title();
        }else{
            return "Error while fetching title. HTTP response code: "+responseCode;
        }
    }
}
