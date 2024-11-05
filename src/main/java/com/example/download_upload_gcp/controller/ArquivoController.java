package com.example.download_upload_gcp.controller;

import com.example.download_upload_gcp.domain.Arquivo;
import com.example.download_upload_gcp.integration.dto.ArquivoDetalhesDownloadDto;
import com.example.download_upload_gcp.service.ArquivoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/arquivos")
@RequiredArgsConstructor
public class ArquivoController {

    private final ArquivoService arquivoService;

    @PostMapping
    public ResponseEntity<Arquivo> upload(
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam("tipoAnexo") String tipoAnexo
    ) {
        Arquivo arquivoSalvo = arquivoService.gravarArquivo(arquivo, tipoAnexo);
        return ResponseEntity.created(null).body(arquivoSalvo);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Integer id) {
        ArquivoDetalhesDownloadDto arquivoDetalhesDownloadDto = arquivoService.baixar(id);
        try {
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + arquivoDetalhesDownloadDto.getNomeArquivoOriginal())
                    .contentLength(arquivoDetalhesDownloadDto.getArquivoInputStream().available())
                    .body(arquivoDetalhesDownloadDto.getArquivoInputStream().readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
