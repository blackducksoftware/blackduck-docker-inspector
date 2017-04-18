/*
 * Copyright (C) 2017 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.hub.docker

class OsMapper {
    private Map<OperatingSystemEnum, OperatingSystemEnum> targetToRuntimeMap = new HashMap<>();

    OsMapper() {
        targetToRuntimeMap.put(OperatingSystemEnum.UBUNTU, OperatingSystemEnum.CENTOS);
        targetToRuntimeMap.put(OperatingSystemEnum.CENTOS, OperatingSystemEnum.UBUNTU);
    }

    OperatingSystemEnum getRuntimeOsForTargetImageOs(OperatingSystemEnum targetImageOs) {
        targetToRuntimeMap.get(targetImageOs)
    }
}
