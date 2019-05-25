package ru.pavel2107.interfax.starter.utils;


import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Properties;

public class PasswordConverter {
    //
    // алгоритм
    //
    private static String algorithm = "AES";
    //
    // секретный ключ
    //
    private static byte[] keyValue=new byte[] {'v','a','l','e','n','k','i','-','e','t','p','1','3','5','6','7'};// your key


    public static String convert( URL url, String passwordProperty, String md5Property){
        String password = "";
        try{
            //
            // читаем файл свойств
            //
            InputStream is = url.openStream();
            Properties properties = new Properties();
            properties.load(is);
            //
            // читаем файл как строку
            //
            Path path = Paths.get( url.toURI());
            String content = new String(Files.readAllBytes(path), "UTF-8");
            //
            // находим пароль
            //
            password = properties.getProperty(passwordProperty);
            //
            // находим md5 файла
            //
            String md5 = properties.getProperty(md5Property)  ;
            md5 = md5 == null ? "" : md5;
            //
            // рассчитываем хеш без учета этих свойств
            //
            content = content.replace(passwordProperty + "=" + password, passwordProperty + "=");
            content = content.replace(md5Property + "=" + md5, md5Property + "=");
            //
            // подсчитаем MD5 файла
            //
            byte[] bytesOfMessage = content.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytesOfMessage);
            String realMD5 = Base64.getEncoder().encodeToString( digest);
            //
            // если MD5 изменился, то запишем зашифрованный пароль и новый md5
            // если MD5 совпадает, то файл не изменялся
            //
            if(  !md5.equals( realMD5)){
                password = encrypt( password);
                content = content.replace(passwordProperty + "=", passwordProperty + "=" + password);
                content = content.replace(md5Property + "=", md5Property + "=" + realMD5);
                Files.write( path, content.getBytes( "UTF-8"));
            }
            //
            // расшифруем пароль и отдадим его программе
            //
            password = decrypt( password);
        }
        catch ( Exception e){

        }
        return password;
    }

    public static String encrypt(String plainText) throws Exception
    {
        Key key = generateKey();
        Cipher chiper = Cipher.getInstance(algorithm);
        chiper.init(Cipher.ENCRYPT_MODE, key);
        byte[] encVal = chiper.doFinal(plainText.getBytes());
        String encryptedValue = new BASE64Encoder().encode(encVal);
        return encryptedValue;
    }

    // Performs decryption
    public static String decrypt(String encryptedText) throws Exception
    {
        // generate key
        Key key = generateKey();
        Cipher chiper = Cipher.getInstance(algorithm);
        chiper.init(Cipher.DECRYPT_MODE, key);
        byte[] decordedValue = new BASE64Decoder().decodeBuffer(encryptedText);
        byte[] decValue = chiper.doFinal(decordedValue);
        String decryptedValue = new String(decValue);
        return decryptedValue;
    }

    //generateKey() is used to generate a secret key for AES algorithm
    private static Key generateKey() throws Exception
    {
        Key key = new SecretKeySpec(keyValue, algorithm);
        return key;
    }
}

