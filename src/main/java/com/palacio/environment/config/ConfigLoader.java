package com.palacio.environment.config;

import com.palacio.environment.logging.MyCustomFormatter;
import com.palacio.environment.main.JerarquizacionApp;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public static Map<String, List<String>> copyLevelZeroFolders(File source, File destination) {
        Map<String, List<String>> storeTerminalMap = new HashMap<>();

        if (source.isDirectory()) {
            // Verificar si es una carpeta de nivel 0
            if (isLevelZeroFolder(source)) {
                String storeName = source.getName();
                List<String> terminals = new ArrayList<>();

                try {
                    // Copiar la carpeta de nivel 0
                    Path sourcePath = source.toPath();
                    Path destinationPath = new File(destination, storeName).toPath();
                    Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);

                    // Recorrer las subcarpetas y agregar los nombres de las terminales a la lista
                    File[] subFolders = source.listFiles(File::isDirectory);
                    if (subFolders != null) {
                        for (File subFolder : subFolders) {
                            terminals.add(subFolder.getName());
                        }
                    }

                    storeTerminalMap.put(storeName, terminals);
                    logger.log(Level.INFO, "Copiando carpeta: {0}", source);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error al copiar carpeta: " + source, e);
                }
            } else {
                // Si no es de nivel 0, solo recorremos las subcarpetas sin copiarlas
                File[] subFolders = source.listFiles(File::isDirectory);
                if (subFolders != null) {
                    for (File subFolder : subFolders) {
                        Map<String, List<String>> subStoreTerminalMap = copyLevelZeroFolders(subFolder, destination);
                        storeTerminalMap.putAll(subStoreTerminalMap);
                    }
                }
            }
        }

        return storeTerminalMap;
    }

    private static boolean isLevelZeroFolder(File folder) {
        // Implementa tu lógica para determinar si la carpeta es de nivel 0
        return folder.getName().matches("\\d+");
    }

    public static void syncBackupWithRepository(File repository, File backup) {
        // Obtén la lista de carpetas en el repositorio original
        List<String> repositoryFolders = Arrays.asList(repository.list());

        // Obtén la lista de carpetas en el respaldo
        String[] backupFolders = backup.list();

        // Verifica las diferencias y elimina las carpetas en el respaldo que no existen en el repositorio
        if (backupFolders != null) {
            for (String backupFolder : backupFolders) {
                if (!repositoryFolders.contains(backupFolder)) {
                    File folderToDelete = new File(backup, backupFolder);
                    deleteFolder(folderToDelete);
                    logger.log(Level.INFO, "Eliminando carpeta en respaldo: {0}", folderToDelete);
                }
            }
        }
    }

    private static void deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] contents = folder.listFiles();
            if (contents != null) {
                for (File file : contents) {
                    deleteFolder(file);
                }
            }
        }
        folder.delete();
    }

}
