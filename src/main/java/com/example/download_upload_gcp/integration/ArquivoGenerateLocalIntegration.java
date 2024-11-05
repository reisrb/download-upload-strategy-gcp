package com.example.download_upload_gcp.integration;

import com.example.download_upload_gcp.domain.Arquivo;
import com.example.download_upload_gcp.domain.ArquivoIntegration;
import com.example.download_upload_gcp.integration.dto.ArquivoDetalhesDownloadDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@Validated
@Profile({"local", "docker", "test"})
@Qualifier("localIntegration")
@Primary
public class ArquivoGenerateLocalIntegration implements ArquivoIntegration {

    private Path diretorioBase;

    private final SecretKeySpec fileSecretKeySpec;

    public ArquivoGenerateLocalIntegration(SecretKeySpec fileSecretKeySpec) {
        this.fileSecretKeySpec = fileSecretKeySpec;

        try {
            this.diretorioBase = Files.createTempDirectory("anexos_");
            log.info("Using {}", this.diretorioBase.toString());
        } catch (IOException e) {
            log.error("Não foi possível criar o diretório na pasta temporária: {}", e.getMessage());
        }
    }

    @Override
    public String salvar(MultipartFile arquivo) {
        String extensaoArquivo = FilenameUtils.getExtension(arquivo.getOriginalFilename());
        final String nomeArquivoFormatado = renomearArquivo(arquivo.getOriginalFilename());
        final File caminhoArquivo = diretorioBase.resolve(nomeArquivoFormatado).toFile();

        try (final OutputStream outputStream = new CipherOutputStream(
                new GZIPOutputStream(new FileOutputStream(caminhoArquivo)),
                ArquivoCriptografia.getCipher(Cipher.ENCRYPT_MODE, this.fileSecretKeySpec)
        )) {
            arquivo.getInputStream().transferTo(outputStream);
            outputStream.flush();
        } catch (IOException e) {
            log.error("Não foi possível criar o arquivo no diretório: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não foi possível criar o arquivo no diretório: " + e.getMessage());
        }

        return nomeArquivoFormatado;
    }

    @Override
    public ArquivoDetalhesDownloadDto baixar(Arquivo arquivo) {
        try {

            if (!existeArquivo(arquivo.getNomeArquivoBlob())) {
                log.info("Arquivo recuperado do diretório temporário não existe. Gerando arquivo em branco.");
                return null;
            }

            // Arquivo descriptografado retornado da temporária
            CipherInputStream cipherInputStream = new CipherInputStream(new GZIPInputStream(
                    new FileInputStream(this.diretorioBase.resolve(arquivo.getNomeArquivoBlob()).toFile())
            ), ArquivoCriptografia.getCipher(Cipher.DECRYPT_MODE, this.fileSecretKeySpec));

            return ArquivoDetalhesDownloadDto.builder()
                    .categoria(arquivo.getDescricao())
                    .nomeArquivoOriginal(arquivo.getNomeOriginal())
                    .nomeArquivoBlob(arquivo.getNomeArquivoBlob())
                    .extensaoArquivo(arquivo.getExtensao())
                    .arquivoInputStream(cipherInputStream)
                    .build();
        } catch (IOException e) {
            log.error("Não foi possível recuperar o arquivo no diretório: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não foi possível recuperar o arquivo no diretório: " + e.getMessage());
        }
    }

    @Override
    public boolean existeArquivo(String nomeArquivo) {
        return this.diretorioBase.resolve(nomeArquivo).toFile().exists();
    }

    @Override
    public String renomearArquivo(String nomeArquivo) {
        String nomeArquivoSemExtensao = FilenameUtils.removeExtension(nomeArquivo);
        String extensaoArquivo = FilenameUtils.getExtension(nomeArquivo);

        ZoneId zoneSaoPaulo = ZoneId.of("America/Sao_Paulo");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
        String dataFormatada = LocalDate.now(zoneSaoPaulo).format(formatter);

        return String.format("%s_%s.%s", nomeArquivoSemExtensao, dataFormatada, extensaoArquivo);
    }

    @Override
    public List<ArquivoDetalhesDownloadDto> baixarEmLote(List<Arquivo> arquivos) {

        List<ArquivoDetalhesDownloadDto> arquivosDetalhes = new ArrayList<>();

        arquivos.forEach(arquivo -> arquivosDetalhes.add(baixar(arquivo)));

        return arquivosDetalhes;
    }

    @Override
    public InputStream adicionarArquivosNoZip(List<ArquivoDetalhesDownloadDto> arquivos) {

        try {
            File zipTemp = this.diretorioBase.resolve(
                    String.format("zip_temp_%s.zip", Thread.currentThread().getId())).toFile();

            ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipTemp));

            for (ArquivoDetalhesDownloadDto arquivo : arquivos) {

                ZipEntry e = new ZipEntry(renomearArquivo(arquivo.getNomeArquivoOriginal()));

                zipOut.putNextEntry(e);

                byte[] buffer = new byte[1024];

                int len;
                while ((len = arquivo.getArquivoInputStream().read(buffer)) > 0) {
                    zipOut.write(buffer, 0, len);
                }

                zipOut.flush();
            }

            zipOut.closeEntry();
            zipOut.close();

            InputStream zipStream = new FileInputStream(zipTemp);

            if (!zipTemp.delete()) {
                log.error("Não foi possível deletar o arquivo temporário.");
            }

            return zipStream;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
