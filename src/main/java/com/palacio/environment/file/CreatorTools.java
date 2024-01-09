package com.palacio.environment.file;

import com.palacio.environment.main.JerarquizacionApp;
import static com.palacio.environment.main.JerarquizacionApp.terminalesPorTienda;
import static com.palacio.environment.main.JerarquizacionApp.tiendasNivel0;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class CreatorTools {

    private static final Logger logger = Logger.getLogger(JerarquizacionApp.class.getName());

    public static void populateTiendasNivel0AndTerminales(File rootFolder) {
        try {
            Files.walkFileTree(rootFolder.toPath(), EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE,
                    new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // Agrega el nombre de la tienda al arreglo tiendasNivel0
                    tiendasNivel0.add(dir.getFileName().toString());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    // Obtén la ruta relativa desde el directorio raíz
                    Path relativePath = rootFolder.toPath().relativize(file);

                    // Asegúrate de que la ruta contenga al menos dos componentes
                    if (relativePath.getNameCount() >= 2) {
                        String tienda = relativePath.getName(0).toString();
                        String terminal = relativePath.getName(1).toString();

                        // Agrega la tienda y la terminal al arreglo terminalesPorTienda
                        terminalesPorTienda.computeIfAbsent(tienda, k -> new HashSet<>()).add(terminal);
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createTiendasAndTerminalesFolders(String baseFolderPath) {
        for (Map.Entry<String, Set<String>> entry : terminalesPorTienda.entrySet()) {
            String tienda = entry.getKey();
            String tiendaPath = Paths.get(baseFolderPath, tienda).toString();
            createFolderIfNotExists(new File(tiendaPath));

            // Obtén las terminales asociadas a la tienda
            Set<String> terminales = entry.getValue();

            if (terminales != null) {
                for (String terminal : terminales) {
                    // Crea carpeta para la terminal dentro de la tienda
                    String terminalPath = Paths.get(tiendaPath, terminal).toString();
                    createFolderIfNotExists(new File(terminalPath));
                }
            }
        }

        // Log solo las tiendas del nivel 0
        //logger.info("Tiendas del Nivel 0: " + tiendasNivel0.stream().filter(terminalesPorTienda::containsKey).collect(Collectors.toList()));
    }

    private static void createFolderIfNotExists(File folder) {
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                logger.info("Creada carpeta: " + folder.getAbsolutePath());
            } else {
                logger.warning("No se pudo crear la carpeta: " + folder.getAbsolutePath());
            }
        } else {
            logger.warning("La carpeta ya existe: " + folder.getAbsolutePath());
        }
    }

}