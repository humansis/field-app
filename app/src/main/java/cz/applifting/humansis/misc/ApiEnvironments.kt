package cz.applifting.humansis.misc

import cz.applifting.humansis.BuildConfig

enum class ApiEnvironments(val id: Int) {
    FRONT(0) {
        override fun getUrl() = BuildConfig.FRONT_API_URL
    },
    DEMO(1) {
        override fun getUrl() = BuildConfig.DEMO_API_URL
    },
    STAGE(2) {
        override fun getUrl() = BuildConfig.STAGE_API_URL
    },
    DEV(3) {
        override fun getUrl() = BuildConfig.DEV_API_URL
    },
    TEST(4) {
        override fun getUrl() = BuildConfig.TEST_API_URL
    },
    BASE(5) {
        override fun getUrl() = BuildConfig.API_BASE_URL
    };

    abstract fun getUrl(): String
}
