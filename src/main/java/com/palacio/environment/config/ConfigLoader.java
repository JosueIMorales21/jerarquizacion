package com.palacio.environment.config;

import com.palacio.environment.logging.MyCustomFormatter;
import com.palacio.environment.main.JerarquizacionApp;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigLoader {
    
    public static int LIMITE_YEAR;
    public static int LIMITE_MESES;
    public static String ZIP_PASSWORD;
    public static String REPOSITORIO_RAIZ;
    public static String RESPALDO_RUTA;
   
    private static final Logger logger = Logger.getLogger(JerarquizacionApp.class.getName());

    public static void loadConfig() {
        Properties properties = new Properties();
        try ( InputStream input = new FileInputStream("C:\\JERARQUIZACION_1\\config.properties")) {
            if (input == null) {
                String errorMessage = "No se ha encontrado CONFIG.PROPERTIES";
                System.err.println(errorMessage);
                logger.log(Level.SEVERE, errorMessage);
                return;
            }

            // carga un conjunto de propiedades desde el archivo de entrada
            properties.load(input);

            // Obtén los límites de años y meses desde el archivo de configuración
            LIMITE_YEAR = Integer.parseInt(properties.getProperty("LIMITE_YEAR"));
            LIMITE_MESES = Integer.parseInt(properties.getProperty("LIMITE_MESES"));

            // Accede a la contraseña del CONFIG.PROPERTIES
            ZIP_PASSWORD = properties.getProperty("zip.password");

            // Ruta delRESPALDO
            REPOSITORIO_RAIZ = properties.getProperty("REPOSITORIO_RAIZ");
            RESPALDO_RUTA = properties.getProperty("RESPALDO_RUTA");

            // Obtén la fecha actual para incluirla en el nombre del archivo de registro
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String currentDate = dateFormat.format(new Date());
            String logFileName = "log" + currentDate + ".log";

            // Especifica la ubicación completa donde se guardará el archivo de registro
            String logFilePath = "C:\\JERARQUIZACION_1\\logger\\" + logFileName;

            // Configura el manejador de archivos para el logger con la ruta completa del archivo
            FileHandler fileHandler = new FileHandler(logFilePath);
            fileHandler.setFormatter(new MyCustomFormatter()); // Configura el formatter personalizado
            logger.addHandler(fileHandler);

            // Imprime los límites para verificar
            logger.log(Level.INFO, "LIMITE_AÑOS: {0}", LIMITE_YEAR);
            logger.log(Level.INFO, "LIMITE_MESES: {0}", LIMITE_MESES);
        } catch (IOException | NumberFormatException ex) {
            String errorMessage = "Error al cargar la configuración: " + ex.getMessage();
            System.err.println(errorMessage);
            logger.log(Level.SEVERE, errorMessage, ex);
        }
    }
    
    public static void configureLogger() {
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);

        // Cambia el nombre de los niveles de log
        consoleHandler.setFormatter(new MyCustomFormatter("INFORMACIÓN"));

        logger.addHandler(consoleHandler);
        logger.setLevel(Level.ALL);
    }

}
