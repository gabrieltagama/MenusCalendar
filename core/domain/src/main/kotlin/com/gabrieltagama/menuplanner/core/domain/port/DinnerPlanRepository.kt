package com.gabrieltagama.menuplanner.core.domain.port

import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay

/**
 * Persistence port for the dinner calendar (see DayPlanRepository). Implemented in :core:data.
 */
interface DinnerPlanRepository : DayPlanRepository<DinnerDay>
