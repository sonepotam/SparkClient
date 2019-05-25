package ru.pavel2107.interfax.starter;

import org.w3c.dom.Document;
import ru.pavel2107.interfax.starter.utils.Utils;
import java.net.HttpURLConnection;
import java.io.*;
import java.nio.file.Files;

public class RequestProcessor {
    private HttpURLConnection httpConnection;
    private String actionPrefix;

    public RequestProcessor( HttpURLConnection httpConnection, String actionPrefix){
        this.httpConnection = httpConnection;
        this.actionPrefix   = actionPrefix;
    }

    private void sendRequest( String request, String soapAction) throws Exception{
        byte buffer[] = request.getBytes( "UTF-8");

        httpConnection.setRequestProperty( "Content-Length", String.valueOf( buffer.length));
        httpConnection.setRequestProperty( "SOAPAction", actionPrefix + soapAction);

        OutputStream outputStream = httpConnection.getOutputStream();
        outputStream.write( buffer);
        outputStream.close();
    }

    private String getResponse() throws Exception{
        StringBuffer stringBuffer = new StringBuffer();
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader( httpConnection.getInputStream()));

        String inputLine;
        while ( (inputLine = bufferedReader.readLine()) != null){
            stringBuffer.append( inputLine).append( "\n");
        }
        bufferedReader.close();

        return stringBuffer.toString();
    }

    public String exchange( String filename) throws Exception{
        //
        // читаем файл в строку
        //
        File file = new File( filename);
        String fileContent = new String( Files.readAllBytes( file.toPath()), "UTF-8");
        //
        // строим документ
        //
        Document document  = Utils.buildDocument( fileContent);
        //
        // получаем действие
        //
        String action      = Utils.extractAction( document);
        //
        //  отправляем запрос
        //
        sendRequest( fileContent, action);
        //
        // получаем ответ
        //
        String result = getResponse();
        //
        // закрываем соединение. все равно больше обмен нельзя сделать
        //
        httpConnection.disconnect();

        return result;
    }

}
