package com.github.assemblathe1.chartographer.repositories;

import com.github.assemblathe1.chartographer.entities.Picture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PicturesRepository extends JpaRepository<Picture, Long> {
}
