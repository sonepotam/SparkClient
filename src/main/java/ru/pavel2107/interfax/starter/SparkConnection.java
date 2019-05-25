package ru.pavel2107.interfax.starter;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class SparkConnection {
    private final String soap_url;
    private HttpURLConnection httpConnection;

    public SparkConnection( String soap_url){
        this.soap_url = soap_url;
    }

    public HttpURLConnection getConnection() {
        return httpConnection;
    }

    public void configureConnection() throws Exception{
        URL url = new URL( soap_url);
        URLConnection connection = url.openConnection();
        httpConnection = (HttpURLConnection)connection;

        connection.setRequestProperty( "Host",  url.getHost());
        connection.setRequestProperty( "Content-Type", "text/xml; charset=utf-8");

        httpConnection.setRequestMethod( "POST");
        httpConnection.setDoOutput( true);
        httpConnection.setDoInput(  true);
    }
}
