package com.palacio.environment.main;

import static com.palacio.environment.config.ConfigLoader.LIMITE_MESES;
import static com.palacio.environment.config.ConfigLoader.LIMITE_YEAR;
import static com.palacio.environment.config.ConfigLoader.REPOSITORIO_RAIZ;
import static com.palacio.environment.config.ConfigLoader.RESPALDO_RUTA;
import static com.palacio.environment.config.ConfigLoader.ZIP_PASSWORD;
import static com.palacio.environment.config.ConfigLoader.configureLogger;
import static com.palacio.environment.config.ConfigLoader.loadConfig;
import com.palacio.environment.file.CreateZipMaster;
import static com.palacio.environment.file.CreatorTools.createTiendasAndTerminalesFolders;
import static com.palacio.environment.file.CreatorTools.populateTiendasNivel0AndTerminales;
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

    public static void main(String[] args) {
        loadConfig(); // Cargar configuración desde conCfig.properties
        configureLogger(); // Cargar el LOGGER

        try {
            File rootFolder = new File(REPOSITORIO_RAIZ);

            // Limpia y llena el arreglo tiendasNivel0 antes de realizar la búsqueda y copia de seguridad
            tiendasNivel0.clear();
            populateTiendasNivel0AndTerminales(rootFolder);

            // Muestra el contenido del arreglo tiendasNivel0
            List<String> tiendasNivel0ConInfo = tiendasNivel0.stream()
                    .filter(terminalesPorTienda::containsKey)
                    .collect(Collectors.toList());
            logger.info("Tiendas del Nivel 0: " + tiendasNivel0ConInfo);

            // Muestra las terminales asociadas a cada tienda
            for (String tienda : tiendasNivel0ConInfo) {
                Set<String> terminales = terminalesPorTienda.get(tienda);
                if (terminales != null) {
                    logger.info("Terminales para la tienda " + tienda + ": " + terminales);
                }
            }

            // Itera sobre las tiendas del nivel 0 y crea un archivo ZIP maestro para cada conjunto de terminales
            for (String tienda : tiendasNivel0ConInfo) {
                Set<String> terminales = terminalesPorTienda.get(tienda);
                if (terminales != null) {
                    for (String terminal : terminales) {
                        // Crea el archivo ZIP maestro para cada terminal
                        CreateZipMaster.createMasterZipForTerminal(RESPALDO_RUTA + File.separator + tienda + File.separator + terminal,
                                RESPALDO_RUTA + File.separator + tienda + File.separator + terminal);
                    }
                }
            }

            // Crear carpetas en RESPALDO_RUTA para cada tienda del nivel 0
            createTiendasAndTerminalesFolders(RESPALDO_RUTA);

            // Llamada a la función para buscar archivos .txt en las terminales y realizar copias de seguridad
            searchAndBackupTxtFiles(rootFolder, ZIP_PASSWORD, LIMITE_MESES);

            cleanupOldZipFiles(RESPALDO_RUTA);

            // Itera sobre los archivos ZIP creados y organízalos en las carpetas de las terminales
            for (String zipFileName : createdZipFiles) {
                // Verifica si la longitud del nombre del archivo ZIP es suficiente para extraer 3 dígitos
                if (zipFileName.length() >= 3) {

                    // Agrega un log para imprimir el nombre del archivo ZIP
                    logger.log(Level.INFO, "Procesando archivo ZIP: {0}", zipFileName);

                    // Extrae los últimos 3 dígitos del nombre del archivo ZIP
                    String terminalNumber = zipFileName.replaceAll("[^\\d]", "");
                    terminalNumber = terminalNumber.length() >= 3
                            ? terminalNumber.substring(terminalNumber.length() - 3) : "";
                    
                    final String finalTerminalNumber = terminalNumber;

                    // Verifica si la terminal existe en el conjunto de terminales del nivel 0
                    if (terminalesPorTienda.values().stream().anyMatch(terminales -> terminales.contains(finalTerminalNumber))) {
                        // Resto del código...

                        // Mueve el archivo ZIP a la carpeta correspondiente a la terminal en RESPALDO
                        String sourceFilePath = Paths.get(RESPALDO_RUTA, zipFileName).toString();
                        String destinationFolderPath = Paths.get(RESPALDO_RUTA, "TERMINAL_" + terminalNumber).toString();
                        String destinationFilePath = Paths.get(destinationFolderPath, zipFileName).toString();

                        try {
                            // Crea la carpeta de la terminal si no existe
                            Files.createDirectories(Paths.get(destinationFolderPath));

                            // Mueve el archivo ZIP a la carpeta de la terminal
                            Files.move(Paths.get(sourceFilePath), Paths.get(destinationFilePath), StandardCopyOption.REPLACE_EXISTING);
                            logger.log(Level.INFO, "Archivo ZIP {0} movido a la carpeta de la terminal {1}", new Object[]{zipFileName, terminalNumber});
                        } catch (IOException e) {
                            logger.log(Level.SEVERE, "Error al mover el archivo ZIP a la carpeta de la terminal", e);
                        }
                    } else {
                        logger.log(Level.WARNING, "No se encontró una terminal asociada al archivo ZIP {0}", zipFileName);
                    }
                } else {
                    // Manejar la situación donde zipFileName es demasiado corto
                    logger.log(Level.WARNING, "El nombre del archivo ZIP es demasiado corto para extraer el número de terminal: {0}", zipFileName);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void searchAndBackupTxtFiles(File directory, String zipPassword, int limiteMeses) throws IOException {
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
                    cleanupOldTxtFiles(file.getAbsolutePath(), limiteMeses, terminalName);

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

    private static void cleanupOldTxtFiles(String filePath, int monthsOld, String terminalName) {
        Path start = Paths.get(filePath).getParent();  // Obtiene la ruta del directorio del archivo

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
                            // Extrae el nombre de la terminal de la ruta del archivo
                            String terminalOfFile = extractTerminalName(file.toString());

                            // Compara el nombre de la terminal del archivo con el nombre de la terminal actual
                            if (terminalOfFile.equals(terminalName)) {
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
                            } else {
                                // Agrega un log para archivos que no cumplen con los criterios de limpieza
                                logger.info("Archivo TXT no eliminado (no pertenece a la terminal actual): " + file);
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
        if (pathSegments.length >= 3) {
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