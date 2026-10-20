package com.t53.telecom53

object Config {
    /** T53 production API. Not the Dynamic Robotics 53 website domain. */
    const val API_BASE = "https://t53.dynamicrobotics53.com"
    const val DASHBOARD_URL = "https://t53.dynamicrobotics53.com/parent/"

    /** While the T53 home screen is visible it checks the parent's settings this often. */
    const val UI_POLL_MS = 20_000L

    /** Background check interval when the child is inside an approved app (Android minimum is 15 min). */
    const val JOB_INTERVAL_MS = 15L * 60L * 1000L
    const val JOB_ID = 5301

    /** How long a temporary "let me use this app" approval lasts if the server does not say. */
    const val DEFAULT_GRANT_SECONDS = 1800L
}
