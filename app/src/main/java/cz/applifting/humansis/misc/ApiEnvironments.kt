package cz.applifting.humansis.misc

import cz.applifting.humansis.BuildConfig

enum class ApiEnvironments(val id: Int, val secure: Boolean, val url: String, val port: Int?) {
    FRONT(0, true, BuildConfig.FRONT_API_URL, null),
    DEMO(1, true, BuildConfig.DEMO_API_URL, null),
    STAGE(2, true, BuildConfig.STAGE_API_URL, null),
    DEV(3, true, BuildConfig.DEV_API_URL, null),
    TEST(4, true, BuildConfig.TEST_API_URL, null),
    LOCAL(5, false, BuildConfig.LOCAL_API_URL, 8091);
}
