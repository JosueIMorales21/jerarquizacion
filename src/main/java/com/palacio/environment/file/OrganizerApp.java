package com.palacio.environment.file;

import static com.palacio.environment.config.ConfigLoader.RESPALDO_RUTA;
import com.palacio.environment.main.JerarquizacionApp;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrganizerApp {

    private static final Logger logger = Logger.getLogger(JerarquizacionApp.class.getName());

    public static void organizeZipFiles(String backupDir) {
        // Obtén la lista de archivos ZIP maestros en el directorio de respaldo
        File[] masterZipFiles = new File(backupDir).listFiles((dir, name) -> name.toLowerCase().endsWith(".zip") && name.startsWith("Respaldo"));

        if (masterZipFiles != null && masterZipFiles.length > 0) {
            for (File masterZipFile : masterZipFiles) {
                // Extrae el número de la terminal del nombre del ZIP maestro
                String terminalNumber = extractTerminalNumberFromMasterZip(masterZipFile.getName());

                // Busca la carpeta de la terminal en TODAS las carpetas y niveles
                try {
                    Files.walk(Paths.get(backupDir))
                            .filter(Files::isDirectory)
                            .filter(path -> path.getFileName().toString().endsWith(terminalNumber))
                            .forEach(terminalPath -> {
                                // Copia el archivo ZIP maestro a la carpeta de la terminal
                                copyZipToTerminalFolder(masterZipFile, terminalPath.toString());
                                logger.log(Level.INFO, "ORGANIZER Archivo ZIP maestro {0} copiado a la carpeta de la terminal {1}", new Object[]{masterZipFile.getName(), terminalPath});
                            });
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "ORGANIZER Error al organizar el archivo ZIP maestro en la carpeta de la terminal", e);
                }
            }
        } else {
            logger.log(Level.WARNING, "ORGANIZER No se encontraron archivos ZIP maestros en el directorio de respaldo: {0}", backupDir);
        }
    }

    private static String extractTerminalNumberFromMasterZip(String masterZipFileName) {
        // Implementa la lógica para extraer el número de la terminal del nombre del ZIP maestro
        // Puedes usar expresiones regulares u otras técnicas según el formato del nombre
        // En este ejemplo, se asume que el número de terminal está al final del nombre antes de la extensión .zip
        String extractedTerminalNumber = masterZipFileName.replaceAll("[^\\d]", "");
        return extractedTerminalNumber.length() >= 3 ? extractedTerminalNumber.substring(extractedTerminalNumber.length() - 3) : "";
    }

    public static void copyZipToTerminalFolder(File masterZipFile, String destinationFolderPath) {
        try {
            Files.copy(masterZipFile.toPath(), Paths.get(destinationFolderPath, masterZipFile.getName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "ORGANIZER Error al copiar el archivo ZIP maestro a la carpeta de la terminal", e);
        }
    }

    public static void deleteOriginalZipFiles() {
        try {
            Path respaldoPath = Paths.get(RESPALDO_RUTA);

            Files.walk(respaldoPath)
                    .filter(path -> Files.isRegularFile(path))
                    .filter(path -> path.toString().toLowerCase().endsWith(".zip"))
                    .filter(path -> respaldoPath.relativize(path).getNameCount() == 1) // Filtra solo los archivos en el nivel de RESPALDO_RUTA
                    .forEach(zipFile -> {
                        try {
                            Files.delete(zipFile);
                            logger.log(Level.INFO, "Eliminando archivo ZIP original: {0}", zipFile);
                        } catch (IOException e) {
                            logger.log(Level.SEVERE, "Error al eliminar archivo ZIP original: " + zipFile, e);
                        }
                    });
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error al buscar y eliminar archivos ZIP originales", e);
        }
    }

}