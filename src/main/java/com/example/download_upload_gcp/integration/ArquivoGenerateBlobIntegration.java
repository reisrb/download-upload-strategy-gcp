package com.example.download_upload_gcp.integration;

import com.example.download_upload_gcp.domain.Arquivo;
import com.example.download_upload_gcp.domain.ArquivoIntegration;
import com.example.download_upload_gcp.integration.dto.ArquivoDetalhesDownloadDto;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
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
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@Validated
@Profile({"dev", "hml", "prd"})
@Qualifier("blobIntegration")
public class ArquivoGenerateBlobIntegration implements ArquivoIntegration {

    private Path diretorioBase;

    @Value("${cloud.bucket}")
    private String bucketName;

    private Storage storage;

    private final SecretKeySpec fileSecretKeySpec;

    public ArquivoGenerateBlobIntegration(SecretKeySpec fileSecretKeySpec) {
        this.fileSecretKeySpec = fileSecretKeySpec;

        try {

            this.storage = StorageOptions
                  .newBuilder()
                  .setCredentials(ServiceAccountCredentials.fromStream(
                        new FileInputStream(System.getenv(
                              "STORAGE_GCP_JSON_KEY")))) // arquivo json da key do google
                  .build()
                  .getService();

            this.diretorioBase = Files.createTempDirectory("anexos_");

            log.info(String.format("Using %s", this.diretorioBase.toString()));
            log.info(String.format("Using blob storage: %s", bucketName));
        } catch (IOException e) {
            log.error("Não foi possível criar o diretório na pasta temporária: {}", e.getMessage());
        }
    }

    @Override
    public String salvar(final MultipartFile arquivo) {
        final String nomeArquivoFormatado = renomearArquivo(arquivo.getOriginalFilename());
        final File caminhoArquivo = diretorioBase.resolve(nomeArquivoFormatado).toFile();

        try {

            OutputStream outputStream = new FileOutputStream(caminhoArquivo);

            arquivo.getInputStream().transferTo(outputStream);
            outputStream.flush();

            BlobId blobId = BlobId.of(bucketName, nomeArquivoFormatado);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

            Storage.BlobWriteOption precondicao;

            if (Objects.isNull(storage.get(bucketName, nomeArquivoFormatado))) {
                precondicao = Storage.BlobWriteOption.doesNotExist();
            } else {
                precondicao = Storage
                      .BlobWriteOption
                      .generationMatch(storage.get(bucketName, nomeArquivoFormatado)
                            .getGeneration());
            }

            storage.createFrom(blobInfo, caminhoArquivo.toPath(), precondicao);

            if (!caminhoArquivo.delete()) {
                log.error("Não foi possível deletar o arquivo temporário.");
            }

            return nomeArquivoFormatado;
        } catch (IOException e) {
            log.error("Não foi possível criar o arquivo no diretório: {}", e.getMessage());
            throw new ResponseStatusException(
                  HttpStatus.UNPROCESSABLE_ENTITY,
                  "Não foi possível criar o arquivo no diretório: " + e.getMessage());
        }
    }

    @Override
    public ArquivoDetalhesDownloadDto baixar(Arquivo arquivo) {

        try {
            Blob blob = storage.get(bucketName, arquivo.getNomeArquivoBlob());

            if (Objects.isNull(blob) || blob.getContent().length == 0) {
                log.error("Arquivo recuperado do blob está vázio ou não existe.");
                return null;
            }

            blob.downloadTo(
                  this.diretorioBase.resolve(arquivo.getNomeArquivoBlob()).toAbsolutePath());
            FileInputStream fileInputStream =
                  new FileInputStream(
                        this.diretorioBase.resolve(arquivo.getNomeArquivoBlob()).toFile());
            removerArquivoTemporario(arquivo.getNomeArquivoBlob());

            return ArquivoDetalhesDownloadDto.builder()
                  .categoria(arquivo.getDescricao())
                  .nomeArquivoOriginal(arquivo.getNomeOriginal())
                  .nomeArquivoBlob(arquivo.getNomeArquivoBlob())
                  .extensaoArquivo(arquivo.getExtensao())
                  .arquivoInputStream(fileInputStream)
                  .build();
        } catch (IOException e) {
            log.error("Não foi possível recuperar o arquivo baixado do blob: {}", e.getMessage());
            throw new ResponseStatusException(
                  HttpStatus.UNPROCESSABLE_ENTITY,
                  "Não foi possível recuperar o arquivo baixado do blob: " + e.getMessage());
        }
    }

    private void removerArquivoTemporario(String nomeArquivo) {
        this.diretorioBase.resolve(nomeArquivo).toFile().delete();
    }

    @Override
    public List<ArquivoDetalhesDownloadDto> baixarEmLote(List<Arquivo> arquivos) {
        return arquivos.stream().map(this::baixar).toList();
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
    public boolean existeArquivo(String nomeArquivo) {
        return !Objects.isNull(this.storage.get(bucketName, nomeArquivo));
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
