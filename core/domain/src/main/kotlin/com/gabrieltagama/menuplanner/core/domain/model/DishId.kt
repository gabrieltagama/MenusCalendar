package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Value object: identity of a dish. It is a UUID text so dishes can be merged across devices when
 * a shared JSON is imported, and a distinct type so an id is never confused with another string.
 */
@JvmInline
value class DishId(val value: String) {
    override fun toString(): String = value
}
