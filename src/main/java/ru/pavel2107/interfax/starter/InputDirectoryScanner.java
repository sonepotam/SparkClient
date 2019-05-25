package ru.pavel2107.interfax.starter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import ru.pavel2107.interfax.starter.utils.Utils;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.nio.file.*;
import java.util.stream.StreamSupport;

public class InputDirectoryScanner extends Thread {

    static final Logger logger = LogManager.getLogger(InputDirectoryScanner.class);
    static private InputDirectoryScanner InputDirectoryScanner = null;

    private Config config;

    private InputDirectoryScanner(){}

    public static InputDirectoryScanner getInstance() throws Exception{
        if( InputDirectoryScanner == null){
            InputDirectoryScanner = new InputDirectoryScanner();
            InputDirectoryScanner.config = Config.getInstance();
        }

        return InputDirectoryScanner;
    }

    @Override
    public void run(){
        try {
            int MAX_DELAY = 5 * 60 * 1000;
            int curDelay  = 0;
            logger.info( "Входной каталог " + config.getInboxPath() + ", Маска файлов " + config.getFileMask());
            while ( true){
                iteration();
                // logger.debug( "Пауза сканирования входного каталога " + config.getDelay() + " ms");
                if( curDelay >= MAX_DELAY){
                    curDelay = 0;
                    logger.debug( "Пауза сканирования входного каталога " + ( MAX_DELAY/ 60/ 1000) + " секунд");
                }
                curDelay = curDelay + config.getScanTimeout();
                Thread.sleep( config.getScanTimeout() * 1000);
            }
        }
        catch (Exception e){
            logger.error( "Запуск сканирования каталогов", e);
        }
    }

    //
    // один проход по каталогу
    //
    public void iteration(){

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + config.getFileMask());

        Path dir = Paths.get(config.getInboxPath());
        try (DirectoryStream<Path> stream = Files.newDirectoryStream( dir, config.getFileMask())) {
            logger.debug( "Начинаем сканирование");

            StreamSupport.stream( stream.spliterator(), false)
                    //
                    // сортируем по времени модификации
                    //
                    .sorted((o1, o2) -> {
                        try {
                            return Files.getLastModifiedTime(o1).compareTo(Files.getLastModifiedTime(o2));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        return 0;
                    })
                    //
                    // берем файлы по заданной маске и ненулевого размера. их уже отпустила АБС
                    //
                    .filter(
                            file -> matcher.matches( file.getFileName()) && ( file.toFile().length() > 0)
                    )
                    //
                    // обрабатываем
                    //
                    .forEach(file -> {
                        try {
                            logger.debug( file);
                            processFile( file);
                        }
                        catch (Exception e){
                            logger.error( "Ошибка при обработке файла " + file, e);
                        }
                    });

        } catch (Exception e) {
            logger.error( "Возникла ошибка ", e);
        }
    }


    private void processFile( Path file)throws Exception {
        try {
            logger.info( "Файл " + file);
            Config config = Config.getInstance();
            String result = null;
            //
            // поддержим сессию http
            //
            CookieManager cookieManager = new CookieManager();
            CookieHandler.setDefault( cookieManager);
            //
            // авторизация
            //
            SparkConnection sparkConnection = new SparkConnection( config.getUrl());
            sparkConnection.configureConnection();

            RequestProcessor processor = new RequestProcessor( sparkConnection.getConnection(), config.getActionPrefix());
            result = processor.exchange(  config.getTemplatesPath() + "\\auth.xml");
            System.out.println( result);
            //
            // отправляем запрос
            //
            sparkConnection = new SparkConnection( config.getUrl());
            sparkConnection.configureConnection();

            processor = new RequestProcessor( sparkConnection.getConnection(), config.getActionPrefix());
            result = processor.exchange( file.toString());
            logger.info( result);
            //
            // определяем имя для ответа
            //
            Document document = Utils.buildDocument( result);
            String responseName = Utils.extractAction( document);
            logger.info( "response type ->" + responseName);
            String fileNumber = file.toString().substring( file.toString().indexOf( "_", -1));
            File responseFile = new File( config.getTempPath() + "/" + responseName + fileNumber);
            //
            // сохраняем ответ в файле
            //
            BufferedWriter writer = new BufferedWriter( new FileWriter( responseFile));
            writer.write( result);
            writer.close();
            //
            // переносим файл в выходной каталог
            //
            Utils.moveTo( responseFile, config.getOutboxPath());
            //
            // окончание сессии
            //
            sparkConnection = new SparkConnection( config.getUrl());
            sparkConnection.configureConnection();

            processor = new RequestProcessor( sparkConnection.getConnection(), config.getActionPrefix());
            result = processor.exchange( config.getTemplatesPath() + "\\end.xml");
            System.out.println( result);
        }
        catch ( Exception e){
            logger.error( "FILE:" + file.toFile() + "^\n" + "Возникла ошибка при обработке файлов" +
                    "", e);
        }
        finally {
          //  file.toFile().delete();
        }
    }



}



/*
    public Etp signFile( DocumentBuilder builder, StringBuffer docStr, StringBuffer msgID, File file)throws Exception{
        Etp etp = null;
        RandomAccessFile randomAccessFile = null;
        try {
            //
            // читаем файл
            //
            logger.info("читаем файл " + file);
            randomAccessFile = new RandomAccessFile(file, "r");
            byte[] encoded = new byte[(int) randomAccessFile.length()];
            randomAccessFile.read(encoded);
            String content = new String(encoded, StandardCharsets.UTF_8);
            logger.info("содержимое файла " + content);
            //
            // разбираем содержимое в dom-модель
            //
            Document docPackage = builder.parse(new InputSource(new StringReader(content)));
            docPackage.getDocumentElement().normalize();
            //
            // читаем содержимое траспортного документа
            //
            Node docNode = docPackage.getElementsByTagName("Document").item(0);
            //
            // извлекаем содержимое тега Document
            //
            String document = utils.getTagValue(docPackage, "Document");
            //logger.debug("содержимое тега Document:\n" + document);
            //
            // снимаем кодировку тега Document
            //
            byte[] decodedDocument = Base64.getDecoder().decode(document);
            String decoded = new String(decodedDocument, "UTF-8");
            logger.info("декодированнное содержимое тега Document\n" + utils.prettyFormat( decoded));
            //
            // анализируем прочитанное содержимое
            //
            Document doc = builder.parse(new InputSource(new StringReader(decoded)));
            doc.getDocumentElement().normalize();
            String signature = signer.sign(decoded);
            //
            // вычисляем msgID
            //
            msgID.append( utils.getTagValue( doc, "MsgID"));
            //
            // извлекаем площадку
            //
            String operatorName = utils.getTagValue(doc, "OperatorName");
            logger.info( "Обрабатываем сообщение # " + msgID + " на площадку " + operatorName);
            //
            // кодируем заново и снова записываем
            //
            byte[] codedDocument = Base64.getEncoder().encode(decodedDocument);
            if (document.equals(codedDocument)) {
                logger.debug("Двойное преобразование успешно");
            }

            docNode.getFirstChild().setNodeValue(new String(codedDocument, "UTF-8"));
            //
            // записываем подпись
            //
            Node signNode = docPackage.getElementsByTagName("Signature").item(0);
            signNode = signNode.getFirstChild();
            signNode.setNodeValue(signature);
            //
            // определим площадку
            //
            etp = EtpHolder.getInstance().get(operatorName);
            //
            // выводим результат
            //
            docStr.append( utils.convertDocToString(docPackage));
            String signedFile = config.getTempPath() + "\\" + file.getName() + ".signed";
            Array.writeFile(signedFile, docStr.toString().getBytes("UTF-8"));
            logger.info(docStr);
        }
        catch ( Exception e){
            logger.error( "FILE:" + file + "^\n" + "Возникла ошибка при обработке файлов" +
                    "", e);
        }
        finally {
            if ( randomAccessFile != null) {
                randomAccessFile.close();
            }
        }
        return etp;


    }

    private boolean repeatableSend( RestTemplate restTemplate, String msgID, Etp etp, String request){
        boolean lSended = false;
        int attempts =  config.getAttempts();
        //
        // упаковываем документ в строку и отправляем на сервер
        //
        HttpHeaders headers = new HttpHeaders();
        MediaType mediaType = new MediaType("text", "xml", StandardCharsets.UTF_8);
        headers.setContentType(mediaType);
        ResponseEntity<String> responseEntity = null;
        Exception ee = null;

        String url = etp.getUrl();
        String responseFileName = null;
        String responseString = "";

        if (url == null) {
            logger.error("Не определен url для площадки: " + etp.getName());
        } else {
            logger.info("Определен url: " + url);
            HttpEntity<String> entity = new HttpEntity<String>(request, headers);
            if ("yes".equals(etp.getSend())) {
                logger.info("Запускаем отправку -> " + url);
                for( int i = 0; i < attempts; i++) {
                    try {
                        responseEntity = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                        logger.info( "Статус отправки : " + responseEntity.getStatusCode().toString() );
                        //
                        // если отправка состоялась, формируем ответ и выходим
                        //
                        if ( HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                            responseFileName = "rest_server_response_msgid_" + msgID + ".xml";
                            responseString   = responseEntity.getStatusCode().toString();
                            lSended = true;
                            break;
                        }
                    }
                    catch( HttpStatusCodeException statusException){
                        responseFileName = "etp_err_msgid_" + msgID + ".xml";
                        responseString   = statusException.getResponseBodyAsString();
                        //
                        // если получили 500 - это прикладная ошибка и можно выходить
                        //
                        if( statusException.getStatusCode().equals( HttpStatus.INTERNAL_SERVER_ERROR)){
                            lSended = true;
                            break;
                        }
                        logger.info("Неудачная попытка отправки # " + (i +1) + " " +  statusException.toString());
                        ee = statusException;
                    }
                    catch (Exception e) {
                        logger.info("Неудачная попытка отправки # " + (i +1) + " " +  e.toString());
                        ee = e;
                    }
                    //
                    // сохраняем информацию об ошибке, может и понадобится
                    //
                    if( ee != null){
                        responseFileName = "rest_server_error_msgid_" + msgID + ".xml";
                        StringWriter writer = new StringWriter();
                        ee.printStackTrace(new PrintWriter(writer));
                        responseString = writer.toString();
                        ee = null;
                    }
                    //
                    //
                    // выдерживаем паузу
                    //
                    logger.info( "Пауза " + config.getSendTimeout() + " ms");
                    try { Thread.sleep(config.getSendTimeout()); } catch ( InterruptedException ie){}
                }
                //
                // сохраняем результат в файл
                //
                File respFile = new File(config.getFrom_ETP_Path() + "/" + responseFileName);
                try {
                    logger.info( "Сохраняем файл " + respFile.toPath() + " ->\n" + responseString);
                    Files.write(respFile.toPath(), responseString.getBytes("UTF-8"));
                } catch ( Exception e){
                    logger.error( "Не удалось сохранить в файл " + responseFileName + " результат выполнения" + responseString, e);
                }

            }
        }
        return lSended;
    }

 */
