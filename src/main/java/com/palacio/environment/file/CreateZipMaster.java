package com.palacio.environment.file;

import com.palacio.environment.main.JerarquizacionApp;
import static com.palacio.environment.main.JerarquizacionApp.createdZipFiles;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;


public class CreateZipMaster {
    
    private static final Logger logger = Logger.getLogger(JerarquizacionApp.class.getName());
    
    public static void createMasterZip(String sourceDir, String backupDir) throws IOException, ZipException {
        File sourceDirectory = new File(sourceDir);
        File backupDirectory = new File(backupDir);

        if (!backupDirectory.exists()) {
            backupDirectory.mkdirs();
        }

        String currentDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String masterZipFileName = "Respaldo_" + currentDate + ".zip";
        String masterZipFilePath = Paths.get(backupDir, masterZipFileName).toString();

        net.lingala.zip4j.ZipFile masterZipFile = new net.lingala.zip4j.ZipFile(masterZipFilePath);
        net.lingala.zip4j.model.ZipParameters parameters = new net.lingala.zip4j.model.ZipParameters();
        parameters.setCompressionMethod(CompressionMethod.DEFLATE);
        parameters.setCompressionLevel(CompressionLevel.NORMAL);

        try {
            Files.walkFileTree(sourceDirectory.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // Verifica si el archivo es un archivo ZIP y fue creado durante la ejecución actual
                    if (Files.isRegularFile(file) && file.toString().toLowerCase().endsWith(".zip")
                            && createdZipFiles.contains(file.getFileName().toString())) {
                        try {
                            // Agrega el archivo ZIP al ZIP maestro
                            masterZipFile.addFile(file.toFile(), parameters);
                            // Elimina el archivo ZIP individual
                            Files.delete(file);
                            logger.log(Level.INFO, "Agregado archivo ZIP al ZIP maestro: {0}", file.toString());
                        } catch (net.lingala.zip4j.exception.ZipException e) {
                            // Manejo de excepciones al agregar archivos al ZIP maestro
                            String errorMessage = "Error al agregar archivo al ZIP maestro: " + e.getMessage();
                            logger.log(Level.SEVERE, errorMessage, e);
                            System.err.println(errorMessage);
                            e.printStackTrace();
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    // Maneja los errores al visitar archivos
                    String errorMessage = "Error visitando archivo: " + file;
                    logger.log(Level.SEVERE, errorMessage, exc);
                    System.err.println(errorMessage);
                    exc.printStackTrace();
                    return FileVisitResult.CONTINUE;
                }
            });

            // Agregar mensaje de log indicando que el ZIP maestro se generó exitosamente
            logger.log(Level.INFO, "ZIP maestro de la fecha {0} generado exitosamente.", currentDate);
        } catch (IOException e) {
            // Manejo de excepciones al caminar por el árbol de archivos
            String errorMessage = "Error al caminar por el árbol de archivos";
            logger.log(Level.SEVERE, errorMessage, e);
            System.err.println(errorMessage);
            e.printStackTrace();
        }
    }
    
}
