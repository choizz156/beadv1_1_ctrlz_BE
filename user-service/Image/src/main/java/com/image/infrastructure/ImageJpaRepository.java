package com.image.infrastructure;

import org.springframework.data.repository.Repository;

import com.image.domain.entity.Image;
import com.image.domain.repository.ImageRepository;

@org.springframework.stereotype.Repository
public interface ImageJpaRepository extends Repository<Image, String>, ImageRepository {

}
