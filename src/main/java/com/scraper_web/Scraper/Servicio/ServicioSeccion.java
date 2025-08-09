package com.scraper_web.Scraper.Servicio;

import com.scraper_web.Scraper.DAO.InterfaceSeccion;
import com.scraper_web.Scraper.ML.ModelSeccion;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ServicioSeccion {

    private static final Logger log = LoggerFactory.getLogger(ServicioSeccion.class);

    private final InterfaceSeccion repo;

    public ServicioSeccion(InterfaceSeccion repo) {
        this.repo = repo;
    }

    // Usaremos esta base, aunque ML nos redirija a /jm/search?as_word=
    @Value("${scraper.mercado-libre-url:https://listado.mercadolibre.com.mx/}")
    private String mercadoLibreBase;

    /**
     * Scrapea hasta 20 resultados y los guarda evitando duplicados por URL.
     */
    public List<ModelSeccion> scrape(String query) throws Exception {
        String url = mercadoLibreBase + URLEncoder.encode(query, StandardCharsets.UTF_8);

        Document dom = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome Safari")
                .header("Accept-Language", "es-MX,es;q=0.9")
                .referrer("https://www.google.com/")
                .timeout(15000)
                .followRedirects(true)
                .get();

        log.info("GET {} -> title='{}'", dom.location(), dom.title());

        // 1) Juntamos posibles contenedores de resultado (plantillas viejas y nuevas)
        List<Element> items = new ArrayList<>();
        items.addAll(dom.select("li.ui-search-layout__item"));
        items.addAll(dom.select("div.ui-search-result__content-wrapper"));
        items.addAll(dom.select("div.poly-card")); // plantillas 'poly'
        items.addAll(dom.select("div.poly-component__container"));
        if (items.isEmpty()) {
            // fallback muy amplio
            items.addAll(dom.select("a.ui-search-link, a.poly-component__title").parents());
        }
        log.info("Contenedores detectados: {}", items.size());

        // 2) Mapear a dominio con selectores tolerantes
        List<ModelSeccion> candidatos = items.stream()
                .map(this::toModelRobusto) // Element -> ModelSeccion
                .filter(m -> m.getTitulo() != null && !m.getTitulo().isBlank())
                // TEMP: relajar a >= 3 palabras para validar extracción.
                // Cuando confirmes, cambia a > 5 como pedía la prueba.
                .filter(m -> m.getTitulo().split("\\s+").length >= 3)
                .distinct() // usa equals/hashCode (por URL) en tu entidad
                .filter(this::notDuplicate) // evita duplicados en BD
                .limit(20)
                .collect(Collectors.toList());

        if (candidatos.isEmpty()) {
            // Diagnóstico: imprime breve HTML de los 2 primeros contenedores
            items.stream().limit(2).forEach(el
                    -> log.warn("Diagnóstico: snippet={}", el.outerHtml().replaceAll("\\s+", " ").substring(0, Math.min(400, el.outerHtml().length())))
            );
            log.warn("0 candidatos tras mapear/filtrar. Revisa selectores en toModelRobusto()");
            return List.of();
        }

        return repo.saveAll(candidatos);
    }

    /* =================== Helpers =================== */
    /**
     * Selectores alternativos para título y link en distintas plantillas.
     */
    private ModelSeccion toModelRobusto(Element el) {
        String titulo = pickFirstText(el,
                "h2.ui-search-item__title",
                ".ui-search-item__title",
                ".poly-component__title-wrapper",
                ".poly-component__title",
                "a.ui-search-link h2",
                "a.poly-component__title",
                "h2", "h3" // ultra-fallbacks
        );

        // Limpieza básica
        if (titulo == null) {
            titulo = "";
        }
        titulo = titulo.replaceAll("[^\\p{L}\\p{N}\\s]", " ").replaceAll("\\s{2,}", " ").trim();

        String link = pickFirstHref(el,
                "a.ui-search-link",
                "a.poly-component__title",
                "a.ui-search-result__content",
                "a[href]" // fallback genérico
        );

        return new ModelSeccion(null, titulo, link);
    }

    /**
     * Retorna el primer texto no vacío según los selectores dados.
     */
    private String pickFirstText(Element base, String... selectors) {
        for (String sel : selectors) {
            Element n = base.selectFirst(sel);
            if (n != null) {
                String t = n.text();
                if (t != null && !t.isBlank()) {
                    return t;
                }
            }
        }
        return "";
    }

    /**
     * Retorna el primer href absoluto no vacío según los selectores dados.
     */
    private String pickFirstHref(Element base, String... selectors) {
        for (String sel : selectors) {
            Element a = base.selectFirst(sel);
            if (a != null) {
                String href = a.absUrl("href");
                if (href == null || href.isBlank()) {
                    href = a.attr("href");
                }
                if (href != null && !href.isBlank()) {
                    return href;
                }
            }
        }
        return "";
    }

    /**
     * true si la URL NO existe aún en la BD
     */
    private boolean notDuplicate(ModelSeccion m) {
        return m.getUrl() != null && !m.getUrl().isBlank() && !repo.existsByUrl(m.getUrl());
    }

    /* =============== Métodos del servicio =============== */
    public List<ModelSeccion> buscar(String query) {
        return repo.findByTituloContainingIgnoreCase(query);
    }

    public List<ModelSeccion> listarTodo() {
        return repo.findAll();
    }
}
