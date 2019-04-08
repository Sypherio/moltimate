package org.moltimate.moltimatebackend.service;

import lombok.extern.slf4j.Slf4j;
import org.biojava.nbio.structure.Structure;
import org.moltimate.moltimatebackend.dto.MotifFile;
import org.moltimate.moltimatebackend.dto.Request.MotifTestRequest;
import org.moltimate.moltimatebackend.dto.Alignment.AlignmentMotifResponse;
import org.moltimate.moltimatebackend.dto.PdbQueryResponse;
import org.moltimate.moltimatebackend.model.Alignment;
import org.moltimate.moltimatebackend.util.MotifUtils;
import org.moltimate.moltimatebackend.util.PdbXmlClient;
import org.moltimate.moltimatebackend.util.ProteinUtils;
import org.moltimate.moltimatebackend.util.StructureUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class MotifTestService {

    @Autowired
    private AlignmentService alignmentService;

    private static final int maxRandom = 50;

    public AlignmentMotifResponse testMotifAlignment(MotifTestRequest motifTestRequest) {
        Structure motifStructure = motifTestRequest.motifStructure();
        MotifFile testMotifFile = MotifFile.builder()
                .motif(MotifUtils.generateMotif(motifTestRequest.getPdbId(),
                        motifTestRequest.getEcNumber(),
                        motifStructure,
                        motifTestRequest.parseResidueEntries()))
                .structure(motifStructure)
                .build();

        List<Structure> structureList = new ArrayList<>();
        PdbQueryResponse pdbQueryResponse;
        AlignmentMotifResponse alignmentMotifResponse = new AlignmentMotifResponse(testMotifFile.getMotif());

        switch (motifTestRequest.getType()) {
            case SELF:
                structureList.add(motifStructure);
                structureList.addAll(motifTestRequest.extractCustomStructuresFromFiles());
                break;
            case LIST:
                pdbQueryResponse = motifTestRequest.callPdbForResponse();
                structureList.addAll(pdbQueryResponse.getStructures());
                structureList.addAll(motifTestRequest.extractCustomStructuresFromFiles());

                alignmentMotifResponse.addFailedPdbIds(pdbQueryResponse.getFailedPdbIds());
                break;
            case HOMOLOG:
                List<String> homologuePdbIds = PdbXmlClient.postEcNumberForPdbIds(testMotifFile.getMotif().getEcNumber());
                pdbQueryResponse = ProteinUtils.queryPdbResponse(homologuePdbIds);

                structureList.addAll(pdbQueryResponse.getStructures());
                structureList.addAll(motifTestRequest.extractCustomStructuresFromFiles());

                alignmentMotifResponse.addFailedPdbIds(pdbQueryResponse.getFailedPdbIds());
                break;
            case RANDOM:
                List<String> allPdbIds = PdbXmlClient.getPdbIds();
                Collections.shuffle(allPdbIds);

                int max = motifTestRequest.getRandomCount();
                if (max > maxRandom) {
                    max = maxRandom;
                }
                while (max > 0) {
                    String randomPdbId = allPdbIds.get(max - 1);
                    Optional<Structure> optionalStructure = ProteinUtils.queryPdbOptional(randomPdbId);
                    if (optionalStructure.isPresent()) {
                        Structure testStructure = optionalStructure.get();
                        structureList.add(testStructure);
                        max--;
                    } else {
                        alignmentMotifResponse.addFailedPdbId(randomPdbId);
                    }
                }
                break;
        }

        log.info(String.format("Aligning active sites of %s with %d structures (%d custom structures).",
                testMotifFile.getMotif().getPdbId(), structureList.size(), motifTestRequest.getCustomStructures().size()));

        for (Structure structure : structureList) {
            Alignment alignment = alignmentService.alignActiveSites(structure, testMotifFile.getMotif(), motifStructure, motifTestRequest.getPrecisionFactor());
            if (alignment != null) {
                alignmentMotifResponse.addSuccessfulEntry(structure, alignment);
            } else {
                alignmentMotifResponse.addFailedEntry(structure.getPDBCode(), StructureUtils.ecNumber(structure));
            }
        }
        return alignmentMotifResponse;
    }
}
