package com.scraper_web.Scraper.ML;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "seccion", indexes = {
    @Index(name = "idx_seccion_url", columnList = "url", unique = true)
})
public class ModelSeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String titulo;

    // Usa TEXT para H2/PostgreSQL (suficiente para 10k+ chars)
    @Column(columnDefinition = "TEXT", nullable = false, unique = true)
    private String url;

    public ModelSeccion() {
    }

    public ModelSeccion(Long id, String titulo, String url) {
        this.id = id;
        this.titulo = titulo;
        this.url = url;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /* Para distinct() y para evitar duplicados por URL */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ModelSeccion that)) {
            return false;
        }
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return "ModelSeccion{id=" + id + ", titulo='" + titulo + "', url='" + url + "'}";
    }
}
