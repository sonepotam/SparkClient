package ru.pavel2107.interfax.starter.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Utils {

    public static String getStringFromDocument(Document doc) throws TransformerException {
        DOMSource domSource = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(domSource, result);
        return writer.toString();
    }


    public static String prettyPrint(Document xml) throws Exception {
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        Writer out = new StringWriter();
        tf.transform(new DOMSource(xml), new StreamResult(out));
        return out.toString();
    }


    public static Document buildDocument( String str)throws Exception{
        //
        // готовимся к разбору документа
        //
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware( true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        //
        // читаем в dom
        //
        Document document = builder.parse( new ByteArrayInputStream( str.getBytes("UTF-8")));
        document.getDocumentElement().normalize();

        return document;
    }

    public static String extractAction( Document document){
        String result = null;
        //
        // ищем тело сообщения
        //
        NodeList list = document.getDocumentElement().getElementsByTagName( "soap:Body");
        Node body = list.item( 0);
        //
        // определяем действие
        //
        list = body.getChildNodes();
        for( int i = 0; i < list.getLength(); i++){
            Node node = list.item( i);
            if( node.getNodeType() == Node.ELEMENT_NODE) {
                result= node.getNodeName();
                break;
            }
        }
        return result;
    }

    public static void moveTo(File file, String dest) throws IOException{
        File newFile = new File( dest + "\\" + file.getName());
        Files.move( file.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING );
    }
}
