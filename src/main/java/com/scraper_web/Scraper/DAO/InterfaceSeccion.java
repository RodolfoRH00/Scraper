package com.scraper_web.Scraper.DAO;

import com.scraper_web.Scraper.ML.ModelSeccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterfaceSeccion extends JpaRepository<ModelSeccion, Long> {

    List<ModelSeccion> findByTituloContainingIgnoreCase(String keyword);

    Optional<ModelSeccion> findByUrl(String url);

    boolean existsByUrl(String url);
}
