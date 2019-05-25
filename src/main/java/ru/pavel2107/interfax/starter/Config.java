package ru.pavel2107.interfax.starter;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class Config {

    static final Logger logger = LogManager.getLogger(Config.class);

    private static Config config = null;

    private String inboxPath;
    private String outboxPath;
    private String tempPath;

    private String templatesPath;
    private int    scanTimeout;

    private String userName;
    private String password;

    private String url;
    private String actionPrefix;

    private String fileMask ;

    public static synchronized Config getInstance() {
        if (config == null) {
            try {
                URL url = Config.class.getClassLoader().getResource("config.properties");
                InputStream is = url.openStream();

                Properties properties = new Properties();
                properties.load(is);
                config = new Config();

                config.inboxPath     = properties.getProperty( "directory.inbox");
                config.outboxPath    = properties.getProperty( "directory.outbox");
                config.tempPath      = properties.getProperty( "directory.temp_dir");
                config.templatesPath = properties.getProperty( "directory.templates");
                config.fileMask      = properties.getProperty( "directory.file_mask");

                config.scanTimeout   = Integer.parseInt( properties.getProperty( "scan.timeout"));

                config.userName      = properties.getProperty( "service.username");
                config.password      = properties.getProperty( "service.password");

                config.url           = properties.getProperty( "service.url");
                config.actionPrefix  = properties.getProperty( "service.action_prefix");

                // String p = PasswordConverter.convert( url, "crypto.key.password", "crypto.key.md5");
                // config.password = p;
            }
            catch ( Exception e){
                logger.error( "Не удалось прочитать настройки", e );
                System.exit(1);
            }
        }
        return config;
    }

    public String getInboxPath() {
        return inboxPath;
    }
    public String getOutboxPath() {
        return outboxPath;
    }
    public String getTempPath() {
        return tempPath;
    }
    public int    getScanTimeout() {
        return scanTimeout;
    }
    public String getUserName() {
        return userName;
    }
    public String getPassword() {
        return password;
    }
    public String getUrl() {
        return url;
    }
    public String getActionPrefix() {
        return actionPrefix;
    }
    public String getFileMask() {
        return fileMask;
    }
    public String getTemplatesPath() {
        return templatesPath;
    }

}
