package com.example.download_upload_gcp.integration.dto;

import java.io.InputStream;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArquivoDetalhesDownloadDto {

    private String categoria;
    private String nomeArquivoOriginal;
    private String nomeArquivoBlob;
    private String extensaoArquivo;
    private InputStream arquivoInputStream;
}
