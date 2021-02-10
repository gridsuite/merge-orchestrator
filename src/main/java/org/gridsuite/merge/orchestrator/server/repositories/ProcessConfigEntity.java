/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.merge.orchestrator.server.repositories;

import lombok.Getter;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.List;

/**
 * @author Chamseddine Benhamed <chamseddine.benhamed at rte-france.com
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com
 */
@Getter
@Table("configs")
public class ProcessConfigEntity {

    @PrimaryKey
    private String process;

    private String businessProcess;

    private List<TsoEntity> tsos;

    private boolean runBalancesAdjustment;

    public ProcessConfigEntity(String process, String businessProcess, List<TsoEntity> tsos, boolean runBalancesAdjustment) {
        this.process = process;
        this.businessProcess = businessProcess;
        this.tsos = tsos;
        this.runBalancesAdjustment = runBalancesAdjustment;
    }
}
