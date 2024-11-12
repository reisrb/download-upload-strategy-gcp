package com.example.download_upload_gcp.service;

import com.example.download_upload_gcp.domain.Arquivo;
import com.example.download_upload_gcp.domain.ArquivoIntegration;
import com.example.download_upload_gcp.integration.dto.ArquivoDetalhesDownloadDto;
import com.example.download_upload_gcp.repository.ArquivoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ArquivoService {

    public static final String BLOB_CONTAINER = "storage";
    private final ArquivoIntegration arquivoIntegration;
    private final ArquivoRepository arquivoRepository;

    public Arquivo gravarArquivo(
            MultipartFile arquivoRecebido,
            String tipoArquivo,
            String diretorio) {

        String nomeArquivoBlob = arquivoIntegration.salvar(arquivoRecebido, diretorio);

        Arquivo arquivo = new Arquivo();
        arquivo.setNomeOriginal(arquivoRecebido.getOriginalFilename());
        arquivo.setDescricao(tipoArquivo);
        arquivo.setNomeArquivoBlob(diretorio + nomeArquivoBlob);
        arquivo.setBlobContainer(BLOB_CONTAINER);
        arquivo.setExtensao(
                StringUtils.getFilenameExtension(arquivoRecebido.getOriginalFilename()));

        return arquivoRepository.save(arquivo);
    }

    public ArquivoDetalhesDownloadDto baixar(Integer id) {
        Arquivo arquivo = arquivoRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(NOT_FOUND, "Arquivo não encontrado"));
        return arquivoIntegration.baixar(arquivo);
    }

    public InputStream baixarZip() {
        List<Arquivo> arquivos = arquivoRepository.findAll();
        List<ArquivoDetalhesDownloadDto> arquivosDto = arquivoIntegration.baixarEmLote(arquivos);
        return arquivoIntegration.adicionarArquivosNoZip(arquivosDto);
    }

}
