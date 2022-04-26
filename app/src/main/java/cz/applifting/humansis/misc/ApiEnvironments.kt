package cz.applifting.humansis.misc

import cz.applifting.humansis.BuildConfig

enum class ApiEnvironments(val id: Int, val secure: Boolean, val url: String, val port: Int?) {
    FRONT(0, true, BuildConfig.FRONT_API_URL, null),
    DEMO(1, true, BuildConfig.DEMO_API_URL, null),
    STAGE(2, true, BuildConfig.STAGE_API_URL, null),
    DEV1(3, true, BuildConfig.DEV1_API_URL, null),
    DEV2(4, true, BuildConfig.DEV2_API_URL, null),
    DEV3(5, true, BuildConfig.DEV3_API_URL, null),
    TEST(6, true, BuildConfig.TEST_API_URL, null),
    LOCAL(7, false, BuildConfig.LOCAL_API_URL, 8091);
}
