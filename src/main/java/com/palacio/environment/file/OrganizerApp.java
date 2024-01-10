package com.palacio.environment.file;

import static com.palacio.environment.config.ConfigLoader.RESPALDO_RUTA;
import com.palacio.environment.main.JerarquizacionApp;
import static com.palacio.environment.main.JerarquizacionApp.createdZipFiles;
import java.io.IOException;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrganizerApp {

    private static final Logger logger = Logger.getLogger(JerarquizacionApp.class.getName());

    public static void organizeZipFiles() {
        for (String zipFileName : createdZipFiles) {
            if (zipFileName.length() >= 3) {
                logger.log(Level.INFO, "ORGANIZER Procesando archivo ZIP: {0}", zipFileName);
                final String terminalNumber = zipFileName.replaceAll("[^\\d]", "");
                final String extractedTerminalNumber = terminalNumber.length() >= 3
                        ? terminalNumber.substring(terminalNumber.length() - 3) : "";

                // Busca la carpeta de la terminal en TODAS las carpetas y niveles
                try {
                    Files.walk(Paths.get(RESPALDO_RUTA))
                            .filter(Files::isDirectory)
                            .filter(path -> path.getFileName().toString().endsWith(extractedTerminalNumber))
                            .forEach(terminalPath -> {
                                // Copia el archivo ZIP a la carpeta de la terminal
                                copyZipToTerminalFolder(zipFileName, terminalPath.toString());
                                logger.log(Level.INFO, "ORGANIZER Archivo ZIP {0} copiado a la carpeta de la terminal {1}", new Object[]{zipFileName, terminalPath});
                            });
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "ORGANIZER Error al organizar el archivo ZIP en la carpeta de la terminal", e);
                }
            } else {
                logger.log(Level.WARNING, "ORGANIZER El nombre del archivo ZIP es demasiado corto para extraer el número de terminal: {0}", zipFileName);
            }
        }
    }

    public static void copyZipToTerminalFolder(String zipFileName, String destinationFolderPath) {
        try {
            Files.copy(Paths.get(RESPALDO_RUTA, zipFileName), Paths.get(destinationFolderPath, zipFileName),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "ORGANIZER Error al copiar el archivo ZIP a la carpeta de la terminal", e);
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
