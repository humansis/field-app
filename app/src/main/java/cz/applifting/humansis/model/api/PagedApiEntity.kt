package cz.applifting.humansis.model.api

class PagedApiEntity<E>(
    var totalCount: Long = 0,
    var data: List<E> = listOf()
)
