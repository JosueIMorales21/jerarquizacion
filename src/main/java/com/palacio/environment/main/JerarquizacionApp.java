package com.palacio.environment.main;

import static com.palacio.environment.config.ConfigLoader.LIMITE_MESES;
import static com.palacio.environment.config.ConfigLoader.LIMITE_YEAR;
import static com.palacio.environment.config.ConfigLoader.REPOSITORIO_RAIZ;
import static com.palacio.environment.config.ConfigLoader.RESPALDO_RUTA;
import static com.palacio.environment.config.ConfigLoader.ZIP_PASSWORD;
import static com.palacio.environment.config.ConfigLoader.configureLogger;
import static com.palacio.environment.config.ConfigLoader.loadConfig;
import static com.palacio.environment.file.CreateZipMaster.createMasterZipsForTerminals;
import static com.palacio.environment.file.CreatorTools.createTiendasAndTerminalesFolders;
import static com.palacio.environment.file.CreatorTools.populateTiendasNivel0AndTerminales;
import com.palacio.environment.file.OrganizerApp;
import com.palacio.environment.file.TiendaTerminales;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

public class JerarquizacionApp {

    public static final List<String> createdZipFiles = new ArrayList<>();

    private static final Logger logger = Logger.getLogger(JerarquizacionApp.class.getName());
    public static final Set<String> tiendasNivel0 = new HashSet<>(); // Nuevo arreglo para almacenar nombres de tiendas del nivel 0
    public static final Map<String, Set<String>> terminalesPorTienda = new HashMap<>();

    public static void main(String[] args) throws IOException {
        loadConfig(); // Cargar configuración desde conCfig.properties
        configureLogger(); // Cargar el LOGGER

        List<TiendaTerminales> listaTiendasTerminales = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : terminalesPorTienda.entrySet()) {
            String tienda = entry.getKey();
            Set<String> terminales = entry.getValue();
            listaTiendasTerminales.add(new TiendaTerminales(tienda, terminales));
        }

        try {
            File rootFolder = new File(REPOSITORIO_RAIZ);

            // Limpia y llena el arreglo tiendasNivel0 antes de realizar la búsqueda y copia de seguridad
            tiendasNivel0.clear();
            populateTiendasNivel0AndTerminales(rootFolder);

            // Muestra el contenido del arreglo tiendasNivel0
            List<String> tiendasNivel0ConInfo = tiendasNivel0.stream()
                    .filter(terminalesPorTienda::containsKey)
                    .collect(Collectors.toList());
            logger.info("Tiendas encontradas: " + tiendasNivel0ConInfo);

            // Muestra las terminales asociadas a cada tienda
            for (String tienda : tiendasNivel0ConInfo) {
                Set<String> terminales = terminalesPorTienda.get(tienda);
                if (terminales != null) {
                    logger.info("Terminales para la tienda " + tienda + ": " + terminales);
                }
            }

            // Crear carpetas en RESPALDO_RUTA para cada tienda del nivel 0
            createTiendasAndTerminalesFolders(RESPALDO_RUTA);

            // Llamada a la función para buscar archivos .txt en las terminales y realizar copias de seguridad
            searchAndBackupTxtFiles(rootFolder, ZIP_PASSWORD, LIMITE_MESES);

            cleanupOldZipFiles(RESPALDO_RUTA);

            // Itera sobre los archivos ZIP creados y organízalos en las carpetas de las terminales
        } catch (IOException e) {
            e.printStackTrace();
        }

        String backupDir = RESPALDO_RUTA;
        String directoryPath = REPOSITORIO_RAIZ;
        int monthsOld = LIMITE_MESES;

        createMasterZipsForTerminals(backupDir);

        OrganizerApp.organizeZipFiles(backupDir);

        OrganizerApp.deleteOriginalZipFiles();

        cleanupOldTxtFiles(directoryPath, monthsOld);
    }

    public static void searchAndBackupTxtFiles(File directory, String zipPassword, int limiteMeses) throws IOException {
        // Asegurémonos de que la ruta termine con un separador de directorios
        if (!directory.getPath().endsWith(File.separator)) {
            directory = new File(directory.getPath() + File.separator);
        }

        // Obtén la lista de archivos en el directorio actual
        File[] files = directory.listFiles();

        if (files != null) {
            File backupDirectory = new File(RESPALDO_RUTA);
            if (!backupDirectory.exists()) {
                backupDirectory.mkdirs();
            }

            // Imprime un mensaje antes del bucle
            logger.info("Iniciando procesamiento de archivos en: " + directory.getPath());

            for (File file : files) {
                // Agrega logs para rastrear el flujo del programa
                logger.info("Encontrado archivo/directorio: " + file.getAbsolutePath());

                if (file.isDirectory()) {
                    // Si es un directorio, llama recursivamente a la función
                    searchAndBackupTxtFiles(file, zipPassword, limiteMeses);

                    // Verifica si la tienda pertenece al nivel 0 y agrega el nombre al arreglo tiendasNivel0
                    if (tiendasNivel0.contains(file.getName())) {
                        tiendasNivel0.add(file.getName());
                    }
                } else if (file.isFile() && file.getName().toLowerCase().endsWith(".txt")) {
                    // Agrega logs para rastrear el flujo del programa
                    logger.info("Procesando archivo: " + file.getAbsolutePath());

                    // Extrae el nombre de la terminal de la ruta del archivo
                    String terminalName = extractTerminalName(file.getAbsolutePath());

                    // Llama a la función de copia de seguridad y limpieza específica para cada terminal
                    backupFiles(file.getAbsolutePath(), ZIP_PASSWORD, 1, 2, terminalName, RESPALDO_RUTA, 1);

                    //cleanupOldTxtFiles(file.getAbsolutePath(), limiteMeses, terminalName);
                    // Verifica si la tienda pertenece al nivel 0 y agrega el nombre al arreglo tiendasNivel0
                    if (tiendasNivel0.contains(terminalName)) {
                        tiendasNivel0.add(terminalName);
                    }
                }
            }

            // Imprime un mensaje después del bucle
            logger.info("Finalizado el procesamiento de archivos en: " + directory.getPath());
        }
    }

    private static void cleanupOldZipFiles(String directoryPath) {
        Path start = Paths.get(directoryPath);

        // Obtén la fecha actual en formato Instant
        Instant currentInstant = Instant.now();
        LocalDate currentDate = currentInstant.atZone(ZoneId.systemDefault()).toLocalDate();

        try {
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // Verifica si el archivo es un archivo ZIP
                    if (Files.isRegularFile(file) && file.toString().toLowerCase().endsWith(".zip")) {
                        // Obtiene la fecha de modificación del archivo
                        Instant lastModifiedInstant = attrs.lastModifiedTime().toInstant();
                        LocalDate lastModifiedDate = lastModifiedInstant.atZone(ZoneId.systemDefault()).toLocalDate();

                        // Calcula la diferencia en años y meses
                        int yearsDifference = currentDate.getYear() - lastModifiedDate.getYear();
                        int monthsDifference = currentDate.getMonthValue() - lastModifiedDate.getMonthValue();

                        // Verifica si la diferencia es mayor a LIMITE_AÑOS o si son exactamente LIMITE_AÑOS y hay al menos 1 mes de diferencia
                        if (yearsDifference > LIMITE_YEAR || (yearsDifference == LIMITE_YEAR && monthsDifference > 0)) {
                            try {
                                Files.delete(file);
                                // Registra el mensaje en el log
                                logger.log(Level.INFO, "Eliminando archivo ZIP: {0}", file);
                            } catch (IOException e) {
                                // Maneja los errores al intentar eliminar el archivo
                                logger.log(Level.SEVERE, "Error al eliminar archivo ZIP: " + file, e);
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    // Maneja los errores al visitar archivos
                    logger.log(Level.SEVERE, "Error visitando archivo: " + file, exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // Maneja los errores generales al caminar por el árbol de archivos
            logger.log(Level.SEVERE, "Error al caminar por el árbol de archivos", e);
        }
    }

    private static void cleanupOldTxtFiles(String directoryPath, int monthsOld) throws IOException {
        Path start = Paths.get(directoryPath);

        try {
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // Verifica si el archivo es un archivo TXT y tiene más de 'monthsOld' meses de antigüedad
                    if (Files.isRegularFile(file) && file.toString().toLowerCase().endsWith(".txt")) {
                        long lastModifiedTime = attrs.lastModifiedTime().toMillis();
                        long currentTime = System.currentTimeMillis();
                        long monthsAgo = currentTime - (monthsOld * 30L * 24L * 60L * 60L * 1000L); // Convierte meses a milisegundos

                        if (lastModifiedTime < monthsAgo) {
                            // Agrega un log para verificar qué archivos se están eliminando
                            logger.info("Eliminando archivo TXT: " + file);
                            try {
                                Files.delete(file);
                                // Registra el mensaje en el log
                                logger.info("Eliminando archivo TXT: " + file);
                            } catch (IOException e) {
                                // Maneja los errores al intentar eliminar el archivo
                                logger.log(Level.SEVERE, "Error al eliminar archivo TXT: " + file, e);
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    // Maneja los errores al visitar archivos
                    logger.log(Level.SEVERE, "Error visitando archivo: " + file, exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // Maneja los errores generales al caminar por el árbol de archivos
            logger.log(Level.SEVERE, "Error al caminar por el árbol de archivos", e);
        }
    }

    // TOMA EL DOCUMENTO Y LO MUEVE A LA CARPETA "Respaldo"
    private static void backupFiles(String sourceFile, String zipPassword, int par, int par1, String terminalName, String destinationFolder, int fileIndex) {
        // Comprime el archivo con su contraseña y número de índice
        compressFile(new File(sourceFile), destinationFolder, zipPassword, terminalName);
    }

    private static void compressFile(File file, String outputDir, String zipPassword, String terminalName) {
        try {
            net.lingala.zip4j.model.ZipParameters parameters = new net.lingala.zip4j.model.ZipParameters();
            parameters.setCompressionMethod(CompressionMethod.DEFLATE);
            parameters.setCompressionLevel(CompressionLevel.NORMAL);
            parameters.setEncryptFiles(true);
            parameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);

            // Construye el nombre del archivo ZIP con el formato originalName_terminalName.zip
            String zipFileName = file.getName().replaceFirst("[.][^.]+$", "") + "_" + terminalName + ".zip";
            net.lingala.zip4j.ZipFile zipFile = new net.lingala.zip4j.ZipFile(new File(outputDir, zipFileName));
            zipFile.setPassword(zipPassword.toCharArray());
            zipFile.addFile(file, parameters);

            // Almacena el nombre del archivo ZIP creado durante la ejecución actual
            createdZipFiles.add(zipFileName);
        } catch (net.lingala.zip4j.exception.ZipException e) {
            // Manejo de Excepciones
            logger.log(Level.SEVERE, "Error al comprimir el archivo: " + file.getName(), e);
        } catch (Exception e) {
            // Manejo de otra excepción
            logger.log(Level.SEVERE, "Error al comprimir el archivo: " + file.getName(), e);
        }
    }

    private static String extractTerminalName(String filePath) {
        // Divide la ruta usando el separador de directorios correspondiente al sistema operativo
        String[] pathSegments = filePath.split("\\\\");

        // Verifica si hay al menos tres segmentos en la ruta
        if (pathSegments.length >= 4) {
            // Agrega loggers informativos
            logger.log(Level.INFO, "Ruta completa: {0}", filePath);
            logger.log(Level.INFO, "Segmentos obtenidos: {0}", Arrays.toString(pathSegments));

            // Elige el segmento que contiene el número de la terminal (ajusta según sea necesario)
            int terminalIndex = 3;  // Puedes ajustar esto según la posición del número de terminal en tu estructura de directorios
            return pathSegments[terminalIndex];
        } else {
            // En caso de que la ruta no tenga suficientes segmentos, devuelve un valor predeterminado o maneja la situación según sea necesario
            logger.log(Level.WARNING, "La ruta no tiene suficientes segmentos: {0}", filePath);
            return "UNKNOWN_TERMINAL";
        }
    }

}
