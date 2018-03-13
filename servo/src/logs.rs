/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

use log::{set_logger, Log, LogRecord, LogMetadata, LogLevelFilter, LogLevel};
use std::ffi::CString;
use std::os::raw::c_char;

pub struct Logger(extern fn(*const c_char));

impl Logger {
    pub fn init(callback: extern fn(*const c_char)) {
        set_logger(|max_log_level| {
            max_log_level.set(LogLevelFilter::Info);
            Box::new(Logger(callback))
        }).expect("set_logger failed");
    }
}

impl Log for Logger {
    fn enabled(&self, metadata: &LogMetadata) -> bool {
        metadata.level() <= LogLevel::Debug
    }

    fn log(&self, record: &LogRecord) {
        if self.enabled(record.metadata()) {
            let msg = format!("[{}] [{}] [{}]", record.level(), record.args(), record.target());
            let text = CString::new(msg.to_owned()).unwrap();
            let ptr = text.as_ptr();
            self.0(ptr);
        }
    }
}
