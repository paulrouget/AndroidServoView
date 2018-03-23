/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

extern crate jni;
extern crate libc;
#[macro_use] extern crate log;
extern crate android_logger;
extern crate servo;

#[cfg(target_os = "macos")]
extern crate core_foundation;

mod api;
mod gl_glue;
mod glue;

pub use api::*;
