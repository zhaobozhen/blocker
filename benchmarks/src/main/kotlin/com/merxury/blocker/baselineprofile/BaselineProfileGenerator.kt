/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.merxury.blocker.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import com.merxury.blocker.PACKAGE_NAME
import com.merxury.blocker.allowNotifications
import com.merxury.blocker.applist.appListScrollDownUp
import com.merxury.blocker.applist.appListWaitForContent
import com.merxury.blocker.rules.goToRulesScreen
import com.merxury.blocker.rules.rulesScrollListDownUp
import com.merxury.blocker.search.goToSearchScreen
import org.junit.Rule
import org.junit.Test

/**
 * Generates a baseline profile which can be copied to `app/src/main/baseline-prof.txt`.
 */
class BaselineProfileGenerator {
    @get:Rule val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() =
        baselineProfileRule.collect(PACKAGE_NAME) {
            // This block defines the app's critical user journey. Here we are interested in
            // optimizing for app startup. But you can also navigate and scroll
            // through your most important UI.
            allowNotifications()
            pressHome()
            startActivityAndWait()
            allowNotifications()

            appListWaitForContent()
            appListScrollDownUp()

            goToRulesScreen()
            rulesScrollListDownUp()

            goToSearchScreen()
        }
}
