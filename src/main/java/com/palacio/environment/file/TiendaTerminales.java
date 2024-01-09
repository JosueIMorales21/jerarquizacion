package com.palacio.environment.file;

import java.util.Set;

public class TiendaTerminales {
    private String tienda;
    private Set<String> terminales;

    public TiendaTerminales(String tienda, Set<String> terminales) {
        this.tienda = tienda;
        this.terminales = terminales;
    }

    public String getTienda() {
        return tienda;
    }

    public Set<String> getTerminales() {
        return terminales;
    }
}
