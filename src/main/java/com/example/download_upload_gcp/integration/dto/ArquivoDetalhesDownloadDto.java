package com.example.download_upload_gcp.integration.dto;

import lombok.Builder;
import lombok.Data;

import java.io.InputStream;

@Data
@Builder
public class ArquivoDetalhesDownloadDto {

    private String categoria;
    private String nomeArquivoOriginal;
    private String nomeArquivoBlob;
    private String extensaoArquivo;
    private InputStream arquivoInputStream;
}
