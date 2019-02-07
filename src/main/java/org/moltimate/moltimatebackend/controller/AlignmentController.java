package org.moltimate.moltimatebackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.moltimate.moltimatebackend.request.ActiveSiteAlignmentRequest;
import org.moltimate.moltimatebackend.request.BackboneAlignmentRequest;
import org.moltimate.moltimatebackend.response.AlignmentResponse;
import org.moltimate.moltimatebackend.service.AlignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Alignment REST API
 */
@RestController
@RequestMapping(value = "/align")
@Api(value = "/align", description = "Alignment Controller", produces = "application/json")
@Slf4j
public class AlignmentController {

    @Autowired
    private AlignmentService alignmentService;

    @ApiOperation(value = "Active Site Alignment", response = AlignmentResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "List of active site alignments", response = AlignmentResponse.class),
            @ApiResponse(code = 500, message = "Internal Server Error"),
            @ApiResponse(code = 404, message = "Active site not found")
    })
    @RequestMapping(value = "/activesite", method = RequestMethod.POST)
    public ResponseEntity<AlignmentResponse> activeSiteAlignment(@RequestBody ActiveSiteAlignmentRequest alignmentRequest) {
        /* TODO: Test case if a bad PDB id is given
            Ignore bad PDB ids and continue search
            Collect failed ids and return so a toast can trigger
            TODO: Throw 404 if all PDB ids are incorrect */
        log.info("Received request to align active sites: " + alignmentRequest);
        return ResponseEntity.ok(alignmentService.alignActiveSites(alignmentRequest));
    }

    @ApiOperation(value = "Backbone alignment", response = AlignmentResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "List of active site alignments", response = AlignmentResponse.class),
            @ApiResponse(code = 500, message = "Internal Server Error"),
            @ApiResponse(code = 404, message = "Active site not found")
    })
    @RequestMapping(value = "/backbone", method = RequestMethod.POST)
    public ResponseEntity<AlignmentResponse> backboneAlignment(@RequestBody BackboneAlignmentRequest alignmentRequest) {
        log.info("Received request to align active sites: " + alignmentRequest);
        return ResponseEntity.ok(alignmentService.alignBackbones(alignmentRequest));
    }
}
