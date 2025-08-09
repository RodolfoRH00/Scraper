package com.scraper_web.Scraper.RestController;

import com.scraper_web.Scraper.ML.ModelSeccion;
import com.scraper_web.Scraper.Servicio.ServicioSeccion;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seccion")
public class RestControllerSeccion {

    private final ServicioSeccion servicio;

    public RestControllerSeccion(ServicioSeccion servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<ModelSeccion> listar() {
        return servicio.listarTodo();
    }

    @GetMapping("/search")
    public List<ModelSeccion> buscar(@RequestParam("query") String query) {
        return servicio.buscar(query);
    }

    @PostMapping("/scrape")
    public List<ModelSeccion> scrape(@RequestParam("query") String query) throws Exception {
        return servicio.scrape(query);
    }

    // Endpoint opcional de estadísticas con Streams
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        var all = servicio.listarTodo();
        var promedioLargoTitulo = all.stream()
                .mapToInt(s -> s.getTitulo() == null ? 0 : s.getTitulo().length())
                .average().orElse(0);

        Map<String, Object> out = new HashMap<>();
        out.put("total", all.size());
        out.put("promedioLongitudTitulo", promedioLargoTitulo);
        out.put("porLetra", all.stream().collect(java.util.stream.Collectors.groupingBy(
                s -> s.getTitulo() == null || s.getTitulo().isBlank() ? "#" : s.getTitulo().substring(0, 1).toUpperCase()
        )));
        return out;
    }
}
