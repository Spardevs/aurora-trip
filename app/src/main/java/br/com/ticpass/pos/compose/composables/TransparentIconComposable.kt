/*
 * Copyright (c) 2025 Ticpass. All rights reserved.
 *
 * PROPRIETARY AND CONFIDENTIAL
 *
 * This software is the confidential and proprietary information of Ticpass
 * ("Confidential Information"). You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the license agreement you
 * entered into with Ticpass.
 *
 * Unauthorized copying, distribution, or use of this software, via any medium,
 * is strictly prohibited without the express written permission of Ticpass.
 */

package br.com.ticpass.pos.compose.composables

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import br.com.ticpass.pos.R

/**
 * A transparent icon for occupying space
 *
 * This is useful for occupying spaces in composable where alignment is not respected such as
 * DropDownMenu.
 */
@Composable
fun TransparentIconComposable() {
    Icon(
        painter = painterResource(R.drawable.ic_transparent),
        contentDescription = null
    )
}

@Preview(showBackground = true)
@Composable
private fun TransparentIconComposablePreview() {
    TransparentIconComposable()
}
