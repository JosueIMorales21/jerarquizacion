package com.palacio.environment.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;


public class MyCustomFormatter extends Formatter {
    
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private final String customInfoLevelName;

    public MyCustomFormatter(String customInfoLevelName) {
        this.customInfoLevelName = customInfoLevelName;
    }

    // Nuevo constructor predeterminado con valor predeterminado para customInfoLevelName
    public MyCustomFormatter() {
        this.customInfoLevelName = "INFORMACIÓN"; // Puedes ajustar el valor predeterminado si es necesario
    }

    @Override
    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder();

        // Formato de fecha y hora
        builder.append("[").append(dateFormat.format(new Date())).append("]");

        // Tipo de log (ERROR, INFORMACIÓN, etc.)
        builder.append(" [").append(getCustomLevelName(record.getLevel())).append("]");

        // Mensaje de error
        builder.append(" ").append(formatMessage(record));

        // Nueva línea
        builder.append(System.lineSeparator());

        return builder.toString();
    }

    // Método para obtener el nombre del nivel personalizado
    private String getCustomLevelName(Level level) {
        if (level == Level.SEVERE) {
            return "ERROR";
        } else if (level == Level.INFO) {
            return customInfoLevelName;
        } else {
            return level.getName();
        }
    }
    
}