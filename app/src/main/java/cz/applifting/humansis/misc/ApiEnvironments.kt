package cz.applifting.humansis.misc

import cz.applifting.humansis.BuildConfig

enum class ApiEnvironments(val id: Int, val url: String, val port: Int?) {
    FRONT(0, BuildConfig.FRONT_API_URL, null),
    DEMO(1, BuildConfig.DEMO_API_URL, null),
    STAGE(2, BuildConfig.STAGE_API_URL, null),
    DEV(3, BuildConfig.DEV_API_URL, null),
    TEST(4, BuildConfig.TEST_API_URL, null),
    LOCAL(5, BuildConfig.LOCAL_API_URL, 8087);
}
