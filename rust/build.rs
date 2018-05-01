/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

extern crate gl_generator;

use std::env;
use std::path::Path;

use gl_generator::{Api, Fallbacks, Profile, Registry, StaticStructGenerator};
use std::fs::File;

fn main() {
    println!("cargo:rerun-if-changed=build.rs");

    // Generate GL bindings
    let dest = env::var("OUT_DIR").unwrap();
    let mut file = File::create(&Path::new(&dest).join("egl_bindings.rs")).unwrap();
    Registry::new(Api::Egl, (1, 5), Profile::Core, Fallbacks::All, [])
        .write_bindings(StaticStructGenerator, &mut file)
        .unwrap();
}
