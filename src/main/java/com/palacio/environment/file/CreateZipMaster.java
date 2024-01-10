package com.palacio.environment.file;

import com.palacio.environment.main.JerarquizacionApp;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;

public class CreateZipMaster {

    private static final Logger logger = Logger.getLogger(JerarquizacionApp.class.getName());

    public static void createMasterZipsForTerminals(String backupDir) throws IOException, ZipException {
        File backupDirectory = new File(backupDir);

        if (!backupDirectory.exists()) {
            backupDirectory.mkdirs();
        }

        // Obtén la lista de archivos ZIP en el directorio de respaldo
        File[] zipFiles = backupDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));

        if (zipFiles != null && zipFiles.length > 0) {
            // Mapa para almacenar los archivos ZIP según las últimas tres cifras del nombre
            Map<String, List<File>> zipGroups = new HashMap<>();

            // Itera sobre los archivos ZIP
            for (File zipFile : zipFiles) {
                // Obtén las últimas tres cifras del nombre del archivo ZIP
                String zipName = zipFile.getName();
                String terminalKey = zipName.substring(zipName.length() - 7, zipName.length() - 4);

                // Verifica si ya existe un grupo para las últimas tres cifras
                if (!zipGroups.containsKey(terminalKey)) {
                    zipGroups.put(terminalKey, new ArrayList<>());
                }

                // Agrega el archivo ZIP al grupo correspondiente
                zipGroups.get(terminalKey).add(zipFile);
            }

            // Itera sobre los grupos y crea un ZIP maestro para cada uno
            for (Map.Entry<String, List<File>> entry : zipGroups.entrySet()) {
                String terminalKey = entry.getKey();
                List<File> filesInGroup = entry.getValue();

                // Construye el nombre del archivo maestro con las últimas tres cifras comunes
                String currentDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
                String masterZipFileName = "Respaldo_" + currentDate + "_" + terminalKey + ".zip";
                String masterZipFilePath = Paths.get(backupDir, masterZipFileName).toString();

                // Crea o abre el archivo ZIP maestro correspondiente
                net.lingala.zip4j.ZipFile masterZipFile = new net.lingala.zip4j.ZipFile(masterZipFilePath);
                net.lingala.zip4j.model.ZipParameters parameters = new net.lingala.zip4j.model.ZipParameters();
                parameters.setCompressionMethod(CompressionMethod.DEFLATE);
                parameters.setCompressionLevel(CompressionLevel.NORMAL);

                // Agrega todos los archivos en el grupo al ZIP maestro
                for (File file : filesInGroup) {
                    try {
                        masterZipFile.addFile(file, parameters);
                        // Elimina el archivo ZIP individual (opcional)
                        // Files.delete(file.toPath());
                        logger.log(Level.INFO, "Agregado archivo ZIP al ZIP maestro: {0}", file.getAbsolutePath());
                    } catch (net.lingala.zip4j.exception.ZipException e) {
                        // Manejo de excepciones al agregar archivos al ZIP maestro
                        String errorMessage = "Error al agregar archivo al ZIP maestro: " + e.getMessage();
                        logger.log(Level.SEVERE, errorMessage, e);
                        System.err.println(errorMessage);
                        e.printStackTrace();
                    }
                }

                // Agrega mensaje de log indicando que el ZIP maestro se generó exitosamente para la terminal
                logger.log(Level.INFO, "ZIP maestro de la fecha {0} para la terminal {1} generado exitosamente.", new Object[]{currentDate, terminalKey});
            }
        } else {
            logger.log(Level.WARNING, "No se encontraron archivos ZIP en el directorio de respaldo: {0}", backupDir);
        }
    }

    public static String extractTerminalName(String filePath) {
        // Divide la ruta usando el separador de directorios correspondiente al sistema operativo
        String[] pathSegments = filePath.split("\\\\|/");

        // Verifica si hay al menos tres segmentos en la ruta
        if (pathSegments.length >= 1) {
            // Elige el último segmento que contiene el nombre del archivo
            String fileName = pathSegments[pathSegments.length - 1];

            // Verifica si el nombre del archivo contiene un espacio o paréntesis
            if (fileName.contains(" ") || fileName.contains("(") || fileName.contains(")")) {
                // Si contiene espacios o paréntesis, intenta extraer la terminal considerando el formato
                String[] fileNameSegments = fileName.split("[\\s()]+");
                return fileNameSegments[fileNameSegments.length - 1];
            } else {
                // Si no contiene espacios o paréntesis, devuelve el nombre del archivo tal cual
                return fileName;
            }
        } else {
            // En caso de que la ruta no tenga suficientes segmentos, devuelve un valor predeterminado
            logger.log(Level.WARNING, "La ruta no tiene suficientes segmentos: {0}", filePath);
            return "UNKNOWN_TERMINAL";
        }
    }

    public static List<String> getTerminalNamesFromBackups(String backupDir) {
        List<String> terminalNames = new ArrayList<>();

        File backupDirectory = new File(backupDir);

        // Verifica si el directorio de respaldo existe
        if (backupDirectory.exists() && backupDirectory.isDirectory()) {
            // Obtén la lista de archivos ZIP en el directorio de respaldo
            File[] zipFiles = backupDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));

            if (zipFiles != null) {
                for (File zipFile : zipFiles) {
                    // Extrae el nombre de la terminal del archivo ZIP utilizando extractTerminalName
                    String terminalName = extractTerminalName(zipFile.getAbsolutePath());
                    terminalNames.add(terminalName);
                }
            }
        }

        return terminalNames;
    }

}
