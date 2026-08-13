package com.codetivelab.fieldcalc

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag

/**
 * Text rendered by the node carrying [tag].
 *
 * Reads the **unmerged** tree on purpose: the operator rows are `Modifier.clickable`, which merges
 * descendant semantics, so the value inside a row is only addressable by its own tag there.
 */
fun ComposeTestRule.textOf(tag: String): String =
    onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().config
        .getOrNull(SemanticsProperties.Text)
        ?.joinToString("") { it.text }
        .orEmpty()

/** True once at least one node with [tag] is present — safe to poll from waitUntil. */
fun ComposeTestRule.hasTag(tag: String): Boolean =
    onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
