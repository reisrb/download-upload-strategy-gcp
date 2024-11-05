package com.example.download_upload_gcp.repository;

import com.example.download_upload_gcp.domain.Arquivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArquivoRepository extends JpaRepository<Arquivo, Integer> {

}
