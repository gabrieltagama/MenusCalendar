package com.gabrieltagama.menuplanner.core.domain.repository

import com.gabrieltagama.menuplanner.core.domain.model.MealDay

/**
 * Persistence port for the lunch calendar (see DayPlanRepository). Implemented in :core:data.
 */
interface MealPlanRepository : DayPlanRepository<MealDay>
