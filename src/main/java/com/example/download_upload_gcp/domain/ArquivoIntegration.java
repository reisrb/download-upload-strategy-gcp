package com.example.download_upload_gcp.domain;

import com.example.download_upload_gcp.integration.dto.ArquivoDetalhesDownloadDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface ArquivoIntegration {

    // retorna o nome do arquivo salvo na Blob
    String salvar(final MultipartFile arquivo);

    ArquivoDetalhesDownloadDto baixar(Arquivo arquivo);

    boolean existeArquivo(String nomeArquivo);

    String renomearArquivo(String nomeArquivo);

    List<ArquivoDetalhesDownloadDto> baixarEmLote(List<Arquivo> arquivos);

    InputStream adicionarArquivosNoZip(List<ArquivoDetalhesDownloadDto> arquivos);
}
