/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

use logs::Logger;
use gl_glue;
use glue::{self, SERVO};
use std::os::raw::c_char;
use jni::JNIEnv;
use jni::objects::{GlobalRef, JClass, JObject, JValue, JString};
use jni::sys::{jint, jlong, jstring};
use jni::signature::{Primitive, JavaType};
use jni::JavaVM;

#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_org_mozilla_geckoview_LibServo_version(env: JNIEnv, _class: JClass) -> jstring {
    let v = glue::servo_version();
    let output = env.new_string(format!("rust says: {}", v)).expect("Couldn't create java string");
    output.into_inner()
}

/// Needs to be called from the EGL thread
#[no_mangle]
#[allow(non_snake_case)]
pub fn Java_org_mozilla_geckoview_LibServo_init(
    env: JNIEnv, _: JClass,
    url: jstring,
    resources_path: jstring,
    callbacks_obj: JObject,
    layout_obj: JObject) {

    let callbacks = HostCallbacks::new(callbacks_obj, env);
    // let layout = ViewLayout::new(layout_obj);
    // let _ = Logger::init(|msg| {callbacks.log(msg)});
    callbacks.flush();
    callbacks.log("hi");
    // let gl = gl_glue::egl::init();
    // glue::init(gl, url, resources_path, callbacks, layout)
}

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

pub struct HostCallbacks {
    callbacks: GlobalRef,
    jvm: JavaVM,
}

impl HostCallbacks {

    pub fn new(jobject: JObject, env: JNIEnv) -> HostCallbacks {
        let jvm = env.get_java_vm().unwrap();
        HostCallbacks { callbacks: env.new_global_ref(jobject).unwrap(), jvm }
    }

    /// Will be called from any thread.
    /// Will be called to notify embedder that some events
    /// are available, and that perform_updates need to be called
    pub fn wakeup(&self) {
        // env.call_method(self.callbacks.as_obj(), "wakeup", JavaType::Primitive(Primitive::void), &[]).unwrap();
    }

    /// Will be called from the thread used for the init call
    /// Will be called when the GL buffer has been updated.
    pub fn flush(&self) {
        let env = self.jvm.get_env().unwrap();
        env.call_method(self.callbacks.as_obj(), "flush", "()V", &[]).unwrap();
    }

    /// Will be call from any thread.
    /// Used to report logging.
    /// Warning: this might be called a lot.
    pub fn log(&self, log: &str) {
        let env = self.jvm.get_env().unwrap();
        let out = env.new_string(log).unwrap();
        env.call_method(self.callbacks.as_obj(), "log", "(Ljava/lang/String;)V",
            &[JValue::Object(JObject::from(out))]).unwrap();
        // env.call_method(self.callbacks.as_obj(), "log", "()V", &[arg]).unwrap();
    }

    /// Page starts loading.
    /// "Reload button" becomes "Stop button".
    /// Throbber starts spinning.
    pub fn on_load_started(&self) {
    }

    /// Page has loaded.
    /// "Stop button" becomes "Reload button".
    /// Throbber stops spinning.
    pub fn on_load_ended(&self) {
    }

    /// Title changed.
    pub fn on_title_changed(&self, title: *const c_char) {
    }

    pub fn on_url_changed(&self, url: *const c_char) {
    }

    /// Back/forward state changed.
    /// Back/forward buttons need to be disabled/enabled.
    pub fn on_history_changed(&self, can_go_back: bool, can_go_forward: bool) {
    }
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
