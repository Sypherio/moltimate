package org.moltimate.moltimatebackend.controller;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.moltimate.moltimatebackend.util.FileUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/download")
@Slf4j
public class FileController {

    private enum ProteinFileType {
        PDB(".pdb"),
        MMCIF(".cif"),
        MOTIF(".motif");

        private String fileExtension;

        ProteinFileType(String fileExtension) {
            this.fileExtension = fileExtension;
        }

        @Override
        public String toString() {
            return fileExtension;
        }
    }

    @ApiOperation(value = "Download PDB")
    @RequestMapping(
            path = "/pdb/{pdbId}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public ResponseEntity<Resource> downloadAsPdb(@PathVariable String pdbId) {
        return createResponseFile(
                FileUtils.getPdbFile(pdbId),
                pdbId + ProteinFileType.PDB
        );
    }

    @ApiOperation(value = "Download MMCIF")
    @RequestMapping(
            path = "/mmcif/{pdbId}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public ResponseEntity<Resource> downloadAsMmcif(@PathVariable String pdbId) {
        return createResponseFile(
                FileUtils.getMmcifFile(pdbId),
                pdbId + ProteinFileType.MMCIF
        );
    }

    @ApiOperation(value = "Download Motif")
    @RequestMapping(
            path = "/motif/{pdbId}",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public ResponseEntity<Resource> downloadAsMotif(@PathVariable String pdbId) {
        return createResponseFile(
                FileUtils.getMotifFile(pdbId, null),
                pdbId + ProteinFileType.MOTIF
        );
    }

    private static ResponseEntity<Resource> createResponseFile(Resource file, String fileName) {
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\""
                )
                .body(file);
    }
}