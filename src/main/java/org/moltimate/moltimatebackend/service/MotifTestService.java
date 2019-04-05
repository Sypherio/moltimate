package org.moltimate.moltimatebackend.service;

import lombok.extern.slf4j.Slf4j;
import org.biojava.nbio.structure.Structure;
import org.moltimate.moltimatebackend.dto.ActiveSiteAlignmentResponse;
import org.moltimate.moltimatebackend.dto.MotifFile;
import org.moltimate.moltimatebackend.dto.PdbQueryResponse;
import org.moltimate.moltimatebackend.dto.MotifTestRequest;
import org.moltimate.moltimatebackend.model.Alignment;
import org.moltimate.moltimatebackend.util.MotifUtils;
import org.moltimate.moltimatebackend.util.PdbXmlClient;
import org.moltimate.moltimatebackend.util.ProteinUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class MotifTestService {

    @Autowired
    private AlignmentService alignmentService;

    private static final int maxRandom = 50;

    public ActiveSiteAlignmentResponse testMotifAlignment(MotifTestRequest motifTestRequest) {
        Structure motifStructure = motifTestRequest.motifStructure();
        MotifFile testMotifFile = MotifFile.builder()
                .motif(MotifUtils.generateMotif(motifTestRequest.getPdbId(),
                        motifTestRequest.getEcNumber(),
                        motifStructure,
                        motifTestRequest.getActiveSiteResidues()))
                .structure(motifStructure)
                .build();

        List<Structure> structureList = new ArrayList<>();
        List<String> failedIds = new ArrayList<>();
        HashMap<String, List<Alignment>> results = new HashMap<>();
        PdbQueryResponse pdbQueryResponse;

        switch (motifTestRequest.getTestType()) {
            case SELF:
                structureList.add(motifStructure);
                structureList.addAll(motifTestRequest.extractCustomStructuresFromFiles());

                for (Structure _structure : structureList) {
                    results.put(_structure.getPDBCode(), new ArrayList<>());
                }
                break;
            case LIST:
                pdbQueryResponse = motifTestRequest.callPdbForResponse();
                structureList.addAll(pdbQueryResponse.getStructures());
                structureList.addAll(motifTestRequest.extractCustomStructuresFromFiles());

                for (Structure _structure : structureList) {
                    results.put(_structure.getPDBCode(), new ArrayList<>());
                }
                failedIds.addAll(pdbQueryResponse.getFailedPdbIds());
                break;
            case HOMOLOG:
                List<String> homologuePdbIds = PdbXmlClient.postEcNumberForPdbIds(testMotifFile.getMotif().getEcNumber());
                pdbQueryResponse = ProteinUtils.queryPdbResponse(homologuePdbIds);

                structureList.addAll(pdbQueryResponse.getStructures());
                structureList.addAll(motifTestRequest.extractCustomStructuresFromFiles());

                for (Structure _structure : structureList) {
                    results.put(_structure.getPDBCode(), new ArrayList<>());
                }
                failedIds.addAll(pdbQueryResponse.getFailedPdbIds());
                break;
            case RANDOM:
                List<String> allPdbIds = PdbXmlClient.getPdbIds();
                Collections.shuffle(allPdbIds);

                int max = motifTestRequest.getRandomCount();
                if (max > maxRandom) {
                    max = maxRandom;
                }
                while (max >= 0) {
                    String randomPdbId = allPdbIds.get(max - 1);
                    Optional<Structure> optionalStructure = ProteinUtils.queryPdbOptional(randomPdbId);
                    if (optionalStructure.isPresent()) {
                        Structure testStructure = optionalStructure.get();
                        structureList.add(testStructure);
                        results.put(testStructure.getPDBCode(), new ArrayList<>());
                        max--;
                    } else {
                        failedIds.add(randomPdbId);
                    }
                }
                break;
        }

        log.info(String.format("Aligning active sites of %s with %d structures (%d custom structures).",
                testMotifFile.getMotif().getPdbId(), structureList.size(), motifTestRequest.getCustomStructures().size()));
        for (Structure structure : structureList) {
            Alignment alignment = alignmentService.alignActiveSites(structure, testMotifFile.getMotif(), motifStructure, motifTestRequest.getPrecisionFactor());
            if (alignment != null) {
                results.get(structure.getPDBCode()).add(alignment);
            }
        }
        return new ActiveSiteAlignmentResponse(results, failedIds);
    }
}
