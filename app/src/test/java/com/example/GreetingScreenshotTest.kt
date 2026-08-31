package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.PersonalInfo
import com.example.data.model.ResumeFull
import com.example.ui.components.ResumeCardItem
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleResume = ResumeFull(
        title = "Senior Android Engineer",
        personalInfo = PersonalInfo(
            fullName = "Jane Doe",
            jobTitle = "Lead Mobile Architect"
        ),
        atsScore = 92
    )

    composeTestRule.setContent {
        MyApplicationTheme {
            ResumeCardItem(
                resume = sampleResume,
                onEdit = {},
                onPreview = {},
                onDuplicate = {},
                onDelete = {},
                onAtsScan = {},
                onShare = {}
            )
        }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/resume_card.png")
  }
}
