package cz.applifting.humansis.misc

import android.nfc.Tag
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

class NfcTagPublisher {
    private val tagPublisher: PublishSubject<Tag> = PublishSubject.create<Tag>()

    fun getTagObservable(): Observable<Tag> {
        return tagPublisher
    }

    fun getTagSubject(): Subject<Tag> {
        return tagPublisher
    }
}
