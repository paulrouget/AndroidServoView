/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

use logs::Logger;
use gl_glue;
use glue::{self, SERVO};
use std::os::raw::c_char;

/// Generic result errors
#[repr(C)]
pub enum ServoResult {
    Ok,
    UnexpectedError,
    WrongThread,
    CantReadStr,
    CantParseUrl,    
    NotImplemented,
}

/// Scroll state
#[repr(C)]
pub enum ScrollState {
    Start,
    Move,
    End,
    Canceled,
}

/// Touch state
#[repr(C)]
pub enum TouchState {
    Down,
    Up,
}

/// Callback used by Servo internals
#[repr(C)]
pub struct HostCallbacks {

    /// Will be called from any thread.
    /// Will be called to notify embedder that some events
    /// are available, and that perform_updates need to be called
    pub wakeup: extern fn(),

    /// Will be called from the thread used for the init call
    /// Will be called when the GL buffer has been updated.
    pub flush: extern fn(),

    /// Will be call from any thread.
    /// Used to report logging.
    /// Warning: this might be called a lot.
    pub log: extern fn(log: *const c_char),

    /// Page starts loading.
    /// "Reload button" becomes "Stop button".
    /// Throbber starts spinning.
    pub on_load_started: extern fn(),

    /// Page has loaded.
    /// "Stop button" becomes "Reload button".
    /// Throbber stops spinning.
    pub on_load_ended: extern fn(),

    /// Title changed.
    pub on_title_changed: extern fn(title: *const c_char),

    /// URL changed.
    pub on_url_changed: extern fn(url: *const c_char),

    /// Back/forward state changed.
    /// Back/forward buttons need to be disabled/enabled.
    pub on_history_changed: extern fn(can_go_back: bool, can_go_forward: bool),
}

#[repr(C)]
#[derive(Debug)]
pub struct Size {
    pub width: u32,
    pub height: u32,
}

#[repr(C)]
#[derive(Debug)]
pub struct Margins {
    pub top: u32,
    pub right: u32,
    pub bottom: u32,
    pub left: u32,
}

#[repr(C)]
#[derive(Debug)]
pub struct Position {
    pub x: i32,
    pub y: i32,
}

#[repr(C)]
#[derive(Debug)]
pub struct ViewLayout {
    /// Size of the view. Hardware pixels.
    pub view_size: Size,
    /// Margins of the view. Hardware pixels.
    /// Pages are painted all over the surface,
    /// but if margins are not zero, the layout
    /// coordinates are bounds by these margins.
    pub margins: Margins,
    /// Position of the window.
    pub position: Position,
    /// Pixel density.
    pub hidpi_factor: f32,
}

#[no_mangle]
pub extern "C" fn servo_version() -> *const c_char {
    glue::servo_version()
}

/// Needs to be called from the EGL thread
#[cfg(not(target_os = "macos"))]
#[no_mangle]
pub extern "C" fn init_with_egl(
    url: *const c_char,
    resources_path: *const c_char,
    callbacks: HostCallbacks,
    layout: ViewLayout) -> ServoResult {
    let _ = Logger::init(callbacks.log);
    let gl = gl_glue::egl::init();
    glue::init(gl, url, resources_path, callbacks, layout)
}

/// Needs to be called from the main thread
#[cfg(target_os = "macos")]
#[no_mangle]
pub extern "C" fn init_with_gl(
    url: *const c_char,
    resources_path: *const c_char,
    callbacks: HostCallbacks,
    layout: ViewLayout) -> ServoResult {
    let _ = Logger::init(callbacks.log);
    let gl = gl_glue::gl::init();
    glue::init(gl, url, resources_path, callbacks, layout)
}


/// This is the Servo heartbeat. This needs to be called
/// everytime wakeup is called.
#[no_mangle]
pub extern "C" fn perform_updates() -> ServoResult {
    let mut res = ServoResult::UnexpectedError;
    SERVO.with(|s| {
        res = s.borrow_mut().as_mut().map(|ref mut s| {
            s.perform_updates()
        }).unwrap_or(ServoResult::WrongThread)
    });
    res
}

#[no_mangle]
pub extern "C" fn scroll(dx: i32, dy: i32, x: u32, y: u32, state: ScrollState) -> ServoResult {
    let mut res = ServoResult::UnexpectedError;
    SERVO.with(|s| {
        res = s.borrow_mut().as_mut().map(|ref mut s| {
            s.scroll(dx, dy, x, y, state)
        }).unwrap_or(ServoResult::WrongThread)
    });
    res
}

#[no_mangle]
pub extern "C" fn click(x: u32, y: u32) -> ServoResult {
    let mut res = ServoResult::UnexpectedError;
    SERVO.with(|s| {
        res = s.borrow_mut().as_mut().map(|ref mut s| {
            s.click(x, y)
        }).unwrap_or(ServoResult::WrongThread)
    });
    res
}

/// Load an URL. This needs to be a valid url.
#[no_mangle]
pub extern "C" fn load_url(url: *const c_char) -> ServoResult {
    let mut res = ServoResult::UnexpectedError;
    SERVO.with(|s| {
        res = s.borrow_mut().as_mut().map(|ref mut s| {
            s.load_url(url)
        }).unwrap_or(ServoResult::WrongThread)
    });
    res
}

/// Reload page.
#[no_mangle]
pub extern "C" fn reload() -> ServoResult {
    let mut res = ServoResult::UnexpectedError;
    SERVO.with(|s| {
        res = s.borrow_mut().as_mut().map(|ref mut s| {
            s.reload()
        }).unwrap_or(ServoResult::WrongThread)
    });
    res
}

/// Reload page.
#[no_mangle]
pub extern "C" fn resize(layout: ViewLayout) -> ServoResult {
    let mut res = ServoResult::UnexpectedError;
    SERVO.with(|s| {
        res = s.borrow_mut().as_mut().map(|ref mut s| {
            s.resize(layout)
        }).unwrap_or(ServoResult::WrongThread)
    });
    res
}

/// Stop page loading.
#[no_mangle]
pub extern "C" fn stop() -> ServoResult {
    // FIXME
    ServoResult::NotImplemented
}

#[no_mangle]
pub extern "C" fn go_back() -> ServoResult {
    let mut res = ServoResult::UnexpectedError;
    SERVO.with(|s| {
        res = s.borrow_mut().as_mut().map(|ref mut s| {
            s.go_back()
        }).unwrap_or(ServoResult::WrongThread)
    });
    res
}

#[no_mangle]
pub extern "C" fn go_forward() -> ServoResult {
    let mut res = ServoResult::UnexpectedError;
    SERVO.with(|s| {
        res = s.borrow_mut().as_mut().map(|ref mut s| {
            s.go_forward()
        }).unwrap_or(ServoResult::WrongThread)
    });
    res
}

#[no_mangle]
pub extern "C" fn erase() -> ServoResult {
    let mut res = ServoResult::UnexpectedError;
    SERVO.with(|s| {
        res = s.borrow_mut().as_mut().map(|ref mut s| {
            s.erase()
        }).unwrap_or(ServoResult::WrongThread)
    });
    res
}
