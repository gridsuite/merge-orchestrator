/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.merge.orchestrator.server;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.gridsuite.merge.orchestrator.server.dto.Merge;
import org.gridsuite.merge.orchestrator.server.dto.MergeConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */

@RestController
@RequestMapping(value = "/" + MergeOrchestratorApi.API_VERSION)
@Transactional
@Api(value = "Merge orchestrator server")
@ComponentScan(basePackageClasses = MergeOrchestratorService.class)
public class MergeOrchestratorController {

    private final MergeOrchestratorService mergeOrchestratorService;

    private final MergeOrchestratorConfigService mergeConfigService;

    public MergeOrchestratorController(MergeOrchestratorService mergeOrchestratorService, MergeOrchestratorConfigService mergeConfigService) {
        this.mergeOrchestratorService = mergeOrchestratorService;
        this.mergeConfigService = mergeConfigService;
    }

    @GetMapping(value = "/configs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all merge configurations", response = List.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The list of all merge configurations")})
    public ResponseEntity<List<MergeConfig>> getConfigs() {
        List<MergeConfig> configs = Collections.singletonList(new MergeConfig(mergeConfigService.getProcess(), mergeConfigService.getTsos()));
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(configs);
    }

    @GetMapping(value = "{process}/merges", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get merges for a process", response = List.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The list of all merges for a process")})
    public ResponseEntity<List<Merge>> getMerges(@PathVariable("process") String process,
                                                 @RequestParam(value = "minDate", required = false) String minDate,
                                                 @RequestParam(value = "maxDate", required = false) String maxDate) {
        List<Merge> merges;
        if (minDate != null && maxDate != null) {
            String decodedMinDate = URLDecoder.decode(minDate, StandardCharsets.UTF_8);
            ZonedDateTime minDateTime = ZonedDateTime.parse(decodedMinDate);
            String decodedMaxDate = URLDecoder.decode(maxDate, StandardCharsets.UTF_8);
            ZonedDateTime maxDateTime = ZonedDateTime.parse(decodedMaxDate);
            merges = mergeOrchestratorService.getMerges(process, minDateTime, maxDateTime);
        } else {
            merges = mergeOrchestratorService.getMerges(process);
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(merges);
    }
}

