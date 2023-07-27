/*
   Copyright © 2017-2023  Kynetics  LLC

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.kynetics.android_i2c_example_app

import io.helins.linux.i2c.I2CBuffer


fun Int.toHexString(): String = Integer.toHexString(this)

/**
 * Shows the buffers content
 *
 * @return Buffer's content in hex as string
 */
fun I2CBuffer.toHexString(): String {
    var result = ""
    for (i in 0 until length) {
        result += "${get(i).toHexString()} "
    }
    return result.trim()
}