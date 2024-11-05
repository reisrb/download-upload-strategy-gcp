package com.example.download_upload_gcp.service;

import com.example.download_upload_gcp.domain.Arquivo;
import com.example.download_upload_gcp.domain.ArquivoIntegration;
import com.example.download_upload_gcp.integration.dto.ArquivoDetalhesDownloadDto;
import com.example.download_upload_gcp.repository.ArquivoRepository;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ArquivoService {

    private final ArquivoIntegration arquivoIntegration;
    private final ArquivoRepository arquivoRepository;

    public Arquivo gravarArquivo(
          MultipartFile arquivoRecebido,
          String tipoArquivo) {

        String nomeArquivoBlob = arquivoIntegration.salvar(arquivoRecebido);

        Arquivo arquivo = new Arquivo();
        arquivo.setNomeOriginal(arquivoRecebido.getOriginalFilename());
        arquivo.setDescricao(tipoArquivo);
        arquivo.setNomeArquivoBlob(nomeArquivoBlob);
        arquivo.setBlobContainer("storage");
        arquivo.setExtensao(
              StringUtils.getFilenameExtension(arquivoRecebido.getOriginalFilename()));

        return arquivoRepository.save(arquivo);
    }

    public ArquivoDetalhesDownloadDto baixar(Integer id) {
        Arquivo arquivo = arquivoRepository.findById(id).orElseThrow(
              () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado"));
        return arquivoIntegration.baixar(arquivo);
    }

    public InputStream baixarZip() {
        List<Arquivo> arquivos = arquivoRepository.findAll();
        List<ArquivoDetalhesDownloadDto> arquivosDto = arquivoIntegration.baixarEmLote(arquivos);
        return arquivoIntegration.adicionarArquivosNoZip(arquivosDto);
    }

}
