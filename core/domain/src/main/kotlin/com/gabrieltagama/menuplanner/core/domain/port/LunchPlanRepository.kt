package com.gabrieltagama.menuplanner.core.domain.port

import com.gabrieltagama.menuplanner.core.domain.model.LunchDay

/**
 * Persistence port for the lunch calendar (see DayPlanRepository). Implemented in :core:data.
 */
interface LunchPlanRepository : DayPlanRepository<LunchDay>
